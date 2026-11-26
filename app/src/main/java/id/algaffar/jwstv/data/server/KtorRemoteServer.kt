package id.algaffar.jwstv.data.server

import android.content.Context
import android.os.SystemClock
import id.algaffar.jwstv.data.auth.AuthManager
import id.algaffar.jwstv.data.auth.UserRole
import id.algaffar.jwstv.data.hardware.SwitchController
import id.algaffar.jwstv.data.media.MediaStorageManager
import id.algaffar.jwstv.data.media.StorageHealthSnapshot
import id.algaffar.jwstv.domain.model.AppState
import id.algaffar.jwstv.domain.model.AppStateSettings
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.header
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receive
import io.ktor.server.request.receiveStream
import io.ktor.server.request.receiveText
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.options
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import java.io.File
import java.net.URI
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.InputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

data class RemoteCommandPayload(
    val action: String,
    val rawJson: JsonObject
) {
    fun stringArg(key: String, maxLen: Int = 500): String? =
        (rawJson[key] as? JsonPrimitive)?.contentOrNull?.let { KtorRemoteServer.sanitizeInput(it, maxLen) }

    fun rawStringArg(key: String, maxLen: Int = 1000): String? =
        (rawJson[key] as? JsonPrimitive)?.contentOrNull?.trim()?.take(maxLen)

    fun doubleArg(key: String): Double? =
        (rawJson[key] as? JsonPrimitive)?.doubleOrNull

    fun intArg(key: String): Int? =
        (rawJson[key] as? JsonPrimitive)?.intOrNull

    fun booleanArg(key: String): Boolean? =
        (rawJson[key] as? JsonPrimitive)?.booleanOrNull

    fun stringListArg(key: String, maxItems: Int = 50, maxLen: Int = 200): List<String>? {
        val arr = rawJson[key] as? JsonArray ?: return null
        return arr.take(maxItems).mapNotNull { el ->
            (el as? JsonPrimitive)?.contentOrNull?.let { KtorRemoteServer.sanitizeInput(it, maxLen) }
        }
    }
}

/**
 * Sliding-window IP rate limiter (default 180 requests per 60 seconds).
 * Mirrors `RateLimiter` in `src-tauri/src/security.rs`.
 */
class IpRateLimiter(
    private val maxRequests: Int = 180,
    private val windowMillis: Long = 60_000L
) {
    private data class Bucket(
        var windowStartMs: Long,
        var count: Int
    )

    private val buckets = ConcurrentHashMap<String, Bucket>()

    @Synchronized
    fun isAllowed(clientIp: String, nowMs: Long = System.currentTimeMillis()): Boolean {
        val key = clientIp.ifBlank { "unknown" }
        val bucket = buckets[key]
        if (bucket == null || nowMs - bucket.windowStartMs >= windowMillis) {
            buckets[key] = Bucket(windowStartMs = nowMs, count = 1)
            return true
        }
        if (bucket.count >= maxRequests) {
            return false
        }
        bucket.count += 1
        return true
    }
}

/**
 * Embedded Ktor CIO HTTP Server on port `1421` serving the JWS Smart Remote PWA
 * (`/`, `/remote`, `/manifest.json`, `/sw.js`, `/alGaffar.png`) and REST API
 * (`GET /api/status`, `POST /api/command`, `POST /api/upload`) with Bearer token auth & IP rate limiting.
 *
 * Mirrors `src-tauri/src/server/mod.rs` and `src-tauri/src/server/handlers.rs`.
 */
class KtorRemoteServer(
    private val context: Context? = null,
    val port: Int = DEFAULT_PORT,
    authToken: String? = null,
    private val tokenStore: RemoteTokenStore? = null,
    private val settingsProvider: () -> AppStateSettings = { AppStateSettings() },
    private val runtimeStatusProvider: () -> RuntimeServerSnapshot = { RuntimeServerSnapshot() },
    private val audioInventoryProvider: () -> Map<String, List<String>> = { emptyMap() },
    private val storageHealthProvider: () -> StorageHealthSnapshot = { StorageHealthSnapshot() },
    private val clockStatusProvider: () -> ClockStatusSnapshot = { ClockStatusSnapshot() },
    private val onSyncTimeReceived: (epochMs: Long) -> Result<String> = {
        Result.failure(UnsupportedOperationException("Sync waktu belum aktif"))
    },
    private val onCommandReceived: (RemoteCommandPayload) -> Unit = {},
    private val onUploadReceived: (category: String, slot: String, fileName: String, bytes: ByteArray) -> Result<String> = { _, _, _, _ ->
        Result.failure(UnsupportedOperationException("Upload belum aktif"))
    },
    private val onUploadStreamReceived: ((category: String, slot: String, fileName: String, inputStream: InputStream) -> Result<String>)? = null
) {
    data class RuntimeServerSnapshot(
        val currentState: AppState = AppState.STANDBY,
        val isAudioPlaying: Boolean = false,
        val isCustomAudioPlaying: Boolean = false
    )

    data class ClockStatusSnapshot(
        val isClockSuspect: Boolean = false,
        val clockOffsetMs: Long = 0L,
        val deviceTimeIso: String = ""
    )

    @Volatile
    private var activeAuthToken: String = authToken?.takeIf { it.isNotBlank() }
        ?: tokenStore?.getOrCreateRemoteToken()
        ?: generateSecureToken(DEFAULT_TOKEN_LENGTH)

    val authToken: String
        get() = activeAuthToken

    fun regenerateAuthToken(): String {
        val next = tokenStore?.regenerateRemoteToken() ?: generateSecureToken(DEFAULT_TOKEN_LENGTH)
        activeAuthToken = next
        return next
    }

    private var engine: CIOApplicationEngine? = null
    private val rateLimiter = IpRateLimiter(maxRequests = 180, windowMillis = 60_000L)
    private val startedAtMs = System.currentTimeMillis()
    private val cachedHtmlRef = AtomicReference<String?>(null)

    val isRunning: Boolean
        get() = engine != null

    fun start(): Boolean {
        if (engine != null) return true
        return try {
            val server = embeddedServer(CIO, host = "0.0.0.0", port = port) {
                intercept(ApplicationCallPipeline.Plugins) {
                    val origin = call.request.header(HttpHeaders.Origin)
                    if (!origin.isNullOrBlank()) {
                        if (!isAllowedLanOrigin(origin)) {
                            call.respondText(
                                text = """{"error":"Origin tidak diizinkan (hanya jaringan lokal LAN)"}""",
                                contentType = ContentType.Application.Json,
                                status = HttpStatusCode.Forbidden
                            )
                            finish()
                            return@intercept
                        }
                        call.response.header(HttpHeaders.AccessControlAllowOrigin, origin)
                        call.response.header(HttpHeaders.Vary, HttpHeaders.Origin)
                        call.response.header(HttpHeaders.AccessControlAllowMethods, "GET, POST, OPTIONS")
                        call.response.header(HttpHeaders.AccessControlAllowHeaders, "Authorization, Content-Type")
                        if (call.request.httpMethod == HttpMethod.Options) {
                            call.respond(HttpStatusCode.NoContent)
                            finish()
                            return@intercept
                        }
                    }
                }

                routing {
                    options("{...}") {
                        call.respond(HttpStatusCode.NoContent)
                    }
                    get("/") { handleHtmlIndex(call) }
                    get("/remote") { handleHtmlIndex(call) }
                    get("/remote/") { handleHtmlIndex(call) }

                    get("/manifest.json") {
                        if (!checkRateLimit(call)) return@get
                        call.respondText(
                            text = REMOTE_MANIFEST_JSON,
                            contentType = ContentType.Application.Json
                        )
                    }

                    get("/sw.js") {
                        if (!checkRateLimit(call)) return@get
                        call.respondText(
                            text = REMOTE_SW_JS,
                            contentType = ContentType.Application.JavaScript
                        )
                    }

                    get("/alGaffar.png") {
                        if (!checkRateLimit(call)) return@get
                        call.respondBytes(
                            bytes = MINIMAL_PNG_ICON,
                            contentType = ContentType.Image.PNG
                        )
                    }

                    get("/app-icon.png") {
                        if (!checkRateLimit(call)) return@get
                        call.respondBytes(
                            bytes = MINIMAL_PNG_ICON,
                            contentType = ContentType.Image.PNG
                        )
                    }

                    get("/api/status") {
                        if (!checkRateLimit(call)) return@get
                        if (checkAuthorizedRole(call) == null) return@get

                        val statusJson = buildStatusJson()
                        call.respondText(
                            text = statusJson.toString(),
                            contentType = ContentType.Application.Json,
                            status = HttpStatusCode.OK
                        )
                    }

                    get("/api/backup") {
                        if (!checkRateLimit(call)) return@get
                        if (checkAuthorizedRole(call) == null) return@get

                        val settings = settingsProvider()
                        val backupJson = Json { prettyPrint = true; encodeDefaults = true }
                            .encodeToString(id.algaffar.jwstv.domain.model.AppStateSettings.serializer(), settings)
                        val slug = sanitizeFileSlug(settings.brand.name)
                        call.response.headers.append(
                            HttpHeaders.ContentDisposition,
                            "attachment; filename=\"algaffar-tv-backup-$slug.json\""
                        )
                        call.respondText(
                            text = backupJson,
                            contentType = ContentType.Application.Json,
                            status = HttpStatusCode.OK
                        )
                    }

                    post("/api/sync_time") {
                        if (!checkRateLimit(call)) return@post
                        if (checkAuthorizedRole(call) == null) return@post

                        val body = call.receiveText()
                        val parsedObj = runCatching { Json.parseToJsonElement(body) as? JsonObject }.getOrNull()
                        val epochMs = (parsedObj?.get("epochMs") as? JsonPrimitive)?.contentOrNull?.toLongOrNull()
                        if (epochMs == null || epochMs < 1_704_067_200_000L) {
                            call.respondText(
                                text = """{"error":"Timestamp epochMs tidak valid"}""",
                                contentType = ContentType.Application.Json,
                                status = HttpStatusCode.BadRequest
                            )
                            return@post
                        }
                        val res = onSyncTimeReceived(epochMs)
                        res.onSuccess { msg ->
                            val resp = buildJsonObject {
                                put("status", "ok")
                                put("message", msg)
                            }
                            call.respondText(resp.toString(), ContentType.Application.Json, HttpStatusCode.OK)
                        }.onFailure { err ->
                            val resp = buildJsonObject {
                                put("error", err.message ?: "Gagal menyinkronkan waktu")
                            }
                            call.respondText(resp.toString(), ContentType.Application.Json, HttpStatusCode.BadRequest)
                        }
                    }

                    post("/api/command") {
                        if (!checkRateLimit(call)) return@post
                        val callerRole = checkAuthorizedRole(call) ?: return@post

                        val body = call.receiveText()
                        if (body.length > MAX_CONTENT_LENGTH) {
                            call.respondText(
                                text = """{"error":"Request too large"}""",
                                contentType = ContentType.Application.Json,
                                status = HttpStatusCode.PayloadTooLarge
                            )
                            return@post
                        }

                        val parsedObj = try {
                            Json.parseToJsonElement(body) as? JsonObject
                        } catch (_: Exception) {
                            null
                        }

                        val action = (parsedObj?.get("action") as? JsonPrimitive)?.contentOrNull?.trim()
                        if (parsedObj == null || action.isNullOrBlank()) {
                            call.respondText(
                                text = """{"error":"Invalid JSON command"}""",
                                contentType = ContentType.Application.Json,
                                status = HttpStatusCode.BadRequest
                            )
                            return@post
                        }

                        val sanitizedAction = sanitizeInput(action, 64)
                        if (!isActionAllowedForRole(sanitizedAction, callerRole)) {
                            call.respondText(
                                text = """{"error":"Akses ditolak: aksi '$sanitizedAction' hanya untuk Super Admin"}""",
                                contentType = ContentType.Application.Json,
                                status = HttpStatusCode.Forbidden
                            )
                            return@post
                        }

                        onCommandReceived(
                            RemoteCommandPayload(
                                action = sanitizedAction,
                                rawJson = parsedObj
                            )
                        )

                        call.respondText(
                            text = """{"status":"ok"}""",
                            contentType = ContentType.Application.Json,
                            status = HttpStatusCode.OK
                        )
                    }

                    post("/api/upload") {
                        if (!checkRateLimit(call)) return@post
                        val callerRole = checkAuthorizedRole(call) ?: return@post

                        val category = sanitizeInput(call.request.queryParameters["category"].orEmpty(), 32).lowercase()
                        if (category == "ota_apk" && callerRole != UserRole.SUPERADMIN) {
                            call.respondText(
                                text = """{"error":"Akses ditolak: pembaruan APK hanya untuk Super Admin"}""",
                                contentType = ContentType.Application.Json,
                                status = HttpStatusCode.Forbidden
                            )
                            return@post
                        }
                        val slot = sanitizeInput(call.request.queryParameters["slot"].orEmpty(), 32).lowercase()
                        val fileName = sanitizeInput(
                            call.request.queryParameters["filename"].orEmpty().ifBlank { "upload.bin" },
                            120
                        )

                        if (category.isBlank()) {
                            call.respondText(
                                text = """{"error":"Parameter category wajib diisi"}""",
                                contentType = ContentType.Application.Json,
                                status = HttpStatusCode.BadRequest
                            )
                            return@post
                        }

                        val maxAllowedBytes = maxUploadBytesForCategory(category)
                        val declaredLength = call.request.header(HttpHeaders.ContentLength)?.toLongOrNull()
                        if (declaredLength != null && (declaredLength <= 0L || declaredLength > maxAllowedBytes)) {
                            call.respondText(
                                text = """{"error":"Ukuran file tidak valid (maksimal ${MediaStorageManager.formatByteSize(maxAllowedBytes)})"}""",
                                contentType = ContentType.Application.Json,
                                status = HttpStatusCode.PayloadTooLarge
                            )
                            return@post
                        }

                        val streamHandler = onUploadStreamReceived
                        val result: Result<String> = if (streamHandler != null) {
                            try {
                                val inputStream = call.receiveStream()
                                withContext(Dispatchers.IO) {
                                    inputStream.use { stream ->
                                        streamHandler(category, slot, fileName, stream)
                                    }
                                }
                            } catch (e: Exception) {
                                Result.failure(e)
                            }
                        } else {
                            val bytes = try {
                                call.receive<ByteArray>()
                            } catch (e: Exception) {
                                call.respondText(
                                    text = """{"error":"Gagal membaca file upload"}""",
                                    contentType = ContentType.Application.Json,
                                    status = HttpStatusCode.BadRequest
                                )
                                return@post
                            }

                            if (bytes.isEmpty() || bytes.size.toLong() > maxAllowedBytes) {
                                call.respondText(
                                    text = """{"error":"Ukuran file tidak valid (maksimal ${MediaStorageManager.formatByteSize(maxAllowedBytes)})"}""",
                                    contentType = ContentType.Application.Json,
                                    status = HttpStatusCode.PayloadTooLarge
                                )
                                return@post
                            }

                            onUploadReceived(category, slot, fileName, bytes)
                        }

                        result.onSuccess { msg ->
                            val resp = buildJsonObject {
                                put("status", "ok")
                                put("message", msg)
                            }
                            call.respondText(
                                text = resp.toString(),
                                contentType = ContentType.Application.Json,
                                status = HttpStatusCode.OK
                            )
                        }.onFailure { err ->
                            val resp = buildJsonObject {
                                put("error", err.message ?: "Gagal menyimpan file")
                            }
                            call.respondText(
                                text = resp.toString(),
                                contentType = ContentType.Application.Json,
                                status = HttpStatusCode.BadRequest
                            )
                        }
                    }
                }
            }
            server.start(wait = false)
            engine = server
            true
        } catch (_: Exception) {
            false
        }
    }

    fun stop() {
        runCatching {
            engine?.stop(gracePeriodMillis = 200, timeoutMillis = 500)
        }
        engine = null
    }

    fun getRemoteUrl(includeTokenQuery: Boolean = false): String {
        val ip = resolveLocalNetworkIp()
        val base = "http://$ip:$port/remote"
        return if (includeTokenQuery) "$base?token=$authToken" else base
    }

    /**
     * Non-blocking pair of `(remoteUrl, qrPairingUrl)` using the IO-refreshed cached LAN IP.
     * Safe to invoke every second on the Main thread without triggering netlink syscalls.
     */
    fun getRemoteUrlsNonBlocking(): Pair<String, String> {
        val ip = getCachedLocalIpOrLoopback()
        val base = "http://$ip:$port/remote"
        return base to "$base?token=$authToken"
    }

    private suspend fun handleHtmlIndex(call: ApplicationCall) {
        if (!checkRateLimit(call)) return
        call.respondText(
            text = loadRemoteHtml(),
            contentType = ContentType.Text.Html
        )
    }

    private suspend fun checkRateLimit(call: ApplicationCall): Boolean {
        val ip = extractClientIp(call)
        if (!rateLimiter.isAllowed(ip)) {
            call.respondText(
                text = """{"error":"Rate limit exceeded. Try again later."}""",
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.TooManyRequests
            )
            return false
        }
        return true
    }

    private suspend fun checkAuthorizedRole(call: ApplicationCall): UserRole? {
        val authHeader = call.request.header(HttpHeaders.Authorization)
        val settings = settingsProvider()
        val role = resolveAuthorizedRole(authHeader, authToken, settings)
        if (role == null) {
            call.respondText(
                text = """{"error":"Unauthorized"}""",
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.Unauthorized
            )
            return null
        }
        return role
    }

    internal fun buildStatusJson(): JsonObject {
        val settings = settingsProvider()
        val runtime = runtimeStatusProvider()
        val audioInv = runCatching { audioInventoryProvider() }.getOrDefault(emptyMap())
        val runtimeMem = Runtime.getRuntime()
        val maxMem = runtimeMem.maxMemory().coerceAtLeast(1L)
        val usedHeap = (runtimeMem.totalMemory() - runtimeMem.freeMemory()).coerceAtLeast(0L)
        val ramPct = ((usedHeap.toDouble() / maxMem.toDouble()) * 100.0 * 10.0).toInt().coerceIn(0, 1000) / 10.0
        val sysRamPct: Double? = context?.let { ctx ->
            runCatching {
                val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                val mi = android.app.ActivityManager.MemoryInfo()
                am?.getMemoryInfo(mi)
                if (mi.totalMem > 0L) {
                    val sysUsed = (mi.totalMem - mi.availMem).coerceAtLeast(0L)
                    ((sysUsed.toDouble() / mi.totalMem.toDouble()) * 100.0 * 10.0).toInt().coerceIn(0, 1000) / 10.0
                } else null
            }.getOrNull()
        }

        val uptimeSec = try {
            SystemClock.elapsedRealtime() / 1000L
        } catch (_: Throwable) {
            (System.currentTimeMillis() - startedAtMs) / 1000L
        }.coerceAtLeast(1L)

        val days = uptimeSec / 86400L
        val hours = (uptimeSec % 86400L) / 3600L
        val mins = (uptimeSec % 3600L) / 60L
        val uptimeStr = when {
            days > 0 -> "${days}d ${hours}h ${mins}m"
            hours > 0 -> "${hours}h ${mins}m"
            else -> "${mins}m"
        }

        val effJumatKey = settings.jumat.petugas.selectedDate.takeIf { it.isNotBlank() }
            ?: settings.jumat.petugas.records.keys.sorted().lastOrNull().orEmpty()
        val effJumatRec = settings.jumat.petugas.records[effJumatKey]

        return buildJsonObject {
            put("state", runtime.currentState.serialName)
            put("stateLabel", runtime.currentState.displayLabel)
            put("audio", runtime.isAudioPlaying)
            put("customAudio", runtime.isCustomAudioPlaying || settings.customAudio.isPlayingLive)
            put("volume", (settings.globalVolume * 100f).toInt().coerceIn(0, 100))
            put("theme", settings.themeSettings.currentThemeId)
            put("layout", settings.layoutSettings.position.serialName)
            put("clockStyle", settings.brand.clockStyle)
            put("jumatEnabled", settings.jumat.enabled)
            put("ramadhanEnabled", settings.ramadhan.isRamadhan)
            put("idulFitriEnabled", settings.idulFitri.enabled)
            put("idulAdhaEnabled", settings.idulAdha.enabled)
            put("slideshowEnabled", settings.standbySlideshow.enabled)
            put("audioEngine", "backend")
            put("activeLiveVideoId", settings.live.activeYoutubeId)
            put("liveSource", settings.live.source)

            putJsonObject("liveConfig") {
                put("source", settings.live.source)
                put("makkahVideoId", settings.live.makkahVideoId)
                put("madinahVideoId", settings.live.madinahVideoId)
                put("customVideoId", settings.live.customVideoId)
                put("makkahHlsUrl", settings.live.makkahHlsUrl)
                put("madinahHlsUrl", settings.live.madinahHlsUrl)
                put("customStreamUrl", settings.live.customStreamUrl)
                put("preferNativeHls", settings.live.preferNativeHls)
                put("muted", settings.live.muted)
            }

            putJsonObject("brand") {
                put("name", settings.brand.name)
                put("address", settings.brand.address)
                put("phone", settings.brand.phone)
                put("logo", settings.brand.logo)
            }

            putJsonObject("prayerConfig") {
                put("latitude", settings.prayerSettings.latitude)
                put("longitude", settings.prayerSettings.longitude)
                put("timezone", settings.prayerSettings.timezone)
                put("calculationMethod", settings.prayerSettings.calculationMethod)
                put("madhab", settings.prayerSettings.madhab)
                put("ihtiyati", settings.prayerSettings.ihtiyati)
                put("hijriAdjustment", settings.prayerSettings.hijriAdjustment)
                putJsonObject("corrections") {
                    val c = settings.prayerSettings.corrections
                    put("fajr", c.fajr)
                    put("dhuhr", c.dhuhr)
                    put("asr", c.asr)
                    put("maghrib", c.maghrib)
                    put("isha", c.isha)
                }
            }

            putJsonObject("prayerSettings") {
                val c = settings.prayerSettings.corrections
                val map = listOf(
                    Triple("fajr", settings.prayers.fajr, c.fajr),
                    Triple("dhuhr", settings.prayers.dhuhr, c.dhuhr),
                    Triple("asr", settings.prayers.asr, c.asr),
                    Triple("maghrib", settings.prayers.maghrib, c.maghrib),
                    Triple("isha", settings.prayers.isha, c.isha)
                )
                for ((key, cfg, corr) in map) {
                    putJsonObject(key) {
                        put("isTartil", cfg.isTartil && cfg.tartilDuration > 0)
                        put("isTarhim", cfg.isTarhim && cfg.tarhimDuration > 0)
                        put("isAdzan", cfg.isAdzan)
                        put("tartilDuration", cfg.tartilDuration)
                        put("adzanDuration", cfg.adzanDuration)
                        put("iqomahDuration", cfg.iqomahDuration)
                        put("sholatDuration", cfg.sholatDuration)
                        put("imam", cfg.imam)
                        put("muadzin", cfg.muadzin)
                        put("adzanTagline", cfg.adzanTagline)
                        put("adzanMessage", cfg.adzanMessage)
                        put("sholatMessage", cfg.sholatMessage)
                        put("correction", corr)
                    }
                }
            }

            putJsonArray("overlayTextList") {
                settings.floatingOverlays.forEach { add(it.message) }
            }

            putJsonArray("floatingOverlays") {
                settings.floatingOverlays.forEach { ov ->
                    addJsonObject {
                        put("id", ov.id)
                        put("title", ov.title)
                        put("message", ov.message)
                    }
                }
            }

            putJsonArray("mosqueInfoList") {
                settings.mosqueInfoList.forEach { info ->
                    addJsonObject {
                        put("id", info.id)
                        put("title", info.title)
                        put("content", info.content)
                        put("subtitle", info.subtitle)
                        put("enabled", info.enabled)
                    }
                }
            }

            putJsonArray("slideGroups") {
                settings.slideGroups.forEach { grp ->
                    addJsonObject {
                        put("id", grp.id)
                        put("name", grp.name)
                        put("enabled", grp.enabled)
                        put("totalDuration", grp.totalDurationSeconds)
                        put("overlayPosition", grp.normalizedOverlayPosition)
                        put("qrCardPosition", grp.qrCardPosition)
                        put("showQrCard", grp.showQrCard)
                        put("showInMain", grp.showInMain)
                        put("showInSlideshow", grp.showInSlideshow)
                        put("showInJumat", grp.showInJumat)
                        put("showInRamadhan", grp.showInRamadhan)
                        put("showInIdulFitri", grp.showInIdulFitri)
                        put("showInIdulAdha", grp.showInIdulAdha)
                        putJsonArray("overlayIds") {
                            grp.overlayIds.forEach { add(it) }
                        }
                        putJsonArray("slides") {
                            grp.slides.forEach { s ->
                                addJsonObject {
                                    put("id", s.id)
                                    put("title", s.title)
                                    put("type", s.type)
                                    put("duration", s.duration)
                                    put("enabled", s.enabled)
                                }
                            }
                        }
                    }
                }
            }

            putJsonArray("slides") {
                settings.slideGroups.flatMap { it.slides }.ifEmpty { settings.slides }.forEach { s ->
                    addJsonObject {
                        put("id", s.id)
                        put("title", s.title)
                        put("type", s.type)
                        put("duration", s.duration)
                        put("enabled", s.enabled)
                        put("showInMain", s.showInMain)
                        put("showInSlideshow", s.showInSlideshow)
                        put("showInJumat", s.showInJumat)
                        put("showInRamadhan", s.showInRamadhan)
                        put("showInIdulFitri", s.showInIdulFitri)
                        put("showInIdulAdha", s.showInIdulAdha)
                    }
                }
            }

            putJsonObject("infoSettings") {
                put("enableSunnahFasting", settings.infoSettings.enableSunnahFasting)
                put("lookaheadDays", settings.infoSettings.lookaheadDays)
                putJsonArray("islamicEvents") {
                    settings.infoSettings.islamicEvents.forEach { ev ->
                        addJsonObject {
                            put("id", ev.id)
                            put("name", ev.name)
                            put("type", ev.type)
                            put("hijriMonth", ev.hijriMonth)
                            put("hijriDay", ev.hijriDay)
                            put("gregorianDate", ev.gregorianDate)
                            put("enabled", ev.enabled)
                        }
                    }
                }
            }

            val todayStr = java.time.LocalDate.now().toString()
            val effKeuDate = settings.jumat.keuangan.selectedDate.ifBlank {
                id.algaffar.jwstv.domain.engine.JwsStateMachine.findEffectiveJumatKeuanganDate(
                    settings.jumat.keuangan.records,
                    java.time.LocalDate.now()
                ) ?: todayStr
            }
            val effJumatKeu = settings.jumat.keuangan.records[effKeuDate]

            putJsonObject("jumat") {
                put("enabled", settings.jumat.enabled)
                put("waktuJumatDuration", settings.jumat.waktuJumatDuration)
                put("khutbahDuration", settings.jumat.khutbahDuration)
                put("sholatJumatDuration", settings.jumat.sholatJumatDuration)
                put("khutbahMessage", settings.jumat.khutbahMessage)
                put("sholatMessage", settings.jumat.sholatMessage)
                put("khotib", effJumatRec?.khotib.orEmpty())
                put("imam", effJumatRec?.imam.orEmpty())
                put("muadzin", effJumatRec?.muadzin.orEmpty())
                put("keuanganDate", effKeuDate)
                put("customTitle", settings.jumat.keuangan.customTitle)
                put("source", settings.jumat.keuangan.source)
                put("saldoKasLalu", effJumatKeu?.saldoKasLalu ?: settings.jumat.keuangan.saldoKasLalu)
                put("pemasukan", effJumatKeu?.pemasukan ?: 0L)
                put("pengeluaran", effJumatKeu?.pengeluaran ?: 0L)
                put("saldoAkhir", effJumatKeu?.saldoAkhir ?: settings.jumat.keuangan.saldoKasLalu)
                putJsonArray("entries") {
                    (effJumatKeu?.entries ?: emptyList()).forEach { e ->
                        addJsonObject {
                            put("id", e.id)
                            put("description", e.description)
                            put("amount", e.amount)
                            put("type", e.type)
                        }
                    }
                }
            }

            val nightIdx = (settings.ramadhan.currentNight - 1).coerceIn(0, 29)
            val ramPetugas = settings.ramadhan.petugas.getOrNull(nightIdx)
            val ramKeu = settings.ramadhan.keuangan.getOrNull(nightIdx)
            val ramTakjil = settings.ramadhan.takjil[settings.ramadhan.currentNight.toString()].orEmpty()

            putJsonObject("ramadhan") {
                put("isRamadhan", settings.ramadhan.isRamadhan)
                put("currentNight", settings.ramadhan.currentNight)
                put("hijriYear", settings.ramadhan.hijriYear)
                put("penceramah", ramPetugas?.penceramah.orEmpty())
                put("imamTarawih", ramPetugas?.imamTarawih.orEmpty())
                put("imamWitir", ramPetugas?.imamWitir.orEmpty())
                put("saldoAwal", ramKeu?.saldoAwal ?: 0L)
                put("penerimaanPria", ramKeu?.penerimaanPria ?: 0L)
                put("penerimaanWanita", ramKeu?.penerimaanWanita ?: 0L)
                put("pengeluaran", ramKeu?.pengeluaran ?: 0L)
                putJsonArray("takjil") { ramTakjil.forEach { add(it) } }
                put("sirineEnabled", settings.ramadhan.sirine.enabled)
                put("sirineMaghrib", settings.ramadhan.sirine.playAtMaghrib)
                put("sirineImsak", settings.ramadhan.sirine.playAtImsak)
                put("sirineSahur", settings.ramadhan.sirine.playAtSahur)
                put("sirineSahurTime", settings.ramadhan.sirine.sahurTime)
                put("sirineRepeat", settings.ramadhan.sirine.repeatCount)
            }

            putJsonObject("standbySlideshow") {
                put("enabled", settings.standbySlideshow.enabled)
                put("slideDuration", settings.standbySlideshow.durationMain)
                put("repeatCount", settings.standbySlideshow.repeatCount)
                put("showNextPrayerCard", settings.standbySlideshow.showNextPrayerCard)
                put("nextPrayerThresholdMinutes", settings.standbySlideshow.nextPrayerThresholdMinutes)
            }

            putJsonObject("idulFitri") {
                put("enabled", settings.idulFitri.enabled)
                put("khotib", settings.idulFitri.petugas.khotib)
                put("imam", settings.idulFitri.petugas.imam)
                put("bilal", settings.idulFitri.petugas.bilal)
                put("sholatDuration", settings.idulFitri.sholatDuration)
                put("khutbahDuration", settings.idulFitri.khutbahDuration)
                put("sholatMessage", settings.idulFitri.sholatMessage)
                put("khutbahMessage", settings.idulFitri.khutbahMessage)
            }

            putJsonObject("idulAdha") {
                put("enabled", settings.idulAdha.enabled)
                put("infoQurbanEnabled", settings.infoQurban.enabled)
                put("khotib", settings.idulAdha.petugas.khotib)
                put("imam", settings.idulAdha.petugas.imam)
                put("bilal", settings.idulAdha.petugas.bilal)
                putJsonArray("qurbanGroups") {
                    settings.idulAdha.qurbanGroups.forEach { g ->
                        addJsonObject {
                            put("id", g.id)
                            put("groupName", g.groupName)
                            put("animalType", g.animalType)
                            putJsonArray("members") { g.members.forEach { add(it) } }
                        }
                    }
                }
            }

            val firstSs = settings.screensaver.schedules.firstOrNull()
            putJsonObject("screensaver") {
                put("enabled", settings.screensaver.enabled)
                put("startTime", firstSs?.startTime ?: "22:00")
                put("endTime", firstSs?.endTime ?: "03:30")
            }

            putJsonObject("switchConfig") {
                put("enabled", settings.switchSettings.enabled)
                put("connectionType", settings.switchSettings.connectionType)
                put("wifiIp", settings.switchSettings.wifiIp)
                put("usbPort", settings.switchSettings.usbPort)
                put("baudRate", settings.switchSettings.baudRate)
                putJsonArray("channels") {
                    settings.switchSettings.switches.forEach { ch ->
                        addJsonObject {
                            put("id", ch.id)
                            put("name", ch.name)
                            put("pin", ch.pin)
                            put("enabled", ch.enabled)
                            put("triggerMode", ch.triggerMode)
                            put("onState", ch.onState)
                            put("onStateOffset", ch.onStateOffset)
                            put("offState", ch.offState)
                            put("offStateOffset", ch.offStateOffset)
                            put("manualState", ch.manualState)
                            putJsonObject("schedules") {
                                listOf("fajr", "dhuhr", "asr", "maghrib", "isha").forEach { pid ->
                                    val s = ch.schedules[pid]
                                    putJsonObject(pid) {
                                        put("onOffset", s?.onOffset)
                                        put("offOffset", s?.offOffset)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            putJsonArray("users") {
                settings.users.forEach { u ->
                    addJsonObject {
                        put("id", u.id)
                        put("username", u.username)
                        put("role", u.role)
                    }
                }
            }

            putJsonArray("logs") {
                id.algaffar.jwstv.data.logging.AppLogger.logsFlow.value
                    .takeLast(25)
                    .reversed()
                    .forEach { entry ->
                        addJsonObject {
                            put("time", entry.timestamp)
                            put("level", entry.level.name)
                            put("tag", entry.tag)
                            put("message", entry.message)
                        }
                    }
            }

            putJsonObject("masjidQr") {
                put("enabled", settings.masjidQr.enabled)
                put("showFloatingOnMain", settings.masjidQr.showFloatingOnMain)
                put("link", settings.masjidQr.link)
                put("label", settings.masjidQr.label)
                put("bankName", settings.masjidQr.bankName)
                put("nomorRekening", settings.masjidQr.nomorRekening)
                put("atasNama", settings.masjidQr.atasNama)
            }

            putJsonObject("onlineFinance") {
                put("enabled", settings.onlineFinance.enabled)
                put("showFloatingOnMain", settings.onlineFinance.showFloatingOnMain)
                put("url", settings.onlineFinance.sheetUrl)
                put("reportUrl", settings.onlineFinance.reportUrl)
                put("bankName", settings.onlineFinance.bankName)
                put("nomorRekening", settings.onlineFinance.nomorRekening)
                put("atasNama", settings.onlineFinance.atasNama)
                put("refreshIntervalMinutes", settings.onlineFinance.refreshIntervalMinutes)
            }

            putJsonObject("tartilTarhimMedia") {
                put("tartilMediaType", settings.media.tartilMediaType)
                put("tartilVideo", settings.media.tartilVideo)
                put("tartilSlideInterval", settings.media.tartilSlideInterval)
                putJsonArray("tartilImages") {
                    settings.media.tartilImages.forEach { add(it) }
                }
                put("tarhimMediaType", settings.media.tarhimMediaType)
                put("tarhimVideo", settings.media.tarhimVideo)
                put("tarhimSlideInterval", settings.media.tarhimSlideInterval)
                putJsonArray("tarhimImages") {
                    settings.media.tarhimImages.forEach { add(it) }
                }
            }

            putJsonObject("audioSlots") {
                audioInv.forEach { (category, slots) ->
                    putJsonArray(category) {
                        slots.forEach { add(it) }
                    }
                }
            }

            putJsonObject("customAudioConfig") {
                val ca = settings.customAudio
                put("liveAudioFile", ca.liveAudioFile)
                put("liveDurationMinutes", ca.liveDurationMinutes)
                put("isPlayingLive", ca.isPlayingLive)
                putJsonArray("schedules") {
                    ca.schedules.forEach { s ->
                        addJsonObject {
                            put("id", s.id)
                            put("name", s.name)
                            put("audioFile", s.audioFile)
                            put("scheduleType", s.scheduleType)
                            put("dayOfWeek", s.dayOfWeek)
                            put("date", s.date)
                            put("time", s.time)
                            put("durationMinutes", s.durationMinutes)
                            put("enabled", s.enabled)
                        }
                    }
                }
            }

            val storage = storageHealthProvider()
            val clockStatus = runCatching { clockStatusProvider() }.getOrDefault(ClockStatusSnapshot())
            val thermalC = readDeviceThermalTempC()
            putJsonObject("system") {
                if (thermalC != null) {
                    put("thermalTempC", thermalC)
                }
                put("ram", ramPct)
                sysRamPct?.let { put("systemRam", it) }
                put("uptime", uptimeStr)
                put("clockSuspect", clockStatus.isClockSuspect)
                put("clockOffsetMs", clockStatus.clockOffsetMs)
                if (clockStatus.deviceTimeIso.isNotBlank()) {
                    put("deviceTimeIso", clockStatus.deviceTimeIso)
                }
                put("onboardingCompleted", settings.onboardingCompleted)
                putJsonObject("storage") {
                    put("usableBytes", storage.usableInternalBytes)
                    put("totalBytes", storage.totalInternalBytes)
                    put("usedPercent", storage.internalUsedPercent)
                    put("jwsAssetsBytes", storage.jwsAssetsUsedBytes)
                    put("imagesBytes", storage.imagesBytes)
                    put("audioBytes", storage.audioBytes)
                    put("videosBytes", storage.videosBytes)
                    put("totalFiles", storage.totalFileCount)
                    put("orphanCount", storage.orphanFileCount)
                    put("orphanBytes", storage.orphanBytes)
                    put("isLowStorage", storage.isLowStorage)
                    put("usableFormatted", MediaStorageManager.formatByteSize(storage.usableInternalBytes))
                    put("assetsFormatted", MediaStorageManager.formatByteSize(storage.jwsAssetsUsedBytes))
                    put("orphanFormatted", MediaStorageManager.formatByteSize(storage.orphanBytes))
                }
            }
        }
    }

    private fun readDeviceThermalTempC(): Double? {
        val zonePaths = listOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/devices/virtual/thermal/thermal_zone0/temp"
        )
        for (path in zonePaths) {
            val raw = runCatching {
                val f = File(path)
                if (f.exists() && f.canRead()) f.readText().trim().toDoubleOrNull() else null
            }.getOrNull() ?: continue
            val celsius = if (raw > 1000.0) raw / 1000.0 else raw
            if (celsius in 10.0..120.0) {
                return (celsius * 10.0).toInt() / 10.0
            }
        }
        return null
    }

    private fun loadRemoteHtml(): String {
        val cached = cachedHtmlRef.get()
        if (cached != null) return cached

        val loaded = try {
            context?.assets?.open("remote/remote_html.html")?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            }
        } catch (_: Exception) {
            null
        } ?: FALLBACK_REMOTE_HTML

        cachedHtmlRef.set(loaded)
        return loaded
    }

    private fun extractClientIp(call: ApplicationCall): String {
        val forwarded = call.request.header("X-Forwarded-For")
            ?.split(',')
            ?.firstOrNull()
            ?.trim()
        if (!forwarded.isNullOrBlank()) return forwarded
        return call.request.local.remoteHost
    }

    companion object {
        const val DEFAULT_PORT = 1421
        const val DEFAULT_TOKEN_LENGTH = RemoteTokenStore.DEFAULT_TOKEN_LENGTH
        private const val MAX_CONTENT_LENGTH = 65_536
        private const val MAX_APK_UPLOAD_BYTES = 180L * 1024L * 1024L

        private val SUPERADMIN_ONLY_ACTIONS = setOf(
            "update_admin_pin",
            "regenerate_remote_token",
            "clear_logs",
            "factory_reset",
            "ota_install",
            "check_ota_update"
        )

        fun maxUploadBytesForCategory(category: String): Long = when (category.lowercase()) {
            "ota_apk" -> MAX_APK_UPLOAD_BYTES
            "slide_video", "video" -> MediaStorageManager.MAX_VIDEO_UPLOAD_BYTES
            "tartil", "tarhim", "adzan", "custom" -> MediaStorageManager.MAX_AUDIO_UPLOAD_BYTES
            else -> MediaStorageManager.MAX_IMAGE_UPLOAD_BYTES
        }

        fun generateSecureToken(length: Int = DEFAULT_TOKEN_LENGTH): String =
            RemoteTokenStore.generateSecureToken(length)

        fun sanitizeInput(input: String, maxLength: Int = 256): String {
            return input
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .trim()
                .take(maxLength)
        }

        fun sanitizeFileSlug(raw: String): String {
            return raw.lowercase()
                .replace(Regex("[^a-z0-9]+"), "-")
                .trim('-')
                .ifBlank { "masjid" }
                .take(40)
        }

        /**
         * Restricts CORS origins strictly to localhost or private LAN IP ranges
         * so external websites opened in a caretaker's phone browser cannot trigger cross-site commands.
         */
        fun isAllowedLanOrigin(origin: String?): Boolean {
            if (origin.isNullOrBlank()) return true
            val host = runCatching { URI(origin.trim()).host }.getOrNull()?.lowercase()?.trim() ?: return false
            if (host.isEmpty()) return false
            if (host == "localhost" || host == "127.0.0.1" || host == "::1" || host == "[::1]") return true
            return SwitchController.isPrivateOrLocalIp(host)
        }

        fun isActionAllowedForRole(action: String, role: UserRole): Boolean {
            val normalized = action.trim().lowercase()
            if (normalized in SUPERADMIN_ONLY_ACTIONS) {
                return role == UserRole.SUPERADMIN
            }
            return role == UserRole.SUPERADMIN || role == UserRole.ADMIN
        }

        /**
         * Resolves the authorized [UserRole] from `Authorization: Bearer <token>` against:
         * 1. The server's persistent random [serverToken] (grants [UserRole.SUPERADMIN] for QR-paired devices)
         * 2. Any configured `superadmin` or `admin` account PIN in [settings] (no hardcoded backdoor!).
         */
        fun resolveAuthorizedRole(
            authHeader: String?,
            serverToken: String,
            settings: AppStateSettings
        ): UserRole? {
            if (authHeader.isNullOrBlank()) return null
            val prefix = "Bearer "
            if (!authHeader.startsWith(prefix, ignoreCase = true)) return null
            val provided = authHeader.substring(prefix.length).trim()
            if (provided.isEmpty()) return null

            if (serverToken.isNotBlank() && MessageDigest.isEqual(
                    provided.toByteArray(Charsets.UTF_8),
                    serverToken.toByteArray(Charsets.UTF_8)
                )
            ) {
                return UserRole.SUPERADMIN
            }

            val matchedUser = AuthManager.authenticate(
                username = "",
                rawPin = provided,
                users = settings.users
            ) ?: return null

            val role = UserRole.fromId(matchedUser.role)
            return if (role == UserRole.SUPERADMIN || role == UserRole.ADMIN) role else null
        }

        fun isAuthorizedHeader(
            authHeader: String?,
            serverToken: String,
            settings: AppStateSettings
        ): Boolean = resolveAuthorizedRole(authHeader, serverToken, settings) != null

        @Volatile
        private var cachedLocalIp: String = ""

        @Volatile
        private var cachedLocalIpAtNs: Long = 0L

        private const val LOCAL_IP_CACHE_TTL_NS: Long = 15_000_000_000L

        /**
         * Returns the most recently resolved LAN IP without blocking on network interface syscalls
         * when a cached value is available; refreshes at most once every 15 seconds unless [forceRefresh] is true.
         */
        fun resolveLocalNetworkIp(forceRefresh: Boolean = false): String {
            val nowNs = System.nanoTime()
            val existing = cachedLocalIp
            if (!forceRefresh && existing.isNotEmpty() && (nowNs - cachedLocalIpAtNs) in 0L until LOCAL_IP_CACHE_TTL_NS) {
                return existing
            }
            val fresh = queryLocalNetworkIpNow()
            cachedLocalIp = fresh
            cachedLocalIpAtNs = nowNs
            return fresh
        }

        /**
         * Non-blocking snapshot of the cached LAN IP for the 1Hz Main-thread ticker.
         * Call [refreshCachedLocalIp] from an IO coroutine to keep it up to date.
         */
        fun getCachedLocalIpOrLoopback(): String = cachedLocalIp.ifEmpty { "127.0.0.1" }

        /**
         * Refreshes the cached LAN IP (safe to call from Dispatchers.IO).
         */
        fun refreshCachedLocalIp(): String = resolveLocalNetworkIp(forceRefresh = true)

        /**
         * Finds the device's LAN IPv4 address, prioritizing `192.168.x.x` > `10.x.x.x` > `172.x.x.x`.
         * Mirrors `get_local_ip` in `src-tauri/src/lib.rs`.
         */
        private fun queryLocalNetworkIpNow(): String {
            return try {
                val candidates = mutableListOf<String>()
                val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
                for (netIf in interfaces.asSequence()) {
                    val nameLower = netIf.name.lowercase()
                    if (!netIf.isUp || netIf.isLoopback ||
                        nameLower.contains("docker") ||
                        nameLower.contains("veth") ||
                        nameLower.contains("tun") ||
                        nameLower.contains("dummy")
                    ) {
                        continue
                    }
                    for (addr in netIf.inetAddresses.asSequence()) {
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            candidates.add(addr.hostAddress ?: continue)
                        }
                    }
                }
                candidates.sortedBy { ip ->
                    when {
                        ip.startsWith("192.168.") -> 0
                        ip.startsWith("10.") -> 1
                        ip.startsWith("172.") -> 2
                        else -> 3
                    }
                }.firstOrNull() ?: "127.0.0.1"
            } catch (_: Exception) {
                "127.0.0.1"
            }
        }

        private const val REMOTE_MANIFEST_JSON = """{
            "name": "Al-Gaffar TV Remote",
            "short_name": "Al-Gaffar TV",
            "description": "Remote Control Center for Al-Gaffar TV JWS Masjid",
            "start_url": "/remote",
            "scope": "/",
            "display": "standalone",
            "orientation": "portrait",
            "background_color": "#0a0a0c",
            "theme_color": "#0a0a0c",
            "icons": [
                {
                    "src": "app-icon.png",
                    "sizes": "192x192",
                    "type": "image/png",
                    "purpose": "any maskable"
                }
            ]
        }"""

        private const val REMOTE_SW_JS = """
            const CACHE_NAME = 'jws-remote-v4';
            self.addEventListener('install', (e) => {
                self.skipWaiting();
            });
            self.addEventListener('activate', (e) => {
                e.waitUntil(
                    caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== CACHE_NAME).map((k) => caches.delete(k))))
                );
                self.clients.claim();
            });
            self.addEventListener('fetch', (e) => {
                if (e.request.url.includes('/api/') || e.request.method !== 'GET') return;
                e.respondWith(
                    fetch(e.request)
                        .then((res) => {
                            const copy = res.clone();
                            caches.open(CACHE_NAME).then((c) => c.put(e.request, copy));
                            return res;
                        })
                        .catch(() => caches.match(e.request))
                );
            });
        """

        // 1x1 Emerald PNG icon fallback
        private val MINIMAL_PNG_ICON = byteArrayOf(
            0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x02, 0x00, 0x00, 0x00, 0x90.toByte(), 0x77, 0x53,
            0xDE.toByte(), 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
            0x54, 0x08, 0xD7.toByte(), 0x63, 0x60, 0xF8.toByte(), 0xCF.toByte(), 0x80.toByte(),
            0x00, 0x00, 0x02, 0x05, 0x01, 0x02, 0xA6.toByte(), 0x6C,
            0x9E.toByte(), 0x59, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45,
            0x4E, 0x44, 0xAE.toByte(), 0x42, 0x60, 0x82.toByte()
        )

        private const val FALLBACK_REMOTE_HTML = """<!DOCTYPE html><html lang="id"><head><meta charset="UTF-8"><title>Smart JWS Remote</title></head><body><h1>JWS SMART REMOTE</h1></body></html>"""
    }
}

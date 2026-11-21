package id.algaffar.jwstv.data.server

import android.content.Context
import android.os.SystemClock
import id.algaffar.jwstv.data.auth.AuthManager
import id.algaffar.jwstv.data.media.MediaStorageManager
import id.algaffar.jwstv.data.media.StorageHealthSnapshot
import id.algaffar.jwstv.domain.model.AppState
import id.algaffar.jwstv.domain.model.AppStateSettings
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.request.receiveStream
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
    val authToken: String = generateSecureToken(),
    private val settingsProvider: () -> AppStateSettings = { AppStateSettings() },
    private val runtimeStatusProvider: () -> RuntimeServerSnapshot = { RuntimeServerSnapshot() },
    private val audioInventoryProvider: () -> Map<String, List<String>> = { emptyMap() },
    private val storageHealthProvider: () -> StorageHealthSnapshot = { StorageHealthSnapshot() },
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
                install(CORS) {
                    anyHost()
                    allowMethod(HttpMethod.Get)
                    allowMethod(HttpMethod.Post)
                    allowMethod(HttpMethod.Options)
                    allowHeader(HttpHeaders.Authorization)
                    allowHeader(HttpHeaders.ContentType)
                }

                routing {
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

                    get("/api/status") {
                        if (!checkRateLimit(call)) return@get
                        if (!checkAuthorized(call)) return@get

                        val statusJson = buildStatusJson()
                        call.respondText(
                            text = statusJson.toString(),
                            contentType = ContentType.Application.Json,
                            status = HttpStatusCode.OK
                        )
                    }

                    post("/api/command") {
                        if (!checkRateLimit(call)) return@post
                        if (!checkAuthorized(call)) return@post

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

                        onCommandReceived(
                            RemoteCommandPayload(
                                action = sanitizeInput(action, 64),
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
                        if (!checkAuthorized(call)) return@post

                        val category = sanitizeInput(call.request.queryParameters["category"].orEmpty(), 32).lowercase()
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

    private suspend fun checkAuthorized(call: ApplicationCall): Boolean {
        val authHeader = call.request.header(HttpHeaders.Authorization)
        val settings = settingsProvider()
        if (!isAuthorizedHeader(authHeader, authToken, settings)) {
            call.respondText(
                text = """{"error":"Unauthorized"}""",
                contentType = ContentType.Application.Json,
                status = HttpStatusCode.Unauthorized
            )
            return false
        }
        return true
    }

    internal fun buildStatusJson(): JsonObject {
        val settings = settingsProvider()
        val runtime = runtimeStatusProvider()
        val audioInv = runCatching { audioInventoryProvider() }.getOrDefault(emptyMap())
        val runtimeMem = Runtime.getRuntime()
        val totalMem = runtimeMem.totalMemory().coerceAtLeast(1L)
        val usedMem = (totalMem - runtimeMem.freeMemory()).coerceAtLeast(0L)
        val ramPct = ((usedMem.toDouble() / totalMem.toDouble()) * 100.0 * 10.0).toInt() / 10.0

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
            put("activeLiveVideoId", settings.live.customVideoId.ifBlank { "Cm1v4bteXbI" })

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
            }

            putJsonObject("prayerSettings") {
                val map = mapOf(
                    "fajr" to settings.prayers.fajr,
                    "dhuhr" to settings.prayers.dhuhr,
                    "asr" to settings.prayers.asr,
                    "maghrib" to settings.prayers.maghrib,
                    "isha" to settings.prayers.isha
                )
                for ((key, cfg) in map) {
                    putJsonObject(key) {
                        put("isTartil", cfg.isTartil && cfg.tartilDuration > 0)
                        put("isTarhim", cfg.isTarhim && cfg.tarhimDuration > 0)
                        put("isAdzan", cfg.isAdzan)
                        put("iqomahDuration", cfg.iqomahDuration)
                        put("sholatDuration", cfg.sholatDuration)
                    }
                }
            }

            putJsonArray("overlayTextList") {
                settings.overlayTextList.forEach { add(it) }
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

            putJsonArray("slides") {
                settings.slides.forEach { s ->
                    addJsonObject {
                        put("id", s.id)
                        put("title", s.title)
                        put("type", s.type)
                        put("duration", s.duration)
                        put("enabled", s.enabled)
                        put("showInMain", s.showInMain)
                        put("showInSlideshow", s.showInSlideshow)
                    }
                }
            }

            putJsonObject("infoSettings") {
                put("enableSunnahFasting", settings.infoSettings.enableSunnahFasting)
                put("lookaheadDays", settings.infoSettings.lookaheadDays)
            }

            putJsonObject("jumat") {
                put("enabled", settings.jumat.enabled)
                put("khotib", effJumatRec?.khotib.orEmpty())
                put("imam", effJumatRec?.imam.orEmpty())
                put("muadzin", effJumatRec?.muadzin.orEmpty())
            }

            putJsonObject("ramadhan") {
                put("isRamadhan", settings.ramadhan.isRamadhan)
                put("currentNight", settings.ramadhan.currentNight)
                put("hijriYear", settings.ramadhan.hijriYear)
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
                put("url", settings.onlineFinance.sheetUrl)
                put("reportUrl", settings.onlineFinance.reportUrl)
            }

            putJsonObject("audioSlots") {
                audioInv.forEach { (category, slots) ->
                    putJsonArray(category) {
                        slots.forEach { add(it) }
                    }
                }
            }

            val storage = storageHealthProvider()
            putJsonObject("system") {
                put("cpu", 12.5)
                put("ram", ramPct)
                put("uptime", uptimeStr)
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
        private const val MAX_CONTENT_LENGTH = 65_536

        fun maxUploadBytesForCategory(category: String): Long = when (category.lowercase()) {
            "slide_video", "video" -> MediaStorageManager.MAX_VIDEO_UPLOAD_BYTES
            "tartil", "tarhim", "adzan", "custom" -> MediaStorageManager.MAX_AUDIO_UPLOAD_BYTES
            else -> MediaStorageManager.MAX_IMAGE_UPLOAD_BYTES
        }

        fun generateSecureToken(length: Int = 8): String {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            val random = SecureRandom()
            return buildString(length) {
                repeat(length) {
                    append(chars[random.nextInt(chars.length)])
                }
            }
        }

        fun sanitizeInput(input: String, maxLength: Int = 256): String {
            return input
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .trim()
                .take(maxLength)
        }

        /**
         * Validates `Authorization: Bearer <token>` against:
         * 1. The server's generated [serverToken]
         * 2. Any valid `superadmin` or `admin` PIN in [settings] (including default `732911`).
         */
        fun isAuthorizedHeader(
            authHeader: String?,
            serverToken: String,
            settings: AppStateSettings
        ): Boolean {
            if (authHeader.isNullOrBlank()) return false
            val prefix = "Bearer "
            if (!authHeader.startsWith(prefix, ignoreCase = true)) return false
            val provided = authHeader.substring(prefix.length).trim()
            if (provided.isEmpty()) return false

            if (provided.equals(serverToken, ignoreCase = true)) return true
            if (provided == AuthManager.DEFAULT_SUPERADMIN_PIN) return true

            val matchedUser = AuthManager.authenticate(
                username = "",
                rawPin = provided,
                users = settings.users
            )
            return matchedUser != null && matchedUser.role.lowercase() in setOf("superadmin", "admin")
        }

        /**
         * Finds the device's LAN IPv4 address, prioritizing `192.168.x.x` > `10.x.x.x` > `172.x.x.x`.
         * Mirrors `get_local_ip` in `src-tauri/src/lib.rs`.
         */
        fun resolveLocalNetworkIp(): String {
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
            "name": "JWS Remote Control",
            "short_name": "JWS Remote",
            "description": "Premium Control Center for JWS Al-Gaffar",
            "start_url": "/remote",
            "scope": "/",
            "display": "standalone",
            "orientation": "portrait",
            "background_color": "#0a0a0c",
            "theme_color": "#0a0a0c",
            "icons": [
                {
                    "src": "alGaffar.png",
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

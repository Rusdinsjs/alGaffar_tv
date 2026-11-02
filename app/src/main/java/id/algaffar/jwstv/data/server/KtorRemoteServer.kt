package id.algaffar.jwstv.data.server

import android.content.Context
import android.os.SystemClock
import id.algaffar.jwstv.data.auth.AuthManager
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
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.net.Inet4Address
import java.net.NetworkInterface
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

data class RemoteCommandPayload(
    val action: String,
    val rawJson: JsonObject
) {
    fun stringArg(key: String): String? =
        (rawJson[key] as? JsonPrimitive)?.contentOrNull?.let { KtorRemoteServer.sanitizeInput(it, 500) }

    fun doubleArg(key: String): Double? =
        (rawJson[key] as? JsonPrimitive)?.doubleOrNull
}

/**
 * Sliding-window IP rate limiter (default 120 requests per 60 seconds).
 * Mirrors `RateLimiter` in `src-tauri/src/security.rs`.
 */
class IpRateLimiter(
    private val maxRequests: Int = 120,
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
 * (`GET /api/status`, `POST /api/command`) with Bearer token authentication and IP rate limiting.
 *
 * Mirrors `src-tauri/src/server/mod.rs` and `src-tauri/src/server/handlers.rs`.
 */
class KtorRemoteServer(
    private val context: Context? = null,
    val port: Int = DEFAULT_PORT,
    val authToken: String = generateSecureToken(),
    private val settingsProvider: () -> AppStateSettings = { AppStateSettings() },
    private val runtimeStatusProvider: () -> RuntimeServerSnapshot = { RuntimeServerSnapshot() },
    private val onCommandReceived: (RemoteCommandPayload) -> Unit = {}
) {
    data class RuntimeServerSnapshot(
        val currentState: AppState = AppState.STANDBY,
        val isAudioPlaying: Boolean = false,
        val isCustomAudioPlaying: Boolean = false
    )

    private var engine: CIOApplicationEngine? = null
    private val rateLimiter = IpRateLimiter(maxRequests = 120, windowMillis = 60_000L)
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

        return buildJsonObject {
            put("state", runtime.currentState.serialName)
            put("audio", runtime.isAudioPlaying)
            put("customAudio", runtime.isCustomAudioPlaying || settings.customAudio.isPlayingLive)
            put("theme", settings.themeSettings.currentThemeId)
            put("layout", settings.layoutSettings.position.serialName)
            put("jumatEnabled", settings.jumat.enabled)
            put("ramadhanEnabled", settings.ramadhan.isRamadhan)
            put("idulFitriEnabled", settings.idulFitri.enabled)
            put("idulAdhaEnabled", settings.idulAdha.enabled)
            put("slideshowEnabled", settings.standbySlideshow.enabled)
            put("audioEngine", "backend")
            put("activeLiveVideoId", settings.live.customVideoId.ifBlank { "Cm1v4bteXbI" })

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
                        put("isTartil", cfg.tartilDuration > 0)
                        put("isTarhim", cfg.tarhimDuration > 0)
                        put("isAdzan", cfg.isAdzan)
                    }
                }
            }

            putJsonObject("system") {
                put("cpu", 12.5)
                put("ram", ramPct)
                put("uptime", uptimeStr)
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
        private const val MAX_CONTENT_LENGTH = 32_768

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
            "start_url": "/",
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
            const CACHE_NAME = 'jws-remote-v3';
            const ASSETS_TO_CACHE = ['/', '/remote', '/manifest.json', '/alGaffar.png'];
            self.addEventListener('install', (e) => e.waitUntil(caches.open(CACHE_NAME).then((c) => c.addAll(ASSETS_TO_CACHE))));
            self.addEventListener('fetch', (e) => {
                if (e.request.url.includes('/api/')) return;
                e.respondWith(caches.match(e.request).then((r) => r || fetch(e.request)));
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

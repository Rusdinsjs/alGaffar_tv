package id.algaffar.jwstv.data.hardware

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager
import android.os.Build
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import id.algaffar.jwstv.domain.engine.DailyPrayerSchedule
import id.algaffar.jwstv.domain.model.AppState
import id.algaffar.jwstv.domain.model.PrayerId
import id.algaffar.jwstv.domain.model.SwitchChannelConfig
import id.algaffar.jwstv.domain.model.SwitchSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * 8-Channel Hardware Relay Controller (Lampu, AC, Amplifier Toa Luar & Audio Dalam).
 * Supports both:
 * 1. WiFi HTTP Relay (`GET http://{wifiIp}/relay/{pin}/{on|off}`) with private IP security validation.
 * 2. USB OTG Serial Relay (`{pin}:{1|0}\n` via `usb-serial-for-android` CH340/CP210x/FTDI).
 *
 * Mirrors `src/components/SwitchController.tsx` and `src-tauri/src/lib.rs` (`control_relay`).
 */
class SwitchController(
    private val context: Context? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val httpClient: OkHttpClient = id.algaffar.jwstv.SharedHttpClient.relayClient
) {
    private val activePinStates = ConcurrentHashMap<Int, Boolean>()
    private val _pinStateFlow = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val pinStateFlow: StateFlow<Map<Int, Boolean>> = _pinStateFlow.asStateFlow()

    private var lastObservedAppState: AppState? = null
    private var stateEnteredAtEpochSec: Long = 0L

    private val usbLock = Any()
    @Volatile
    private var openSerialPort: UsbSerialPort? = null
    @Volatile
    private var openDeviceName: String? = null
    @Volatile
    private var openBaudRate: Int = -1

    @Volatile
    private var lastDispatchFailureAtMs: Long = 0L
    @Volatile
    private var lastAutoPermissionRequestAtMs: Long = 0L

    /**
     * Pure calculation of desired pin states (`pin -> Boolean`) for a given timestamp,
     * prayer schedule, and current [AppState].
     */
    fun computeDesiredPinStates(
        now: ZonedDateTime,
        currentState: AppState,
        stateElapsedSeconds: Long,
        switchSettings: SwitchSettings,
        schedule: DailyPrayerSchedule?
    ): Map<Int, Boolean> {
        if (!switchSettings.enabled) return emptyMap()

        val maxPins = switchSettings.maxRelayPins()
        val result = linkedMapOf<Int, Boolean>()
        val nowEpochSec = now.toEpochSecond()
        val zoneId: ZoneId = now.zone

        for (channel in switchSettings.switches) {
            if (!channel.enabled || channel.pin !in 1..maxPins) continue

            when (channel.triggerMode.lowercase()) {
                "prayer" -> {
                    if (schedule != null) {
                        val shouldBeOn = evaluatePrayerTrigger(
                            channel = channel,
                            nowEpochSec = nowEpochSec,
                            zoneId = zoneId,
                            schedule = schedule
                        )
                        // Merge: if any channel mapped to the same pin requests ON, keep it ON
                        val existing = result[channel.pin] ?: false
                        result[channel.pin] = existing || shouldBeOn
                    }
                }

                "state" -> {
                    val currentPinVal = activePinStates[channel.pin] ?: false
                    val evaluated = evaluateStateTrigger(
                        channel = channel,
                        currentState = currentState,
                        stateElapsedSeconds = stateElapsedSeconds,
                        currentPinActive = currentPinVal
                    )
                    if (evaluated != null) {
                        result[channel.pin] = evaluated
                    }
                }
            }
        }

        return result
    }

    private fun evaluatePrayerTrigger(
        channel: SwitchChannelConfig,
        nowEpochSec: Long,
        zoneId: ZoneId,
        schedule: DailyPrayerSchedule
    ): Boolean {
        if (channel.schedules.isNotEmpty()) {
            data class SwitchEvent(val timeSec: Long, val turnOn: Boolean)
            val events = mutableListOf<SwitchEvent>()
            val daySec = 86_400L

            for (dayOffset in -1L..1L) {
                for ((prayerSerial, sched) in channel.schedules) {
                    val prayerId = PrayerId.fromId(prayerSerial) ?: continue
                    val baseEpochSec = schedule.timeFor(prayerId).toEpochSecond() + (dayOffset * daySec)

                    sched.onOffset?.let { onMin ->
                        events.add(SwitchEvent(baseEpochSec - (onMin.toLong() * 60L), true))
                    }
                    sched.offOffset?.let { offMin ->
                        events.add(SwitchEvent(baseEpochSec + (offMin.toLong() * 60L), false))
                    }
                }
            }

            if (events.isEmpty()) return false
            // Sort ascending by timestamp; if tied, OFF takes precedence over ON for safety
            events.sortWith(compareBy<SwitchEvent> { it.timeSec }.thenBy { it.turnOn })

            var latestState = false
            for (ev in events) {
                if (ev.timeSec <= nowEpochSec) {
                    latestState = ev.turnOn
                } else {
                    break
                }
            }
            return latestState
        }

        // Legacy single-offset fallback when schedules map is empty
        val minutesBefore = kotlin.math.abs(channel.onOffset).toLong()
        val minutesAfter = kotlin.math.abs(channel.offOffset).toLong()

        for (prayerSerial in channel.activePrayers) {
            val prayerId = PrayerId.fromId(prayerSerial) ?: continue
            val prayerZonedDt = schedule.timeFor(prayerId)
            val prayerEpochSec = prayerZonedDt.toEpochSecond()

            val startSec = prayerEpochSec - (minutesBefore * 60L)
            val endSec = prayerEpochSec + (minutesAfter * 60L)

            if (nowEpochSec >= startSec && nowEpochSec < endSec) {
                return true
            }
        }
        return false
    }

    private fun evaluateStateTrigger(
        channel: SwitchChannelConfig,
        currentState: AppState,
        stateElapsedSeconds: Long,
        currentPinActive: Boolean
    ): Boolean? {
        val stateSerial = currentState.serialName
        val onOffsetSec = (channel.onStateOffset.coerceAtLeast(0)) * 60L
        val offOffsetSec = (channel.offStateOffset.coerceAtLeast(0)) * 60L

        if (channel.onState.equals(stateSerial, ignoreCase = true) && stateElapsedSeconds >= onOffsetSec) {
            return true
        }
        if (channel.offState.equals(stateSerial, ignoreCase = true) && stateElapsedSeconds >= offOffsetSec) {
            return false
        }
        return currentPinActive
    }

    /**
     * Evaluates and dispatches hardware commands only for pins whose desired state changed.
     * State is committed to [activePinStates] only when hardware dispatch succeeds; on failure,
     * a 5-second backoff is applied before retrying automatically on the 1Hz ticker.
     */
    suspend fun evaluateAndDispatch(
        now: ZonedDateTime,
        currentState: AppState,
        switchSettings: SwitchSettings,
        schedule: DailyPrayerSchedule?
    ) {
        val nowSec = now.toEpochSecond()
        if (lastObservedAppState != currentState) {
            lastObservedAppState = currentState
            stateEnteredAtEpochSec = nowSec
        }
        val stateElapsedSec = (nowSec - stateEnteredAtEpochSec).coerceAtLeast(0L)

        val desired = computeDesiredPinStates(
            now = now,
            currentState = currentState,
            stateElapsedSeconds = stateElapsedSec,
            switchSettings = switchSettings,
            schedule = schedule
        )

        val nowMs = System.currentTimeMillis()
        if (lastDispatchFailureAtMs > 0L && (nowMs - lastDispatchFailureAtMs) < DISPATCH_RETRY_BACKOFF_MS) {
            return
        }

        for ((pin, shouldBeOn) in desired) {
            val previous = activePinStates[pin]
            if (previous != shouldBeOn) {
                val res = sendHardwareCommand(
                    pin = pin,
                    turnOn = shouldBeOn,
                    settings = switchSettings
                )
                if (res.isSuccess) {
                    activePinStates[pin] = shouldBeOn
                    _pinStateFlow.value = activePinStates.toMap()
                    lastDispatchFailureAtMs = 0L
                } else {
                    lastDispatchFailureAtMs = System.currentTimeMillis()
                    val errMsg = res.exceptionOrNull()?.message ?: "Gagal mengirim perintah relay"
                    id.algaffar.jwstv.data.logging.AppLogger.warn("RELAY", "Pin $pin -> ${if (shouldBeOn) "ON" else "OFF"} gagal: $errMsg")
                    break
                }
            }
        }
    }

    /**
     * Manually toggles a specific relay pin (1..maxRelayPins) and dispatches the command to WiFi or USB hardware.
     */
    suspend fun setManualPinState(
        pin: Int,
        turnOn: Boolean,
        settings: SwitchSettings
    ): Result<Unit> {
        val maxPins = settings.maxRelayPins()
        if (pin !in 1..maxPins) {
            return Result.failure(IllegalArgumentException("Pin relay $pin tidak aktif untuk profil ${settings.boardProfile} (maks $maxPins pin)"))
        }
        val res = sendHardwareCommand(pin = pin, turnOn = turnOn, settings = settings)
        if (res.isSuccess) {
            activePinStates[pin] = turnOn
            _pinStateFlow.value = activePinStates.toMap()
            lastDispatchFailureAtMs = 0L
        }
        return res
    }

    suspend fun sendHardwareCommand(
        pin: Int,
        turnOn: Boolean,
        settings: SwitchSettings
    ): Result<Unit> = withContext(ioDispatcher) {
        val maxPins = settings.maxRelayPins()
        if (pin !in 1..maxPins) {
            return@withContext Result.failure(IllegalArgumentException("Pin harus 1..$maxPins, diterima $pin"))
        }

        when (settings.connectionType.lowercase()) {
            "usb", "serial" -> sendUsbSerialRelayCommand(pin, turnOn, settings.baudRate, settings.usbPort)
            else -> sendWifiRelayCommand(settings.wifiIp, pin, turnOn)
        }
    }

    internal fun sendWifiRelayCommand(
        ip: String,
        pin: Int,
        turnOn: Boolean
    ): Result<Unit> = runCatching {
        val cleanIp = ip.trim()
        require(isPrivateOrLocalIp(cleanIp)) {
            "Only local/private IP addresses are allowed for WiFi relay. Got: $cleanIp"
        }
        val url = buildWifiRelayUrl(cleanIp, pin, turnOn)
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("WiFi Relay HTTP ${response.code}")
            }
        }
    }

    private fun selectUsbDriver(
        drivers: List<com.hoho.android.usbserial.driver.UsbSerialDriver>,
        preferredPort: String
    ): com.hoho.android.usbserial.driver.UsbSerialDriver {
        val trimmed = preferredPort.trim()
        if (trimmed.isNotEmpty() && !trimmed.equals("/dev/ttyUSB0", ignoreCase = true)) {
            drivers.firstOrNull { it.device.deviceName.equals(trimmed, ignoreCase = true) }?.let { return it }
        }
        return drivers.first()
    }

    /**
     * Checks if a supported USB Serial Relay adapter (CH340/CP210x/FTDI/PL2303) is attached
     * and requests Android USB Host permission if not yet granted.
     */
    fun requestUsbRelayPermission(preferredPort: String = ""): Result<String> = runCatching {
        val appContext = context ?: throw IllegalStateException("Android Context tidak tersedia")
        val usbManager = appContext.getSystemService(Context.USB_SERVICE) as? UsbManager
            ?: throw IllegalStateException("UsbManager tidak tersedia pada perangkat ini")

        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (drivers.isEmpty()) {
            throw IllegalStateException("Tidak ada modul USB Serial (CH340/CP210x/FTDI/PL2303) yang terdeteksi di port USB OTG")
        }

        val device = selectUsbDriver(drivers, preferredPort).device
        val deviceLabel = "${device.productName ?: "USB Serial"} (${device.deviceName})"
        if (usbManager.hasPermission(device)) {
            lastDispatchFailureAtMs = 0L
            return@runCatching "Izin USB OTG sudah aktif untuk $deviceLabel"
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val intent = Intent(ACTION_USB_PERMISSION).setPackage(appContext.packageName)
        val pendingIntent = PendingIntent.getBroadcast(appContext, 0, intent, flags)
        usbManager.requestPermission(device, pendingIntent)
        "Permintaan izin USB OTG dikirim untuk $deviceLabel. Tekan OK pada dialog izin di layar TV."
    }

    /**
     * Called by BroadcastReceiver when the user responds to the Android USB permission dialog.
     */
    fun onUsbPermissionResult(granted: Boolean, deviceName: String? = null) {
        if (granted) {
            lastDispatchFailureAtMs = 0L
            lastAutoPermissionRequestAtMs = 0L
            id.algaffar.jwstv.data.logging.AppLogger.info(
                "USB",
                "Izin USB OTG disetujui${if (!deviceName.isNullOrBlank()) " untuk $deviceName" else ""}"
            )
        } else {
            closeUsbSerial()
            id.algaffar.jwstv.data.logging.AppLogger.warn(
                "USB",
                "Izin USB OTG ditolak oleh pengguna${if (!deviceName.isNullOrBlank()) " ($deviceName)" else ""}"
            )
        }
    }

    /**
     * Closes the persistent USB Serial port if currently open.
     */
    fun closeUsbSerial() {
        synchronized(usbLock) {
            runCatching { openSerialPort?.close() }
            openSerialPort = null
            openDeviceName = null
            openBaudRate = -1
        }
    }

    private fun sendUsbSerialRelayCommand(
        pin: Int,
        turnOn: Boolean,
        baudRate: Int,
        preferredPort: String = ""
    ): Result<Unit> = runCatching {
        val appContext = context ?: throw IllegalStateException("Android Context required for USB OTG Serial")
        val usbManager = appContext.getSystemService(Context.USB_SERVICE) as? UsbManager
            ?: throw IllegalStateException("UsbManager unavailable")

        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            closeUsbSerial()
            throw IllegalStateException("Tidak ada modul USB Serial (CH340/CP210x/FTDI) yang terdeteksi")
        }

        val driver = selectUsbDriver(availableDrivers, preferredPort)
        if (!usbManager.hasPermission(driver.device)) {
            closeUsbSerial()
            val nowMs = System.currentTimeMillis()
            if (nowMs - lastAutoPermissionRequestAtMs >= AUTO_PERMISSION_COOLDOWN_MS) {
                lastAutoPermissionRequestAtMs = nowMs
                requestUsbRelayPermission(preferredPort)
            }
            throw SecurityException(
                "Izin USB OTG belum diberikan untuk ${driver.device.productName ?: driver.device.deviceName}. " +
                    "Setujui dialog izin di layar TV lalu coba lagi."
            )
        }

        val targetBaud = baudRate.coerceIn(1200, 115200)
        val deviceName = driver.device.deviceName
        val payload = formatSerialRelayPayload(pin, turnOn).toByteArray(Charsets.US_ASCII)

        synchronized(usbLock) {
            var port = openSerialPort
            val needsReopen = port == null ||
                !port.isOpen ||
                openDeviceName != deviceName ||
                openBaudRate != targetBaud

            if (needsReopen) {
                runCatching { port?.close() }
                openSerialPort = null
                openDeviceName = null
                openBaudRate = -1

                val connection = usbManager.openDevice(driver.device)
                    ?: throw SecurityException("Gagal membuka koneksi USB OTG untuk $deviceName")
                val newPort = driver.ports.firstOrNull()
                    ?: throw IllegalStateException("Port serial tidak ditemukan pada perangkat USB")

                newPort.open(connection)
                try {
                    newPort.setParameters(
                        targetBaud,
                        8,
                        UsbSerialPort.STOPBITS_1,
                        UsbSerialPort.PARITY_NONE
                    )
                    // Prevent DTR/RTS auto-reset pulse on Arduino/ESP boards when possible
                    runCatching { newPort.dtr = false }
                    runCatching { newPort.rts = false }
                } catch (t: Throwable) {
                    runCatching { newPort.close() }
                    throw t
                }

                openSerialPort = newPort
                openDeviceName = deviceName
                openBaudRate = targetBaud
                port = newPort
            }

            try {
                port!!.write(payload, 500)
            } catch (t: Throwable) {
                // Reset cached port on I/O failure so the next call reconnects cleanly
                runCatching { openSerialPort?.close() }
                openSerialPort = null
                openDeviceName = null
                openBaudRate = -1
                throw t
            }
        }
    }

    companion object {
        const val ACTION_USB_PERMISSION = "id.algaffar.jwstv.USB_RELAY_PERMISSION"
        private const val DISPATCH_RETRY_BACKOFF_MS = 5_000L
        private const val AUTO_PERMISSION_COOLDOWN_MS = 30_000L
        fun buildWifiRelayUrl(ip: String, pin: Int, turnOn: Boolean): String {
            val stateSegment = if (turnOn) "on" else "off"
            return "http://${ip.trim()}/relay/$pin/$stateSegment"
        }

        fun formatSerialRelayPayload(pin: Int, turnOn: Boolean): String {
            return "$pin:${if (turnOn) 1 else 0}\n"
        }

        /**
         * Validates that [ip] is a strictly numeric local/private IPv4 or IPv6 address
         * (prevents SSRF to public hosts, matching `validate_wifi_relay_ip` in `src-tauri/src/lib.rs`).
         */
        fun isPrivateOrLocalIp(ip: String): Boolean {
            val trimmed = ip.trim()
            if (trimmed.isEmpty()) return false
            // Guard against hostnames that would trigger DNS resolution
            val ipv4Regex = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")
            if (!ipv4Regex.matches(trimmed) && !trimmed.contains(':')) {
                return false
            }
            return try {
                val addr = InetAddress.getByName(trimmed)
                when (addr) {
                    is Inet4Address -> addr.isSiteLocalAddress || addr.isLoopbackAddress || addr.isLinkLocalAddress
                    is Inet6Address -> addr.isLoopbackAddress || addr.isSiteLocalAddress || addr.isLinkLocalAddress ||
                        (addr.address[0].toInt() and 0xfe) == 0xfc // Unique Local fc00::/7
                    else -> false
                }
            } catch (_: Exception) {
                false
            }
        }
    }
}

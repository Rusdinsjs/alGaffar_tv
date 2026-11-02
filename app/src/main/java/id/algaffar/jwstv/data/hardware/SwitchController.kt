package id.algaffar.jwstv.data.hardware

import android.content.Context
import android.hardware.usb.UsbManager
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
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(3, TimeUnit.SECONDS)
        .readTimeout(3, TimeUnit.SECONDS)
        .writeTimeout(3, TimeUnit.SECONDS)
        .build()
) {
    private val activePinStates = ConcurrentHashMap<Int, Boolean>()
    private val _pinStateFlow = MutableStateFlow<Map<Int, Boolean>>(emptyMap())
    val pinStateFlow: StateFlow<Map<Int, Boolean>> = _pinStateFlow.asStateFlow()

    private var lastObservedAppState: AppState? = null
    private var stateEnteredAtEpochSec: Long = 0L

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

        val result = linkedMapOf<Int, Boolean>()
        val nowEpochSec = now.toEpochSecond()
        val zoneId: ZoneId = now.zone

        for (channel in switchSettings.switches) {
            if (!channel.enabled || channel.pin !in 1..8) continue

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
        // Interpret onOffset: negative or positive both represent "minutes before prayer"
        // (e.g. -15 or 15 means 15 minutes before prayer time).
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

        for ((pin, shouldBeOn) in desired) {
            val previous = activePinStates[pin]
            if (previous != shouldBeOn) {
                activePinStates[pin] = shouldBeOn
                _pinStateFlow.value = activePinStates.toMap()
                sendHardwareCommand(
                    pin = pin,
                    turnOn = shouldBeOn,
                    settings = switchSettings
                )
            }
        }
    }

    /**
     * Manually toggles a specific relay pin (1..8) and dispatches the command to WiFi or USB hardware.
     */
    suspend fun setManualPinState(
        pin: Int,
        turnOn: Boolean,
        settings: SwitchSettings
    ): Result<Unit> {
        if (pin !in 1..8) {
            return Result.failure(IllegalArgumentException("Invalid relay pin $pin (must be 1..8)"))
        }
        activePinStates[pin] = turnOn
        _pinStateFlow.value = activePinStates.toMap()
        return sendHardwareCommand(pin = pin, turnOn = turnOn, settings = settings)
    }

    suspend fun sendHardwareCommand(
        pin: Int,
        turnOn: Boolean,
        settings: SwitchSettings
    ): Result<Unit> = withContext(ioDispatcher) {
        if (pin !in 1..8) {
            return@withContext Result.failure(IllegalArgumentException("Pin must be 1..8, got $pin"))
        }

        when (settings.connectionType.lowercase()) {
            "usb", "serial" -> sendUsbSerialRelayCommand(pin, turnOn, settings.baudRate)
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

    private fun sendUsbSerialRelayCommand(
        pin: Int,
        turnOn: Boolean,
        baudRate: Int
    ): Result<Unit> = runCatching {
        val appContext = context ?: throw IllegalStateException("Android Context required for USB OTG Serial")
        val usbManager = appContext.getSystemService(Context.USB_SERVICE) as? UsbManager
            ?: throw IllegalStateException("UsbManager unavailable")

        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            throw IllegalStateException("Tidak ada modul USB Serial (CH340/CP210x/FTDI) yang terdeteksi")
        }

        val driver = availableDrivers.first()
        val connection = usbManager.openDevice(driver.device)
            ?: throw SecurityException("Izin akses USB OTG belum diberikan untuk perangkat ${driver.device.deviceName}")

        val port = driver.ports.firstOrNull()
            ?: throw IllegalStateException("Port serial tidak ditemukan pada perangkat USB")

        port.open(connection)
        try {
            port.setParameters(
                baudRate.coerceIn(1200, 115200),
                8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE
            )
            val payload = formatSerialRelayPayload(pin, turnOn).toByteArray(Charsets.US_ASCII)
            port.write(payload, 500)
        } finally {
            runCatching { port.close() }
        }
    }

    companion object {
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

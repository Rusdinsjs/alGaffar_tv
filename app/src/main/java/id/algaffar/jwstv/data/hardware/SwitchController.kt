package id.algaffar.jwstv.data.hardware

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
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

        val isPlugAndPlayUsb = settings.isPlugAndPlayUsbRelay()
        when {
            isPlugAndPlayUsb || settings.connectionType.lowercase() in setOf("usb", "serial", "usb_hid") ->
                sendUsbRelayCommand(
                    pin = pin,
                    turnOn = turnOn,
                    baudRate = settings.baudRate,
                    preferredPort = settings.usbPort,
                    boardProfile = settings.boardProfile
                )
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
     * Locates an attached USB HID Programmable Relay module (such as the 2-Channel DCTTECH `USB-Relay-2`
     * VID 0x16C0 / PID 0x05DF, WCH HID Relay 0x1A86:0xE026/0x5537, or QinHeng/LCUS HID Relay 0x5131:0x2007).
     */
    private fun findUsbHidRelayDevice(
        usbManager: UsbManager,
        preferredPort: String = ""
    ): UsbDevice? {
        val allDevices = usbManager.deviceList?.values ?: return null
        val trimmed = preferredPort.trim()
        val candidates = allDevices.filter { isUsbHidRelayDevice(it) }
        if (candidates.isEmpty()) return null
        if (trimmed.isNotEmpty() && !trimmed.equals("/dev/ttyUSB0", ignoreCase = true)) {
            candidates.firstOrNull { it.deviceName.equals(trimmed, ignoreCase = true) }?.let { return it }
        }
        return candidates.first()
    }

    private fun isUsbHidRelayDevice(device: UsbDevice): Boolean {
        if (isKnownUsbHidRelayVidPid(device.vendorId, device.productId)) {
            return true
        }
        val prod = (device.productName ?: "").lowercase()
        val mfg = (device.manufacturerName ?: "").lowercase()
        if (prod.contains("usbrelay") || prod.contains("usb-relay") || prod.contains("usb relay") || mfg.contains("dcttech")) {
            return true
        }
        // Check if any interface is a custom non-boot USB HID interface with "relay" in product name
        for (i in 0 until device.interfaceCount) {
            val intf = device.getInterface(i)
            val isCustomHid = intf.interfaceClass == UsbConstants.USB_CLASS_HID &&
                intf.interfaceSubclass == 0 &&
                intf.interfaceProtocol == 0
            if (isCustomHid && prod.contains("relay")) {
                return true
            }
        }
        return false
    }

    /**
     * Checks if a supported USB Relay module — either USB HID (`USB-Relay-2` / DCTTECH / WCH / LCUS)
     * or USB Serial (`CH340 / CP210x / FTDI / PL2303`) — is attached, and requests Android USB Host permission.
     */
    fun requestUsbRelayPermission(preferredPort: String = ""): Result<String> = runCatching {
        val appContext = context ?: throw IllegalStateException("Android Context tidak tersedia")
        val usbManager = appContext.getSystemService(Context.USB_SERVICE) as? UsbManager
            ?: throw IllegalStateException("UsbManager tidak tersedia pada perangkat ini")

        val hidDevice = findUsbHidRelayDevice(usbManager, preferredPort)
        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)

        val device: UsbDevice = when {
            hidDevice != null -> hidDevice
            drivers.isNotEmpty() -> selectUsbDriver(drivers, preferredPort).device
            else -> throw IllegalStateException(
                "Tidak ada modul USB Relay (USB HID Relay 2-Channel / CH340 / CP210x / FTDI) yang terdeteksi di port USB TV/STB"
            )
        }

        val kindLabel = if (hidDevice != null) "USB HID Relay" else "USB Serial"
        val deviceLabel = "${device.productName ?: kindLabel} (${device.deviceName})"
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
        "Permintaan izin USB dikirim untuk $deviceLabel. Tekan OK pada dialog izin di layar TV."
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

    /**
     * Dispatches a relay ON/OFF command over USB, automatically supporting:
     * 1. Programmable USB HID Relay modules (`USB-Relay-2` DCTTECH VID 0x16C0 / PID 0x05DF, WCH/LCUS HID).
     * 2. Plug-and-Play USB Serial CH340 Relay modules (LCUS 4-byte hex `A0 pin state checksum`).
     * 3. ESP32 / Wemos / Arduino USB Serial modules (`{pin}:{1|0}\n`).
     */
    private fun sendUsbRelayCommand(
        pin: Int,
        turnOn: Boolean,
        baudRate: Int,
        preferredPort: String = "",
        boardProfile: String = "esp32_8"
    ): Result<Unit> = runCatching {
        val appContext = context ?: throw IllegalStateException("Android Context required for USB OTG")
        val usbManager = appContext.getSystemService(Context.USB_SERVICE) as? UsbManager
            ?: throw IllegalStateException("UsbManager unavailable")

        // 1. Prioritize USB HID Relay if attached (e.g. 2-Channel USB Programmable Relay `USB-Relay-2`)
        val hidDevice = findUsbHidRelayDevice(usbManager, preferredPort)
        if (hidDevice != null) {
            if (!usbManager.hasPermission(hidDevice)) {
                val nowMs = System.currentTimeMillis()
                if (nowMs - lastAutoPermissionRequestAtMs >= AUTO_PERMISSION_COOLDOWN_MS) {
                    lastAutoPermissionRequestAtMs = nowMs
                    requestUsbRelayPermission(preferredPort)
                }
                throw SecurityException(
                    "Izin USB belum diberikan untuk ${hidDevice.productName ?: hidDevice.deviceName}. " +
                        "Setujui dialog izin di layar TV lalu coba lagi."
                )
            }
            sendUsbHidRelayBytes(usbManager, hidDevice, pin, turnOn)
            return@runCatching
        }

        // 2. Otherwise dispatch via USB Serial (CH340 / CP210x / FTDI)
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        if (availableDrivers.isEmpty()) {
            closeUsbSerial()
            throw IllegalStateException(
                "Tidak ada modul USB Relay (USB HID 2-Channel / CH340 / CP210x / FTDI) yang terdeteksi"
            )
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
        val isPlugAndPlayProfile = boardProfile.equals("usb_relay_2", ignoreCase = true) ||
            boardProfile.equals("usb_relay_1", ignoreCase = true)

        if (isPlugAndPlayProfile) {
            // Send LCUS 4-byte binary hex frame first (for CH340 LCUS-1/LCUS-2 boards), followed by ASCII newline
            val lcusBytes = formatLcusRelayPayload(pin, turnOn)
            writeUsbSerialBytes(usbManager, driver, targetBaud, lcusBytes)
        } else {
            val payload = formatSerialRelayPayload(pin, turnOn).toByteArray(Charsets.US_ASCII)
            writeUsbSerialBytes(usbManager, driver, targetBaud, payload)
        }
    }

    /**
     * Sends the USB HID command to a `USB-Relay-2` / DCTTECH / WCH / LCUS HID Relay board.
     * - DCTTECH `USB-Relay-1/2/4/8` (`usb_relay_device.dll`): HID Class `SET_REPORT` (0x09),
     *   Report Type = Feature (0x0300), 8-byte payload `[0xFF (ON) / 0xFD (OFF), pin, 0, 0, 0, 0, 0, 0]`.
     * - WCH / QinHeng HID Relay fallback: Interrupt OUT / Output Report with LCUS 4-byte frame `[0xA0, pin, state, checksum]`.
     */
    private fun sendUsbHidRelayBytes(
        usbManager: UsbManager,
        device: UsbDevice,
        pin: Int,
        turnOn: Boolean
    ) {
        synchronized(usbLock) {
            val connection = usbManager.openDevice(device)
                ?: throw SecurityException("Gagal membuka koneksi USB HID untuk ${device.deviceName}")

            var hidInterface: UsbInterface? = null
            for (i in 0 until device.interfaceCount) {
                val intf = device.getInterface(i)
                if (intf.interfaceClass == UsbConstants.USB_CLASS_HID) {
                    hidInterface = intf
                    break
                }
            }
            if (hidInterface == null && device.interfaceCount > 0) {
                hidInterface = device.getInterface(0)
            }

            try {
                if (hidInterface != null) {
                    connection.claimInterface(hidInterface, true)
                }
                val intfId = hidInterface?.id ?: 0
                val dctPayload = formatUsbHidRelayPayload(pin, turnOn)

                // 1. Standard DCTTECH USB-Relay-2 Feature Report (SET_REPORT = 0x09, Feature = 0x0300)
                val featureTransferred = connection.controlTransfer(
                    USB_HID_REQ_TYPE_CLASS_INTERFACE_OUT,
                    USB_HID_SET_REPORT,
                    USB_HID_REPORT_TYPE_FEATURE_ID0,
                    intfId,
                    dctPayload,
                    dctPayload.size,
                    500
                )

                // 2. Fallback for WCH / QinHeng HID variants using Output Report or Interrupt OUT endpoint
                if (featureTransferred < 0 || device.vendorId == VID_QINHENG_HID || device.vendorId == VID_WCH) {
                    val lcusPayload = formatLcusRelayPayload(pin, turnOn)
                    val outTransferred = connection.controlTransfer(
                        USB_HID_REQ_TYPE_CLASS_INTERFACE_OUT,
                        USB_HID_SET_REPORT,
                        USB_HID_REPORT_TYPE_OUTPUT_ID0,
                        intfId,
                        dctPayload,
                        dctPayload.size,
                        500
                    )
                    var epTransferred = -1
                    if (hidInterface != null) {
                        for (e in 0 until hidInterface.endpointCount) {
                            val ep = hidInterface.getEndpoint(e)
                            if (ep.direction == UsbConstants.USB_DIR_OUT) {
                                val buf = if (device.vendorId == VID_QINHENG_HID || device.vendorId == VID_WCH) {
                                    lcusPayload
                                } else {
                                    dctPayload
                                }
                                epTransferred = connection.bulkTransfer(ep, buf, buf.size, 500)
                                break
                            }
                        }
                    }
                    if (featureTransferred < 0 && outTransferred < 0 && epTransferred < 0) {
                        throw IllegalStateException("Gagal mengirim paket USB HID Relay ke ${device.deviceName}")
                    }
                }
            } finally {
                if (hidInterface != null) {
                    runCatching { connection.releaseInterface(hidInterface) }
                }
                runCatching { connection.close() }
            }
        }
    }

    private fun writeUsbSerialBytes(
        usbManager: UsbManager,
        driver: com.hoho.android.usbserial.driver.UsbSerialDriver,
        targetBaud: Int,
        payload: ByteArray
    ) {
        val deviceName = driver.device.deviceName
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

    /**
     * Dynamically provisions the ESP32 / Wemos D1 Mini WiFi mode & credentials without reflashing Arduino code.
     * Automatically sends the configuration via USB OTG Serial if attached, or via HTTP `/api/wifi-config` to [settings.wifiIp].
     */
    suspend fun provisionRelayWifiConfig(
        settings: SwitchSettings
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val isOfflineAp = settings.wifiMode.equals("offline_ap", ignoreCase = true) ||
                settings.wifiMode.equals("offline", ignoreCase = true)
            val ssid = settings.targetWifiSsid.trim()
            val pass = settings.targetWifiPassword
            val octet = settings.staticIpLastOctet.coerceIn(0, 254)

            if (!isOfflineAp && ssid.isEmpty()) {
                throw IllegalArgumentException("Nama WiFi Masjid (SSID) wajib diisi untuk Mode Online.")
            }

            // 1. Try USB OTG Serial first if an adapter is plugged in and has permission
            val appContext = context
            val usbManager = appContext?.getSystemService(Context.USB_SERVICE) as? UsbManager
            val availableDrivers = if (usbManager != null) {
                UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
            } else {
                emptyList()
            }

            if (usbManager != null && availableDrivers.isNotEmpty()) {
                val driver = selectUsbDriver(availableDrivers, settings.usbPort)
                if (usbManager.hasPermission(driver.device)) {
                    val serialCmd = formatSerialWifiProvisionPayload(
                        onlineMode = !isOfflineAp,
                        ssid = ssid,
                        password = pass,
                        staticOctet = octet
                    )
                    writeUsbSerialBytes(
                        usbManager = usbManager,
                        driver = driver,
                        targetBaud = settings.baudRate.coerceIn(1200, 115200),
                        payload = serialCmd.toByteArray(Charsets.UTF_8)
                    )
                    return@runCatching if (isOfflineAp) {
                        "Konfigurasi Mode OFFLINE (Hotspot AP 192.168.4.1) berhasil dikirim via USB Serial!"
                    } else {
                        "Konfigurasi WiFi Online ('$ssid', akhiran IP .$octet) berhasil dikirim via USB Serial!"
                    }
                }
            }

            // 2. Otherwise send via HTTP GET/POST to the current Relay IP (or 192.168.4.1 fallback)
            val targetIp = settings.wifiIp.trim().ifEmpty { "192.168.4.1" }
            require(isPrivateOrLocalIp(targetIp)) {
                "IP Relay harus merupakan IP lokal/privat (misal 192.168.4.1 atau 192.168.1.250). Diterima: $targetIp"
            }

            val url = buildWifiProvisionUrl(
                ip = targetIp,
                onlineMode = !isOfflineAp,
                ssid = ssid,
                password = pass,
                staticOctet = octet
            )
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Relay HTTP ${response.code} saat menyimpan konfigurasi WiFi")
                }
            }

            if (isOfflineAp) {
                "Relay berhasil diubah ke Mode OFFLINE (Hotspot AP 192.168.4.1) via WiFi!"
            } else {
                "Konfigurasi WiFi Masjid ('$ssid', akhiran IP .$octet) berhasil disimpan ke Relay!"
            }
        }
    }

    companion object {
        const val ACTION_USB_PERMISSION = "id.algaffar.jwstv.USB_RELAY_PERMISSION"
        private const val DISPATCH_RETRY_BACKOFF_MS = 5_000L
        private const val AUTO_PERMISSION_COOLDOWN_MS = 30_000L

        // USB HID Control Transfer constants (DCTTECH USB-Relay-1/2/4/8 & WCH/QinHeng HID Relay)
        private const val USB_HID_REQ_TYPE_CLASS_INTERFACE_OUT = 0x21
        private const val USB_HID_SET_REPORT = 0x09
        private const val USB_HID_REPORT_TYPE_FEATURE_ID0 = 0x0300
        private const val USB_HID_REPORT_TYPE_OUTPUT_ID0 = 0x0200

        const val VID_DCTTECH_VUSB = 0x16C0 // 5824
        const val PID_DCTTECH_HID_RELAY = 0x05DF // 1503
        const val VID_WCH = 0x1A86 // 6790
        const val PID_WCH_HID_RELAY_1 = 0xE026 // 57382
        const val PID_WCH_HID_RELAY_2 = 0x5537 // 21815
        const val VID_QINHENG_HID = 0x5131 // 20785
        const val PID_QINHENG_HID_RELAY = 0x2007 // 8207

        fun isKnownUsbHidRelayVidPid(vendorId: Int, productId: Int): Boolean {
            return (vendorId == VID_DCTTECH_VUSB && productId == PID_DCTTECH_HID_RELAY) ||
                (vendorId == VID_WCH && (productId == PID_WCH_HID_RELAY_1 || productId == PID_WCH_HID_RELAY_2)) ||
                (vendorId == VID_QINHENG_HID && productId == PID_QINHENG_HID_RELAY)
        }

        fun buildWifiRelayUrl(ip: String, pin: Int, turnOn: Boolean): String {
            val stateSegment = if (turnOn) "on" else "off"
            return "http://${ip.trim()}/relay/$pin/$stateSegment"
        }

        fun buildWifiProvisionUrl(
            ip: String,
            onlineMode: Boolean,
            ssid: String,
            password: String,
            staticOctet: Int = 250
        ): String {
            val modeVal = if (onlineMode) "online" else "offline"
            val encSsid = java.net.URLEncoder.encode(ssid.trim(), "UTF-8")
            val encPass = java.net.URLEncoder.encode(password, "UTF-8")
            val cleanOctet = staticOctet.coerceIn(0, 254)
            return "http://${ip.trim()}/api/wifi-config?json=1&mode=$modeVal&ssid=$encSsid&pass=$encPass&octet=$cleanOctet"
        }

        fun formatSerialRelayPayload(pin: Int, turnOn: Boolean): String {
            return "$pin:${if (turnOn) 1 else 0}\n"
        }

        /**
         * Formats the 8-byte USB HID Feature Report for DCTTECH `USB-Relay-1/2/4/8` (`usb_relay_device.dll`):
         * - Byte 0: `0xFF` (ON single relay) or `0xFD` (OFF single relay)
         * - Byte 1: Relay channel index (`1..8`)
         * - Bytes 2..7: `0x00`
         */
        fun formatUsbHidRelayPayload(pin: Int, turnOn: Boolean): ByteArray {
            val cmdByte = if (turnOn) 0xFF.toByte() else 0xFD.toByte()
            return byteArrayOf(
                cmdByte,
                pin.coerceIn(1, 8).toByte(),
                0x00,
                0x00,
                0x00,
                0x00,
                0x00,
                0x00
            )
        }

        /**
         * Formats the 4-byte binary hex frame for LCUS / WCH Plug-and-Play USB Relay modules:
         * `[0xA0, pin, state (0x01=ON / 0x00=OFF), checksum ((0xA0 + pin + state) & 0xFF)]`.
         */
        fun formatLcusRelayPayload(pin: Int, turnOn: Boolean): ByteArray {
            val cleanPin = pin.coerceIn(1, 8)
            val stateByte = if (turnOn) 1 else 0
            val checksum = (0xA0 + cleanPin + stateByte) and 0xFF
            return byteArrayOf(
                0xA0.toByte(),
                cleanPin.toByte(),
                stateByte.toByte(),
                checksum.toByte()
            )
        }

        fun formatSerialWifiProvisionPayload(
            onlineMode: Boolean,
            ssid: String,
            password: String,
            staticOctet: Int = 250
        ): String {
            return if (!onlineMode) {
                "WIFI:OFFLINE\n"
            } else {
                val cleanSsid = ssid.trim().replace(":", "")
                val cleanOctet = staticOctet.coerceIn(0, 254)
                "WIFI:ONLINE:$cleanSsid:$password:$cleanOctet\n"
            }
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

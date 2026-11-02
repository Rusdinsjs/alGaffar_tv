package id.algaffar.jwstv.data

import id.algaffar.jwstv.data.auth.AuthManager
import id.algaffar.jwstv.data.cctv.OnlineFinanceManager
import id.algaffar.jwstv.data.cctv.OnvifDiscoveryManager
import id.algaffar.jwstv.data.hardware.SwitchController
import id.algaffar.jwstv.data.server.IpRateLimiter
import id.algaffar.jwstv.data.server.KtorRemoteServer
import id.algaffar.jwstv.domain.engine.PrayerTimeCalculator
import id.algaffar.jwstv.domain.model.AppState
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.PrayerId
import id.algaffar.jwstv.domain.model.PrayerSettings
import id.algaffar.jwstv.domain.model.SwitchChannelConfig
import id.algaffar.jwstv.domain.model.SwitchSettings
import id.algaffar.jwstv.domain.model.UserAccount
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class Phase5ServerAndHardwareTest {

    @Test
    fun `SwitchController validates private IPs and formats WiFi and USB payloads`() {
        assertTrue(SwitchController.isPrivateOrLocalIp("192.168.1.100"))
        assertTrue(SwitchController.isPrivateOrLocalIp("10.0.0.25"))
        assertTrue(SwitchController.isPrivateOrLocalIp("172.16.5.10"))
        assertTrue(SwitchController.isPrivateOrLocalIp("127.0.0.1"))

        assertFalse(SwitchController.isPrivateOrLocalIp("8.8.8.8"))
        assertFalse(SwitchController.isPrivateOrLocalIp("1.1.1.1"))
        assertFalse(SwitchController.isPrivateOrLocalIp("evil.example.com"))
        assertFalse(SwitchController.isPrivateOrLocalIp(""))

        assertEquals(
            "http://192.168.1.100/relay/3/on",
            SwitchController.buildWifiRelayUrl("192.168.1.100", 3, true)
        )
        assertEquals(
            "http://192.168.1.100/relay/6/off",
            SwitchController.buildWifiRelayUrl("192.168.1.100", 6, false)
        )
        assertEquals("1:1\n", SwitchController.formatSerialRelayPayload(1, true))
        assertEquals("8:0\n", SwitchController.formatSerialRelayPayload(8, false))
    }

    @Test
    fun `SwitchController evaluates prayer and state triggers accurately`() {
        val calc = PrayerTimeCalculator()
        val zone = ZoneId.of("Asia/Makassar")
        val baseTime = ZonedDateTime.of(2026, 3, 20, 12, 0, 0, 0, zone)
        val schedule = calc.calculateSchedule(baseTime, PrayerSettings())

        // Dhuhr time on 2026-03-20 in Balikpapan
        val dhuhrZoned = schedule.timeFor(PrayerId.DHUHR)
        val controller = SwitchController()

        val switchSettings = SwitchSettings(
            enabled = true,
            switches = listOf(
                // Pin 1: Prayer trigger (-15m before Dhuhr to +30m after Dhuhr)
                SwitchChannelConfig(
                    id = "lampu_dalam",
                    name = "Lampu Dalam",
                    pin = 1,
                    enabled = true,
                    triggerMode = "prayer",
                    onOffset = -15,
                    offOffset = 30,
                    activePrayers = listOf("dhuhr")
                ),
                // Pin 6: State trigger (ON at tartil, OFF at pelaksanaan_sholat)
                SwitchChannelConfig(
                    id = "audio_luar",
                    name = "Audio Luar (Toa)",
                    pin = 6,
                    enabled = true,
                    triggerMode = "state",
                    onState = "tartil",
                    offState = "pelaksanaan_sholat"
                )
            )
        )

        // 10 minutes before Dhuhr during TARTIL -> Pin 1 ON, Pin 6 ON
        val tenMinBeforeDhuhr = dhuhrZoned.minusMinutes(10)
        val statesDuringTartil = controller.computeDesiredPinStates(
            now = tenMinBeforeDhuhr,
            currentState = AppState.TARTIL,
            stateElapsedSeconds = 60L,
            switchSettings = switchSettings,
            schedule = schedule
        )
        assertEquals(true, statesDuringTartil[1])
        assertEquals(true, statesDuringTartil[6])

        // 5 minutes after Dhuhr during PELAKSANAAN_SHOLAT -> Pin 1 still ON, Pin 6 OFF
        val fiveMinAfterDhuhr = dhuhrZoned.plusMinutes(5)
        val statesDuringSholat = controller.computeDesiredPinStates(
            now = fiveMinAfterDhuhr,
            currentState = AppState.PELAKSANAAN_SHOLAT,
            stateElapsedSeconds = 30L,
            switchSettings = switchSettings,
            schedule = schedule
        )
        assertEquals(true, statesDuringSholat[1])
        assertEquals(false, statesDuringSholat[6])

        // 45 minutes after Dhuhr during STANDBY -> Pin 1 OFF
        val fortyFiveMinAfterDhuhr = dhuhrZoned.plusMinutes(45)
        val statesAfterWindow = controller.computeDesiredPinStates(
            now = fortyFiveMinAfterDhuhr,
            currentState = AppState.STANDBY,
            stateElapsedSeconds = 120L,
            switchSettings = switchSettings,
            schedule = schedule
        )
        assertEquals(false, statesAfterWindow[1])
    }

    @Test
    fun `KtorRemoteServer validates Bearer tokens, enforces rate limits, and builds status JSON`() {
        val settings = AppStateSettings(
            users = listOf(
                UserAccount(
                    id = "adm-1",
                    username = "dkm",
                    pinHash = AuthManager.sha256Hex("654321"),
                    role = "admin"
                )
            )
        )

        // 1. Server generated token
        assertTrue(KtorRemoteServer.isAuthorizedHeader("Bearer SECRET99", "SECRET99", settings))
        // 2. Default superadmin PIN
        assertTrue(KtorRemoteServer.isAuthorizedHeader("Bearer 732911", "SECRET99", settings))
        // 3. Configured admin PIN
        assertTrue(KtorRemoteServer.isAuthorizedHeader("Bearer 654321", "SECRET99", settings))
        // 4. Reject invalid token
        assertFalse(KtorRemoteServer.isAuthorizedHeader("Bearer 000000", "SECRET99", settings))
        assertFalse(KtorRemoteServer.isAuthorizedHeader(null, "SECRET99", settings))

        // Rate limiter test (max 3 requests per 60s window)
        val limiter = IpRateLimiter(maxRequests = 3, windowMillis = 60_000L)
        assertTrue(limiter.isAllowed("192.168.1.50", 1_000L))
        assertTrue(limiter.isAllowed("192.168.1.50", 2_000L))
        assertTrue(limiter.isAllowed("192.168.1.50", 3_000L))
        assertFalse(limiter.isAllowed("192.168.1.50", 4_000L))
        // Window rolls over after 60s
        assertTrue(limiter.isAllowed("192.168.1.50", 62_000L))

        // Status JSON payload contract
        val server = KtorRemoteServer(
            authToken = "TOKEN123",
            settingsProvider = { settings },
            runtimeStatusProvider = {
                KtorRemoteServer.RuntimeServerSnapshot(
                    currentState = AppState.TARHIM,
                    isAudioPlaying = true,
                    isCustomAudioPlaying = false
                )
            }
        )
        val statusJson = server.buildStatusJson()
        assertEquals("tarhim", statusJson["state"]?.jsonPrimitive?.contentOrNull)
        assertEquals(true, statusJson["audio"]?.jsonPrimitive?.booleanOrNull)
        assertNotNull(statusJson["prayerSettings"]?.jsonObject)
        assertNotNull(statusJson["system"]?.jsonObject)
    }

    @Test
    fun `OnlineFinanceManager normalizes Google Sheets URLs and parses IDR CSV`() {
        val pubHtmlUrl = "https://docs.google.com/spreadsheets/d/e/2PACX-1vTest/pubhtml"
        assertEquals(
            "https://docs.google.com/spreadsheets/d/e/2PACX-1vTest/pub?output=csv",
            OnlineFinanceManager.normalizeGoogleSheetsUrl(pubHtmlUrl)
        )

        val editUrl = "https://docs.google.com/spreadsheets/d/1AbCdEfGhIjKlMnOp/edit?usp=sharing"
        assertEquals(
            "https://docs.google.com/spreadsheets/d/1AbCdEfGhIjKlMnOp/export?format=csv",
            OnlineFinanceManager.normalizeGoogleSheetsUrl(editUrl)
        )

        val sampleCsv = """
            Tanggal,Keterangan,Masuk,Keluar
            2026-03-01,"Infaq Kotak Jumat, Tromol Utama",Rp 4.500.000,0
            2026-03-02,Tagihan Listrik PLN & Air PDAM,0,Rp 1.250.000
            2026-03-03,Infaq QRIS Jamaah,750.000,0
        """.trimIndent()

        val parsed = OnlineFinanceManager.parseFinanceCsv(sampleCsv, "2026-03-03")
        assertEquals(5_250_000L, parsed.income)
        assertEquals(1_250_000L, parsed.expense)
        assertEquals(4_000_000L, parsed.balance)
        assertEquals(3, parsed.recentTransactions.size)
        assertEquals("Infaq QRIS Jamaah", parsed.recentTransactions.first().description)
    }

    @Test
    fun `OnvifDiscoveryManager parses WS-Discovery SOAP ProbeMatches XML`() {
        val sampleProbeMatchXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://www.w3.org/2003/05/soap-envelope"
                               xmlns:d="http://schemas.xmlsoap.org/ws/2005/04/discovery">
              <SOAP-ENV:Body>
                <d:ProbeMatches>
                  <d:ProbeMatch>
                    <d:Scopes>onvif://www.onvif.org/type/video_encoder onvif://www.onvif.org/name/Hikvision%20Shaf%20Utama</d:Scopes>
                    <d:XAddrs>http://192.168.1.64/onvif/device_service</d:XAddrs>
                  </d:ProbeMatch>
                </d:ProbeMatches>
              </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>
        """.trimIndent()

        val camera = OnvifDiscoveryManager.parseWsDiscoveryResponse(sampleProbeMatchXml, "192.168.1.64")
        assertNotNull(camera)
        assertEquals("Hikvision Shaf Utama", camera?.name)
        assertEquals("http://192.168.1.64/onvif/device_service", camera?.url)
        assertEquals("192.168.1.64", camera?.address)
    }
}

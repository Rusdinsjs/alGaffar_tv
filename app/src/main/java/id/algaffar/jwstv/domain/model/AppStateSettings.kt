package id.algaffar.jwstv.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Complete configuration schema of JWS TV Masjid Al-Gaffar.
 * Designed for 1:1 JSON compatibility with the legacy Tauri/React `AppStateSettings`
 * (`src/types/appState.ts`, `prayerSettings.ts`, `switchSettings.ts`, `layout.ts`, `themes.ts`).
 */

@Serializable
data class PrayerCorrections(
    val imsak: Int = 0,
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuha: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0
) {
    fun forPrayer(prayerId: PrayerId): Int = when (prayerId) {
        PrayerId.IMSAK -> imsak
        PrayerId.FAJR -> fajr
        PrayerId.SUNRISE -> sunrise
        PrayerId.DHUHA -> dhuha
        PrayerId.DHUHR -> dhuhr
        PrayerId.ASR -> asr
        PrayerId.MAGHRIB -> maghrib
        PrayerId.ISHA -> isha
    }
}

@Serializable
data class PrayerSettings(
    val latitude: Double = -1.2379,
    val longitude: Double = 116.8529,
    val timezone: String = "Asia/Makassar",
    val calculationMethod: String = "Singapore",
    val madhab: String = "Shafi",
    val corrections: PrayerCorrections = PrayerCorrections(),
    val ihtiyati: Int = 2,
    val hijriAdjustment: Int = 0,
    val prayerTimeScrollInterval: Int = 3
)

@Serializable
data class BrandSettings(
    val name: String = "MASJID AL-GAFFAR",
    val address: String = "Jl. Perjuangan, Balikpapan, Kalimantan Timur",
    val phone: String = "0812-3456-7890",
    val logo: String = "",
    val titleFontSize: Int = 36,
    val addressFontSize: Int = 18,
    val clockStyle: String = "classic",
    val fullscreenClockStyle: String = "golden",
    val showHijriDate: Boolean = true,
    val showMasehiDate: Boolean = true
)

@Serializable
data class PrayerStageConfig(
    val tartilDuration: Int = 10,     // minutes before Adzan
    val tarhimDuration: Int = 5,      // minutes before Adzan (auto-synced with audio length if available)
    val adzanDuration: Int = 4,       // minutes (auto-synced with audio length if isAdzan == true)
    val iqomahDuration: Int = 10,     // minutes countdown before sholat
    val sholatDuration: Int = 15,     // minutes quiet sholat screen
    val isAdzan: Boolean = true,      // play adzan audio or visual-only
    val imam: String = "",
    val muadzin: String = ""
)

@Serializable
data class FivePrayerConfigs(
    val fajr: PrayerStageConfig = PrayerStageConfig(
        tartilDuration = 15,
        tarhimDuration = 5,
        adzanDuration = 4,
        iqomahDuration = 12,
        sholatDuration = 15
    ),
    val dhuhr: PrayerStageConfig = PrayerStageConfig(
        tartilDuration = 10,
        tarhimDuration = 5,
        adzanDuration = 4,
        iqomahDuration = 10,
        sholatDuration = 15
    ),
    val asr: PrayerStageConfig = PrayerStageConfig(
        tartilDuration = 10,
        tarhimDuration = 5,
        adzanDuration = 4,
        iqomahDuration = 10,
        sholatDuration = 15
    ),
    val maghrib: PrayerStageConfig = PrayerStageConfig(
        tartilDuration = 10,
        tarhimDuration = 5,
        adzanDuration = 4,
        iqomahDuration = 7,
        sholatDuration = 15
    ),
    val isha: PrayerStageConfig = PrayerStageConfig(
        tartilDuration = 10,
        tarhimDuration = 5,
        adzanDuration = 4,
        iqomahDuration = 10,
        sholatDuration = 15
    )
) {
    fun forPrayer(prayerId: PrayerId): PrayerStageConfig = when (prayerId) {
        PrayerId.FAJR -> fajr
        PrayerId.DHUHR -> dhuhr
        PrayerId.ASR -> asr
        PrayerId.MAGHRIB -> maghrib
        PrayerId.ISHA -> isha
        else -> dhuhr
    }
}

// ==================== JUM'AT SETTINGS ====================

@Serializable
data class JumatPetugasRecord(
    val khotib: String = "-",
    val imam: String = "-",
    val muadzin: String = "-"
)

@Serializable
data class JumatPetugasSettings(
    val selectedDate: String = "",
    val records: Map<String, JumatPetugasRecord> = emptyMap()
)

@Serializable
data class JumatJournalEntry(
    val id: String = "",
    val description: String = "",
    val amount: Long = 0L,
    val type: String = "in" // "in" | "out"
)

@Serializable
data class JumatKeuanganRecord(
    val saldoKasLalu: Long = 0L,
    val pemasukan: Long = 0L,
    val pengeluaran: Long = 0L,
    val entries: List<JumatJournalEntry> = emptyList()
) {
    val saldoAkhir: Long
        get() = saldoKasLalu + pemasukan - pengeluaran
}

@Serializable
data class JumatKeuanganSettings(
    val selectedDate: String = "",
    val saldoKasLalu: Long = 0L,
    val records: Map<String, JumatKeuanganRecord> = emptyMap()
)

@Serializable
data class JumatSlideshowSettings(
    val showPetugas: Boolean = true,
    val showKeuangan: Boolean = true,
    val slideDuration: Int = 10 // seconds per slide
)

@Serializable
data class JumatSettings(
    val enabled: Boolean = true,
    val waktuJumatDuration: Int = 12,     // minutes before khutbah
    val khutbahDuration: Int = 30,        // minutes
    val sholatJumatDuration: Int = 20,    // minutes
    val petugas: JumatPetugasSettings = JumatPetugasSettings(),
    val keuangan: JumatKeuanganSettings = JumatKeuanganSettings(),
    val slideshow: JumatSlideshowSettings = JumatSlideshowSettings()
)

// ==================== RAMADHAN SETTINGS ====================

@Serializable
data class RamadhanPetugasItem(
    val malamKe: Int = 1,
    val penceramah: String = "-",
    val imamTarawih: String = "-",
    val imamWitir: String = "-"
)

@Serializable
data class RamadhanKeuanganItem(
    val malamKe: Int = 1,
    val saldoAwal: Long = 0L,
    val penerimaanPria: Long = 0L,
    val penerimaanWanita: Long = 0L,
    val pengeluaran: Long = 0L
) {
    val totalPenerimaan: Long
        get() = penerimaanPria + penerimaanWanita

    val saldoAkhir: Long
        get() = saldoAwal + totalPenerimaan - pengeluaran
}

@Serializable
data class RamadhanLaporanAkhir(
    val enabled: Boolean = false,
    val totalPenerimaan: Long = 0L,
    val totalPengeluaran: Long = 0L,
    val saldoAkhir: Long = 0L,
    val catatan: String = ""
)

@Serializable
data class RamadhanSlideshowConfig(
    val showPetugas: Boolean = true,
    val showKeuangan: Boolean = true,
    val showJadwal: Boolean = true,
    val showTakjil: Boolean = true,
    val slideDuration: Int = 10
)

@Serializable
data class SirineSettings(
    val enabled: Boolean = false,
    val audioFile: String = "",
    val repeatCount: Int = 1,
    val playAtMaghrib: Boolean = true,
    val playAtSahur: Boolean = false,
    val sahurTime: String = "03:00",
    val playAtImsak: Boolean = true
)

@Serializable
data class RamadhanSettings(
    val isRamadhan: Boolean = false,
    val currentNight: Int = 1,
    val tarawihDuration: Int = 15,        // minutes for kultum/info before sholat tarawih
    val sholatTarawihDuration: Int = 45,  // minutes
    val petugas: List<RamadhanPetugasItem> = (1..30).map { RamadhanPetugasItem(malamKe = it) },
    val keuangan: List<RamadhanKeuanganItem> = (1..30).map { RamadhanKeuanganItem(malamKe = it) },
    val takjil: Map<String, List<String>> = emptyMap(), // key: "1".."30", value: list of donor names
    val laporanAkhir: RamadhanLaporanAkhir = RamadhanLaporanAkhir(),
    val slideshow: RamadhanSlideshowConfig = RamadhanSlideshowConfig(),
    val sirine: SirineSettings = SirineSettings()
)

// ==================== HOLIDAY & QURBAN SETTINGS ====================

@Serializable
data class TimeSlot(
    val id: String = "",
    val startTime: String = "06:00", // HH:mm
    val endTime: String = "07:00",   // HH:mm
    val enabled: Boolean = true
)

@Serializable
data class EidPetugas(
    val khotib: String = "-",
    val imam: String = "-",
    val bilal: String = "-"
)

@Serializable
data class EidKeuangan(
    val saldoAwal: Long = 0L,
    val pemasukan: Long = 0L,
    val pengeluaran: Long = 0L
) {
    val saldoAkhir: Long
        get() = saldoAwal + pemasukan - pengeluaran
}

@Serializable
data class IdulFitriSettings(
    val enabled: Boolean = false,
    val timeSlots: List<TimeSlot> = emptyList(),
    val fullscreenDuration: Int = 30, // seconds in idul_fitri before alternating to idul_fitri_standby
    val standbyDuration: Int = 30,    // seconds in idul_fitri_standby
    val sholatDuration: Int = 45,     // minutes
    val khutbahDuration: Int = 30,    // minutes
    val petugas: EidPetugas = EidPetugas(),
    val keuangan: EidKeuangan = EidKeuangan(),
    val slideDuration: Int = 10
)

@Serializable
data class QurbanGroup(
    val id: String = "",
    val animalType: String = "Sapi", // "Sapi" | "Kambing"
    val groupName: String = "Kelompok 1",
    val members: List<String> = emptyList()
)

@Serializable
data class IdulAdhaSettings(
    val enabled: Boolean = false,
    val timeSlots: List<TimeSlot> = emptyList(),
    val fullscreenDuration: Int = 30,
    val standbyDuration: Int = 30,
    val sholatDuration: Int = 45,
    val khutbahDuration: Int = 30,
    val petugas: EidPetugas = EidPetugas(),
    val qurbanGroups: List<QurbanGroup> = emptyList(),
    val slideDuration: Int = 10
)

@Serializable
data class InfoQurbanSettings(
    val enabled: Boolean = false,
    val timeSlots: List<TimeSlot> = emptyList(),
    val fullscreenDuration: Int = 30,
    val standbyDuration: Int = 30,
    val slideDuration: Int = 10
)

// ==================== SLIDES, STANDBY SLIDESHOW & SCREENSAVER ====================

@Serializable
data class SlideItem(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val type: String = "image", // "image" | "video" | "text"
    val imageUrl: String = "",
    val videoUrl: String = "",
    val duration: Int = 10, // seconds
    val enabled: Boolean = true,
    val showInMain: Boolean = true,
    val showInSlideshow: Boolean = true,
    val showInJumat: Boolean = false,
    val showInRamadhan: Boolean = false,
    val showInIdulFitri: Boolean = false,
    val showInIdulAdha: Boolean = false
)

@Serializable
data class StandbySlideshowSettings(
    val enabled: Boolean = false,
    val durationMain: Int = 60, // seconds staying in `standby` before entering `standby_slideshow`
    val repeatCount: Int = 1    // number of full slide cycles in `standby_slideshow` before returning
)

@Serializable
data class ScreenSaverSchedule(
    val id: String = "default",
    val startTime: String = "22:00",
    val endTime: String = "03:00",
    val enabled: Boolean = true
)

@Serializable
data class ScreenSaverSettings(
    val enabled: Boolean = false,
    val schedules: List<ScreenSaverSchedule> = listOf(ScreenSaverSchedule()),
    val message: String = "Mode Hemat Daya — Masjid Al-Gaffar"
)

// ==================== CCTV, LIVE, QR, ONLINE FINANCE, CUSTOM AUDIO ====================

@Serializable
data class CctvCameraConfig(
    val id: String = "",
    val name: String = "Kamera 1",
    val type: String = "ip", // "ip" | "usb"
    val url: String = "",    // RTSP / MJPEG / HTTP snapshot URL
    val deviceId: String = "",
    val enabled: Boolean = true
)

@Serializable
data class CctvScheduleTrigger(
    val id: String = "",
    val prayerId: String = "fajr",
    val referencePoint: String = "tartil", // "tartil" | "sholat"
    val startOffsetMinutes: Int = -5,
    val durationMinutes: Int = 10,
    val enabled: Boolean = false
)

@Serializable
data class CctvSettings(
    val enabled: Boolean = false,
    val gridLayout: Int = 4, // 1 | 2 | 4 | 8
    val showInMainSlide: Boolean = false,
    val showDuringSholat: Boolean = false,
    val resolutionMode: String = "medium", // "low" | "medium" | "high"
    val cameras: List<CctvCameraConfig> = emptyList(),
    val schedules: List<CctvScheduleTrigger> = emptyList()
)

@Serializable
data class LiveScheduleTrigger(
    val id: String = "",
    val prayerId: String = "maghrib",
    val referencePoint: String = "tartil",
    val startOffsetMinutes: Int = -15,
    val durationMinutes: Int = 10,
    val enabled: Boolean = false
)

@Serializable
data class LiveSettings(
    val enabled: Boolean = false,
    val source: String = "makkah", // "makkah" | "madinah" | "custom"
    val customVideoId: String = "",
    val makkahVideoId: String = "Cm1v4bteXbI",
    val madinahVideoId: String = "9A1S0xAPVIs",
    val muted: Boolean = true,
    val showInMainSlide: Boolean = false,
    val schedules: List<LiveScheduleTrigger> = emptyList()
) {
    val activeYoutubeId: String
        get() = when (source) {
            "makkah" -> makkahVideoId
            "madinah" -> madinahVideoId
            else -> customVideoId.ifBlank { makkahVideoId }
        }
}

@Serializable
data class MasjidQrSettings(
    val enabled: Boolean = false,
    val label: String = "Infaq & Sedekah Digital Masjid Al-Gaffar",
    val link: String = "https://masjidalgaffar.id/infaq",
    val customQrImage: String = "",
    val showFloatingOnMain: Boolean = true
)

@Serializable
data class OnlineFinanceSettings(
    val enabled: Boolean = false,
    val sheetUrl: String = "",
    val refreshIntervalMinutes: Int = 15,
    val showInMainSlide: Boolean = false,
    val slideDuration: Int = 15
)

@Serializable
data class ScheduledAudioItem(
    val id: String = "",
    val name: String = "",
    val audioFile: String = "",
    val scheduleType: String = "weekly", // "weekly" | "once"
    val dayOfWeek: Int = 5,              // 0=Sun..6=Sat (5=Fri)
    val date: String = "",               // YYYY-MM-DD for "once"
    val time: String = "11:00",          // HH:mm
    val durationMinutes: Int = 15,
    val enabled: Boolean = true
)

@Serializable
data class CustomAudioSettings(
    val isPlayingLive: Boolean = false,
    val liveAudioFile: String = "",
    val liveDurationMinutes: Int = 10,
    val schedules: List<ScheduledAudioItem> = emptyList()
)

// ==================== RELAY / SWITCH CONTROLLER SETTINGS ====================

@Serializable
data class SwitchChannelConfig(
    val id: String = "",
    val name: String = "",
    val pin: Int = 1,
    val enabled: Boolean = false,
    val triggerMode: String = "prayer", // "prayer" | "state"
    val onOffset: Int = -15,            // minutes relative to prayer (e.g. -15 = 15m before prayer)
    val offOffset: Int = 30,            // minutes after prayer
    val activePrayers: List<String> = listOf("fajr", "dhuhr", "asr", "maghrib", "isha"),
    val onState: String = "tartil",
    val onStateOffset: Int = 0,
    val offState: String = "pelaksanaan_sholat",
    val offStateOffset: Int = 0,
    val manualState: Boolean = false
)

@Serializable
data class SwitchSettings(
    val enabled: Boolean = false,
    val connectionType: String = "wifi", // "wifi" | "usb"
    val wifiIp: String = "192.168.1.100",
    val usbPort: String = "COM3",
    val baudRate: Int = 9600,
    val switches: List<SwitchChannelConfig> = defaultSwitchChannels()
) {
    companion object {
        fun defaultSwitchChannels(): List<SwitchChannelConfig> = listOf(
            SwitchChannelConfig(id = "lampu_dalam", name = "Lampu Dalam", pin = 1),
            SwitchChannelConfig(id = "lampu_luar", name = "Lampu Luar", pin = 2),
            SwitchChannelConfig(id = "ac_1", name = "AC 1", pin = 3),
            SwitchChannelConfig(id = "ac_2", name = "AC 2", pin = 4),
            SwitchChannelConfig(id = "ac_3", name = "AC 3", pin = 5),
            SwitchChannelConfig(
                id = "audio_luar",
                name = "Audio Luar (Toa)",
                pin = 6,
                triggerMode = "state",
                onState = "tartil",
                offState = "pelaksanaan_sholat"
            ),
            SwitchChannelConfig(
                id = "audio_dalam",
                name = "Audio Dalam",
                pin = 7,
                triggerMode = "state",
                onState = "pelaksanaan_sholat",
                offState = "standby"
            ),
            SwitchChannelConfig(id = "saklar_8", name = "Saklar 8", pin = 8)
        )
    }
}

// ==================== LAYOUT, THEME & AUTH SETTINGS ====================

@Serializable
enum class LayoutPosition(val serialName: String) {
    @SerialName("horizontal")
    HORIZONTAL("horizontal"),

    @SerialName("vertical-left")
    VERTICAL_LEFT("vertical-left"),

    @SerialName("vertical-right")
    VERTICAL_RIGHT("vertical-right")
}

@Serializable
enum class ScaleMode(val serialName: String) {
    @SerialName("stretch")
    STRETCH("stretch"),

    @SerialName("contain")
    CONTAIN("contain"),

    @SerialName("cover")
    COVER("cover")
}

@Serializable
data class LayoutSettings(
    val position: LayoutPosition = LayoutPosition.HORIZONTAL,
    val autoRotateLayout: Boolean = false,
    val scaleMode: ScaleMode = ScaleMode.STRETCH
)

@Serializable
data class CustomThemeItem(
    val id: String = "",
    val name: String = "Tema Kustom",
    val bgPrimary: String = "#064E3B",
    val bgSecondary: String = "#047857",
    val accent: String = "#10B981",
    val textPrimary: String = "#FFFFFF",
    val textSecondary: String = "#A7F3D0"
)

@Serializable
data class ThemeSettings(
    val currentThemeId: String = "emerald",
    val autoThemeEnabled: Boolean = false,
    val changeOnPrayerTime: Boolean = true,
    val customThemes: List<CustomThemeItem> = emptyList()
)

@Serializable
data class UserAccount(
    val id: String = "superadmin-1",
    val username: String = "admin",
    val pinHash: String = "732911", // Supports both SHA-256 hex or initial default PIN migration
    val role: String = "superadmin" // "superadmin" | "admin" | "user"
)

@Serializable
data class InfoCardItem(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val enabled: Boolean = true
)

// ==================== ROOT SETTINGS OBJECT ====================

@Serializable
data class AppStateSettings(
    val schemaVersion: Int = 2,
    val currentState: AppState = AppState.STANDBY,
    val brand: BrandSettings = BrandSettings(),
    val prayerSettings: PrayerSettings = PrayerSettings(),
    val prayers: FivePrayerConfigs = FivePrayerConfigs(),
    val jumat: JumatSettings = JumatSettings(),
    val ramadhan: RamadhanSettings = RamadhanSettings(),
    val idulFitri: IdulFitriSettings = IdulFitriSettings(),
    val idulAdha: IdulAdhaSettings = IdulAdhaSettings(),
    val infoQurban: InfoQurbanSettings = InfoQurbanSettings(),
    val slides: List<SlideItem> = emptyList(),
    val standbySlideshow: StandbySlideshowSettings = StandbySlideshowSettings(),
    val screensaver: ScreenSaverSettings = ScreenSaverSettings(),
    val cctv: CctvSettings = CctvSettings(),
    val live: LiveSettings = LiveSettings(),
    val masjidQr: MasjidQrSettings = MasjidQrSettings(),
    val onlineFinance: OnlineFinanceSettings = OnlineFinanceSettings(),
    val customAudio: CustomAudioSettings = CustomAudioSettings(),
    val switchSettings: SwitchSettings = SwitchSettings(),
    val layoutSettings: LayoutSettings = LayoutSettings(),
    val themeSettings: ThemeSettings = ThemeSettings(),
    val overlayTextList: List<String> = listOf(
        "Selamat datang di Masjid Al-Gaffar — Mohon luruskan dan rapatkan shaf serta nonaktifkan dering HP saat sholat berlangsung.",
        "Mari makmurkan masjid dengan sholat berjamaah tepat waktu dan infaq terbaik melalui QRIS Masjid Al-Gaffar."
    ),
    val overlayVisibleDuration: Int = 45, // seconds visible
    val overlayHiddenDuration: Int = 10,  // seconds hidden
    val overlaySpeed: Int = 50,
    val mosqueInfoList: List<InfoCardItem> = listOf(
        InfoCardItem(
            id = "info-1",
            title = "Kajian Rutin Ba'da Maghrib",
            content = "Hadirilah kajian tafsir & fiqih setiap malam Jum'at dan Ahad di ruang utama Masjid Al-Gaffar."
        ),
        InfoCardItem(
            id = "info-2",
            title = "Layanan Zakat, Infaq & Sedekah",
            content = "Salurkan infaq terbaik Anda melalui kotak amal atau scan QRIS Digital Masjid Al-Gaffar."
        )
    ),
    val users: List<UserAccount> = listOf(UserAccount())
)

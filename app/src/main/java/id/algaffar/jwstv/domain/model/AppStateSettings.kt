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
    val provinceName: String = "",
    val cityName: String = "",
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
    val name: String = "MASJID ANDA",
    val address: String = "Atur Alamat Masjid melalui Pengaturan atau Wizard",
    val phone: String = "",
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
    val tartilDuration: Int = 20,     // minutes before Adzan (not added on top of tarhimDuration)
    val tarhimDuration: Int = 5,      // minutes before Adzan (auto-synced with audio/video length if available)
    val adzanDuration: Int = 5,       // minutes (auto-synced with audio length if isAdzan == true)
    val iqomahDuration: Int = 10,     // minutes countdown before sholat
    val sholatDuration: Int = 10,     // minutes quiet sholat screen
    val isTartil: Boolean = false,    // enable tartil stage before prayer
    val isTarhim: Boolean = false,    // enable tarhim stage before adzan
    val isAdzan: Boolean = false,     // play adzan audio or visual-only
    val imam: String = "",
    val muadzin: String = "",
    val sholatMessage: String = "Rapatkan Shaf dan Matikan Gadget",
    val adzanTagline: String = "Panggilan Suci",
    val adzanMessage: String = ""
)

@Serializable
data class FivePrayerConfigs(
    val fajr: PrayerStageConfig = PrayerStageConfig(
        tartilDuration = 20,
        tarhimDuration = 5,
        adzanDuration = 5,
        iqomahDuration = 10,
        sholatDuration = 15,
        adzanMessage = "Fajar Menyingsing, Saatnya Bersujud"
    ),
    val dhuhr: PrayerStageConfig = PrayerStageConfig(
        tartilDuration = 20,
        tarhimDuration = 5,
        adzanDuration = 5,
        iqomahDuration = 10,
        sholatDuration = 10,
        adzanMessage = "Cahaya Terang, Hati Tenang Dalam Shalat"
    ),
    val asr: PrayerStageConfig = PrayerStageConfig(
        tartilDuration = 20,
        tarhimDuration = 5,
        adzanDuration = 5,
        iqomahDuration = 10,
        sholatDuration = 10,
        adzanMessage = "Sore Berkah, Langkah Menuju Kemenangan"
    ),
    val maghrib: PrayerStageConfig = PrayerStageConfig(
        tartilDuration = 15,
        tarhimDuration = 5,
        adzanDuration = 5,
        iqomahDuration = 5,
        sholatDuration = 10,
        adzanMessage = "Tenggelam Mentari, Terbitlah Kedamaian Hati"
    ),
    val isha: PrayerStageConfig = PrayerStageConfig(
        tartilDuration = 20,
        tarhimDuration = 5,
        adzanDuration = 5,
        iqomahDuration = 10,
        sholatDuration = 15,
        adzanMessage = "Malam Menjemput, Mengadu Di Depan Sang Pencipta"
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
    val records: Map<String, JumatKeuanganRecord> = emptyMap(),
    val source: String = "manual", // "manual" | "online"
    val customTitle: String = "Laporan Infaq Kas Jum'at"
)

@Serializable
data class JumatSlideshowSettings(
    val showPetugas: Boolean = false,
    val showKeuangan: Boolean = false,
    val slideDuration: Int = 10 // seconds per slide
)

@Serializable
data class JumatSettings(
    val enabled: Boolean = true,
    val waktuJumatDuration: Int = 12,     // minutes before khutbah
    val khutbahDuration: Int = 30,        // minutes
    val sholatJumatDuration: Int = 20,    // minutes
    val khutbahMessage: String = "Harap Tenang dan Menyimak Khutbah",
    val sholatMessage: String = "Rapatkan Shaf dan Matikan Gadget",
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
data class RamadhanJadwalItem(
    val id: String = "",
    val label: String = "",
    val sourceType: String = "prayer", // "prayer" (otomatis hisab) | "custom" (jam manual HH:mm)
    val prayerId: String = "imsak",    // "imsak" | "fajr" | "sunrise" | "dhuha" | "dhuhr" | "asr" | "maghrib" | "isha"
    val customTime: String = "",       // "HH:mm" when sourceType == "custom"
    val highlighted: Boolean = false,
    val enabled: Boolean = true
) {
    companion object {
        fun defaultImsakiyahItems(): List<RamadhanJadwalItem> = listOf(
            RamadhanJadwalItem("im-imsak", "IMSAK", "prayer", "imsak", "", highlighted = true, enabled = true),
            RamadhanJadwalItem("im-fajr", "SUBUH", "prayer", "fajr", "", highlighted = false, enabled = true),
            RamadhanJadwalItem("im-sunrise", "SYURUQ", "prayer", "sunrise", "", highlighted = false, enabled = false),
            RamadhanJadwalItem("im-dhuha", "DHUHA", "prayer", "dhuha", "", highlighted = false, enabled = false),
            RamadhanJadwalItem("im-dhuhr", "DZUHUR", "prayer", "dhuhr", "", highlighted = false, enabled = true),
            RamadhanJadwalItem("im-asr", "ASHAR", "prayer", "asr", "", highlighted = false, enabled = true),
            RamadhanJadwalItem("im-maghrib", "MAGHRIB (BERBUKA)", "prayer", "maghrib", "", highlighted = true, enabled = true),
            RamadhanJadwalItem("im-isha", "ISYA & TARAWIH", "prayer", "isha", "", highlighted = false, enabled = true)
        )
    }
}

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
    val autoSyncNightFromHijri: Boolean = true,
    val hijriYear: String = "1447",
    val tarawihDuration: Int = 15,        // minutes for kultum/info before sholat tarawih
    val ceramahDuration: Int = 0,         // optional dedicated ceramah tarawih phase (minutes)
    val sholatTarawihDuration: Int = 45,  // minutes
    val ceramahMessage: String = "Harap Tenang dan Menyimak Ceramah",
    val sholatMessage: String = "Khusyuk dalam Melaksanakan Sholat",
    val petugas: List<RamadhanPetugasItem> = (1..30).map { RamadhanPetugasItem(malamKe = it) },
    val keuangan: List<RamadhanKeuanganItem> = (1..30).map { RamadhanKeuanganItem(malamKe = it) },
    val takjil: Map<String, List<String>> = emptyMap(), // key: "1".."30", value: list of donor names
    val jadwalTitle: String = "JADWAL IMSAKIYAH & BERBUKA RAMADHAN",
    val jadwalImsakiyah: List<RamadhanJadwalItem> = RamadhanJadwalItem.defaultImsakiyahItems(),
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
    val timeSlots: List<TimeSlot> = listOf(TimeSlot(id = "1", startTime = "06:00", endTime = "09:00", enabled = true)),
    val fullscreenDuration: Int = 30, // seconds in idul_fitri before alternating to idul_fitri_standby
    val standbyDuration: Int = 30,    // seconds in idul_fitri_standby
    val sholatDuration: Int = 45,     // minutes
    val khutbahDuration: Int = 30,    // minutes
    val date: String = "",
    val hijriYear: String = "1447",
    val sholatMessage: String = "Gema Idul Fitri - Raih Kemenangan",
    val khutbahMessage: String = "Menyimak Khutbah Idul Fitri",
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
    val timeSlots: List<TimeSlot> = listOf(TimeSlot(id = "1", startTime = "06:00", endTime = "09:00", enabled = true)),
    val fullscreenDuration: Int = 30,
    val standbyDuration: Int = 30,
    val sholatDuration: Int = 45,
    val khutbahDuration: Int = 30,
    val date: String = "",
    val hijriYear: String = "1447",
    val sholatMessage: String = "Semangat Qurban dan Ketaatan",
    val khutbahMessage: String = "Menyimak Khutbah Idul Adha",
    val petugas: EidPetugas = EidPetugas(),
    val qurbanGroups: List<QurbanGroup> = emptyList(),
    val slideDuration: Int = 10
)

@Serializable
data class InfoQurbanSettings(
    val enabled: Boolean = false,
    val timeSlots: List<TimeSlot> = listOf(TimeSlot(id = "1", startTime = "06:00", endTime = "09:00", enabled = true)),
    val fullscreenDuration: Int = 30,
    val standbyDuration: Int = 30,
    val slideDuration: Int = 10
)

// ==================== SLIDES, STANDBY SLIDESHOW & SCREENSAVER ====================

@Serializable
data class SlideTextStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val fontSize: String = "3xl",     // "base" | "lg" | "xl" | "2xl" | "3xl" | "4xl" | "5xl" | "6xl"
    val fontColor: String = "#FFFFFF",
    val textAlign: String = "center"  // "left" | "center" | "right"
)

@Serializable
data class SlideGlobalConfig(
    val autoPlay: Boolean = true,
    val defaultDuration: Int = 15,
    val maxVideoResolution: String = "1080p" // "720p" | "1080p" | "4k" | "unlimited"
)

@Serializable
data class SlideItem(
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val type: String = "image", // "welcome" | "analog_clock" | "text" | "image" | "video" | system slide types
    val imageUrl: String = "",
    val videoUrl: String = "",
    val duration: Int = 10, // seconds
    val enabled: Boolean = true,
    val showInMain: Boolean = true,
    val showInSlideshow: Boolean = true,
    val showInJumat: Boolean = false,
    val showInRamadhan: Boolean = false,
    val showInIdulFitri: Boolean = false,
    val showInIdulAdha: Boolean = false,
    val hideOverlayText: Boolean = false,
    val hideQrCard: Boolean = false,
    val textStyle: SlideTextStyle = SlideTextStyle()
) {
    companion object {
        const val DEFAULT_WELCOME_TEMPLATE: String =
            "SELAMAT DATANG DI\n{{Nama Masjid}}\n{{Alamat Masjid}}\nSELAMA BERADA DI DALAM MASJID\nHARAP TENANG DAN KHIDMAT"

        /**
         * Default system slides mirroring `DEFAULT_SLIDE_SETTINGS` in `jws-android-algaffar/src/types/appState.ts`.
         */
        fun defaultSystemSlides(): List<SlideItem> = listOf(
            SlideItem(
                id = "default-slide-welcome",
                title = "Ucapan Selamat Datang di Masjid",
                content = DEFAULT_WELCOME_TEMPLATE,
                type = "welcome",
                duration = 15,
                enabled = true,
                showInMain = true,
                showInSlideshow = true,
                showInJumat = true,
                showInRamadhan = true,
                showInIdulFitri = true,
                showInIdulAdha = true
            ),
            SlideItem(
                id = "default-slide-analog-clock",
                title = "Jam Analog Besar",
                content = "Jam Analog Besar",
                type = "analog_clock",
                duration = 20,
                enabled = true,
                showInMain = true,
                showInSlideshow = true,
                showInJumat = true,
                showInRamadhan = true,
                showInIdulFitri = true,
                showInIdulAdha = true
            )
        )
    }
}

@Serializable
data class FloatingOverlayItem(
    val id: String = "",
    val title: String = "",
    val message: String = ""
) {
    companion object {
        fun defaultItems(): List<FloatingOverlayItem> = listOf(
            FloatingOverlayItem(
                id = "overlay-default-1",
                title = "Adab & Ketertiban Shaf",
                message = "Selamat datang — Mohon luruskan dan rapatkan shaf serta nonaktifkan dering HP saat sholat berlangsung."
            ),
            FloatingOverlayItem(
                id = "overlay-default-2",
                title = "Ajakan Makmurkan Masjid",
                message = "Mari makmurkan masjid dengan sholat berjamaah tepat waktu dan infaq terbaik."
            )
        )
    }
}

enum class SlideshowTargetState(val key: String, val label: String) {
    MAIN("main", "MainSlideContainer"),
    FULLSCREEN("slideshow", "Slide Fullscreen"),
    JUMAT("jumat", "Jum'at"),
    RAMADHAN("ramadhan", "Ramadhan"),
    IDUL_FITRI("idul_fitri", "Idul Fitri"),
    IDUL_ADHA("idul_adha", "Idul Adha")
}

@Serializable
data class SlideshowGroup(
    val id: String = "",
    val name: String = "Group 1",
    val enabled: Boolean = true,
    val slides: List<SlideItem> = emptyList(),
    val overlayIds: List<String> = emptyList(),
    val overlayPosition: String = "bottom-left", // "top-left" | "bottom-left" | "top-right" | "bottom-right"
    val showQrCard: Boolean = true,
    val showInMain: Boolean = true,
    val showInSlideshow: Boolean = true,
    val showInJumat: Boolean = false,
    val showInRamadhan: Boolean = false,
    val showInIdulFitri: Boolean = false,
    val showInIdulAdha: Boolean = false
) {
    val activeSlides: List<SlideItem>
        get() = slides.filter { it.enabled }

    val totalDurationSeconds: Int
        get() = activeSlides.sumOf { it.duration.coerceIn(4, 120) }

    val totalAllSlidesDurationSeconds: Int
        get() = slides.sumOf { it.duration.coerceIn(4, 120) }

    val normalizedOverlayPosition: String
        get() = normalizeCornerPosition(overlayPosition)

    /**
     * Card QR Infaq & QR Kas Online selalu ditampilkan berlawanan kiri/kanan
     * dengan posisi Floating Overlay pada baris vertikal yang sama.
     */
    val qrCardPosition: String
        get() = oppositeHorizontalPosition(normalizedOverlayPosition)

    fun matchesState(state: SlideshowTargetState): Boolean = when (state) {
        SlideshowTargetState.MAIN -> showInMain
        SlideshowTargetState.FULLSCREEN -> showInSlideshow
        SlideshowTargetState.JUMAT -> showInJumat
        SlideshowTargetState.RAMADHAN -> showInRamadhan
        SlideshowTargetState.IDUL_FITRI -> showInIdulFitri
        SlideshowTargetState.IDUL_ADHA -> showInIdulAdha
    }

    fun resolveActiveOverlays(allOverlays: List<FloatingOverlayItem>): List<FloatingOverlayItem> {
        if (overlayIds.isEmpty()) return emptyList()
        val byId = allOverlays.associateBy { it.id }
        return overlayIds.mapNotNull { byId[it] }
            .filter { it.title.isNotBlank() || it.message.isNotBlank() }
    }

    fun overlayDurationMillis(activeOverlayCount: Int): Long {
        if (activeOverlayCount <= 0) return 0L
        val totalMs = totalDurationSeconds.coerceAtLeast(1) * 1000L
        return (totalMs / activeOverlayCount).coerceAtLeast(1000L)
    }

    companion object {
        const val POS_TOP_LEFT = "top-left"
        const val POS_BOTTOM_LEFT = "bottom-left"
        const val POS_TOP_RIGHT = "top-right"
        const val POS_BOTTOM_RIGHT = "bottom-right"

        fun normalizeCornerPosition(raw: String): String = when (raw.lowercase().trim()) {
            "top-left", "top" -> POS_TOP_LEFT
            "top-right" -> POS_TOP_RIGHT
            "bottom-right" -> POS_BOTTOM_RIGHT
            else -> POS_BOTTOM_LEFT
        }

        fun oppositeHorizontalPosition(pos: String): String = when (normalizeCornerPosition(pos)) {
            POS_TOP_LEFT -> POS_TOP_RIGHT
            POS_TOP_RIGHT -> POS_TOP_LEFT
            POS_BOTTOM_LEFT -> POS_BOTTOM_RIGHT
            POS_BOTTOM_RIGHT -> POS_BOTTOM_LEFT
            else -> POS_BOTTOM_RIGHT
        }

        fun cornerPositionLabel(pos: String): String = when (normalizeCornerPosition(pos)) {
            POS_TOP_LEFT -> "Kiri Atas"
            POS_TOP_RIGHT -> "Kanan Atas"
            POS_BOTTOM_LEFT -> "Kiri Bawah"
            POS_BOTTOM_RIGHT -> "Kanan Bawah"
            else -> "Kiri Bawah"
        }

        fun nextDefaultGroupName(existing: List<SlideshowGroup>): String {
            val used = existing.map { it.name.trim().lowercase() }.toSet()
            var n = 1
            while ("group $n" in used) {
                n++
            }
            return "Group $n"
        }

        fun defaultGroups(): List<SlideshowGroup> = listOf(
            SlideshowGroup(
                id = "group-default-1",
                name = "Group 1",
                enabled = true,
                slides = SlideItem.defaultSystemSlides(),
                overlayIds = FloatingOverlayItem.defaultItems().map { it.id },
                overlayPosition = POS_BOTTOM_LEFT,
                showQrCard = true,
                showInMain = true,
                showInSlideshow = true,
                showInJumat = false,
                showInRamadhan = false,
                showInIdulFitri = false,
                showInIdulAdha = false
            )
        )
    }
}

@Serializable
data class StandbySlideshowSettings(
    val enabled: Boolean = false,
    val durationMain: Int = 60, // seconds staying in `standby` before entering `standby_slideshow`
    val repeatCount: Int = 1,   // number of full slide cycles in `standby_slideshow` before returning
    val showNextPrayerCard: Boolean = true, // Show floating Next Prayer Card in bottom-right when near Adzan
    val nextPrayerThresholdMinutes: Int = 30
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
    val message: String = "Mode Hemat Daya — Al-Gaffar TV"
)

// ==================== LIVE, QR, ONLINE FINANCE, CUSTOM AUDIO ====================

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
    val makkahHlsUrl: String = "",
    val madinahHlsUrl: String = "",
    val customStreamUrl: String = "",
    val preferNativeHls: Boolean = true,
    val muted: Boolean = false,
    val showInMainSlide: Boolean = false,
    val directLiveEnabled: Boolean = false,
    val directLiveDurationMinutes: Int = 30, // 0 = non-stop / manual stop
    val directLiveStartEpochMs: Long = 0L,
    val schedules: List<LiveScheduleTrigger> = emptyList()
) {
    val activeYoutubeId: String
        get() = when (source) {
            "makkah" -> makkahVideoId
            "madinah" -> madinahVideoId
            else -> customVideoId.ifBlank { makkahVideoId }
        }

    val activeHlsUrl: String
        get() = when (source) {
            "makkah" -> makkahHlsUrl.trim()
            "madinah" -> madinahHlsUrl.trim()
            else -> customStreamUrl.trim().takeIf {
                it.startsWith("http://", true) || it.startsWith("https://", true)
            }?.takeIf {
                it.contains(".m3u8", true) || it.contains("/hls/", true)
            }.orEmpty()
        }
}

@Serializable
data class MasjidQrSettings(
    val enabled: Boolean = false,
    val label: String = "Infaq & Sedekah Digital Masjid",
    val link: String = "",
    val customQrImage: String = "",
    val showFloatingOnMain: Boolean = true,
    val floatingPosition: String = "bottom-right", // "top-left" | "bottom-left" | "top-right" | "bottom-right"
    val bankName: String = "",
    val nomorRekening: String = "",
    val atasNama: String = ""
)

@Serializable
data class OnlineFinanceSettings(
    val enabled: Boolean = false,
    val sheetUrl: String = "",
    val reportUrl: String = "",
    val bankName: String = "",
    val nomorRekening: String = "",
    val atasNama: String = "",
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
data class PrayerSwitchSchedule(
    val onOffset: Int? = null,  // minutes before prayer (positive)
    val offOffset: Int? = null  // minutes after prayer (positive)
)

@Serializable
data class SwitchChannelConfig(
    val id: String = "",
    val name: String = "",
    val pin: Int = 1,
    val enabled: Boolean = false,
    val triggerMode: String = "prayer", // "prayer" | "state"
    val schedules: Map<String, PrayerSwitchSchedule> = emptyMap(),
    val onOffset: Int = -15,            // legacy fallback minutes relative to prayer
    val offOffset: Int = 30,            // legacy fallback minutes after prayer
    val activePrayers: List<String> = listOf("fajr", "dhuhr", "asr", "maghrib", "isha"),
    val onState: String = "tartil",
    val onStateOffset: Int = 0,
    val offState: String = "pelaksanaan_sholat",
    val offStateOffset: Int = 0,
    val manualState: Boolean = false
) {
    companion object {
        fun defaultEmptyPrayerSchedules(): Map<String, PrayerSwitchSchedule> = linkedMapOf(
            "fajr" to PrayerSwitchSchedule(),
            "dhuhr" to PrayerSwitchSchedule(),
            "asr" to PrayerSwitchSchedule(),
            "maghrib" to PrayerSwitchSchedule(),
            "isha" to PrayerSwitchSchedule()
        )
    }
}

@Serializable
data class SwitchSettings(
    val enabled: Boolean = false,
    val connectionType: String = "wifi", // "wifi" | "usb"
    val wifiIp: String = "192.168.1.50",
    val usbPort: String = "/dev/ttyUSB0",
    val baudRate: Int = 9600,
    val switches: List<SwitchChannelConfig> = defaultSwitchChannels()
) {
    companion object {
        private fun acSchedules(): Map<String, PrayerSwitchSchedule> = linkedMapOf(
            "fajr" to PrayerSwitchSchedule(onOffset = 60, offOffset = 30),
            "dhuhr" to PrayerSwitchSchedule(onOffset = 90, offOffset = 60),
            "asr" to PrayerSwitchSchedule(onOffset = 60, offOffset = 45),
            "maghrib" to PrayerSwitchSchedule(onOffset = 60, offOffset = null),
            "isha" to PrayerSwitchSchedule(onOffset = null, offOffset = 45)
        )

        fun defaultSwitchChannels(): List<SwitchChannelConfig> = listOf(
            SwitchChannelConfig(
                id = "lampu_dalam",
                name = "LAMPU DALAM",
                pin = 1,
                enabled = true,
                schedules = linkedMapOf(
                    "fajr" to PrayerSwitchSchedule(onOffset = 30, offOffset = 60),
                    "dhuhr" to PrayerSwitchSchedule(),
                    "asr" to PrayerSwitchSchedule(),
                    "maghrib" to PrayerSwitchSchedule(onOffset = 30, offOffset = null),
                    "isha" to PrayerSwitchSchedule(onOffset = null, offOffset = 60)
                )
            ),
            SwitchChannelConfig(
                id = "lampu_luar",
                name = "LAMPU LUAR",
                pin = 2,
                enabled = true,
                schedules = linkedMapOf(
                    "fajr" to PrayerSwitchSchedule(onOffset = null, offOffset = 60),
                    "dhuhr" to PrayerSwitchSchedule(),
                    "asr" to PrayerSwitchSchedule(),
                    "maghrib" to PrayerSwitchSchedule(onOffset = 30, offOffset = null),
                    "isha" to PrayerSwitchSchedule()
                )
            ),
            SwitchChannelConfig(
                id = "ac_1",
                name = "AC SATU",
                pin = 3,
                enabled = true,
                schedules = acSchedules()
            ),
            SwitchChannelConfig(
                id = "ac_2",
                name = "AC DUA",
                pin = 4,
                enabled = true,
                schedules = acSchedules()
            ),
            SwitchChannelConfig(
                id = "ac_3",
                name = "AC TIGA",
                pin = 5,
                enabled = true,
                schedules = acSchedules()
            ),
            SwitchChannelConfig(
                id = "audio_luar",
                name = "AUDIO LUAR",
                pin = 6,
                enabled = true,
                triggerMode = "state",
                onState = "tartil",
                onStateOffset = -1,
                offState = "pelaksanaan_sholat",
                offStateOffset = 5
            ),
            SwitchChannelConfig(
                id = "audio_dalam",
                name = "AUDIO DALAM",
                pin = 7,
                enabled = true,
                triggerMode = "state",
                onState = "pelaksanaan_sholat",
                onStateOffset = -1,
                offState = "pelaksanaan_sholat",
                offStateOffset = 5
            ),
            SwitchChannelConfig(
                id = "saklar_8",
                name = "SAKLAR 8",
                pin = 8,
                enabled = false
            )
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
    val subtitle: String = "",
    val enabled: Boolean = true
)

@Serializable
data class CustomIslamicEventItem(
    val id: String = "",
    val name: String = "",
    val type: String = "hijri", // "hijri" | "gregorian" | "custom"
    val hijriMonth: Int = 1,
    val hijriDay: Int = 1,
    val gregorianDate: String = "",
    val customSubtitle: String = "",
    val enabled: Boolean = true
)

@Serializable
data class InfoDisplaySettings(
    val enableSunnahFasting: Boolean = true,
    val lookaheadDays: Int = 60,
    val islamicEvents: List<CustomIslamicEventItem> = defaultIslamicEvents()
) {
    companion object {
        fun defaultIslamicEvents(): List<CustomIslamicEventItem> = listOf(
            CustomIslamicEventItem("phbi-1", "Tahun Baru Islam", "hijri", 1, 1),
            CustomIslamicEventItem("phbi-2", "Hari Asyura", "hijri", 1, 10),
            CustomIslamicEventItem("phbi-3", "Maulid Nabi SAW", "hijri", 3, 12),
            CustomIslamicEventItem("phbi-4", "Isra Mi'raj", "hijri", 7, 27),
            CustomIslamicEventItem("phbi-5", "Nisfu Sya'ban", "hijri", 8, 15),
            CustomIslamicEventItem("phbi-6", "Bulan Ramadhan", "hijri", 9, 1),
            CustomIslamicEventItem("phbi-7", "Nuzulul Qur'an", "hijri", 9, 17),
            CustomIslamicEventItem("phbi-8", "Idul Fitri", "hijri", 10, 1),
            CustomIslamicEventItem("phbi-9", "Puasa Arafah", "hijri", 12, 9),
            CustomIslamicEventItem("phbi-10", "Idul Adha", "hijri", 12, 10)
        )
    }
}

@Serializable
data class MediaDisplaySettings(
    val tartilMediaType: String = "image", // "image" | "video"
    val tarhimMediaType: String = "image", // "image" | "video"
    val tartilVideo: String = "",
    val tarhimVideo: String = "",
    val tartilImages: List<String> = emptyList(),
    val tarhimImages: List<String> = emptyList(),
    val tartilSlideInterval: Int = 12,
    val tarhimSlideInterval: Int = 12,
    val doaAdzanEnabled: Boolean = true,
    val doaAdzanDuration: Int = 45, // seconds (15-120)
    val doaAdzanImage: String = "",
    val khutbahJumatImage: String = "",
    val khutbahIdulFitriImage: String = "",
    val khutbahIdulAdhaImage: String = "",
    val ceramahTarawihImage: String = "",
    val sholat5WaktuImage: String = "",
    val sholatJumatImage: String = "",
    val sholatIdulFitriImage: String = "",
    val sholatIdulAdhaImage: String = "",
    val sholatTarawihWitirImage: String = ""
)

// ==================== ROOT SETTINGS OBJECT ====================

@Serializable
data class AppStateSettings(
    val schemaVersion: Int = 5,
    val onboardingCompleted: Boolean = false,
    val currentState: AppState = AppState.STANDBY,
    val brand: BrandSettings = BrandSettings(),
    val prayerSettings: PrayerSettings = PrayerSettings(),
    val prayers: FivePrayerConfigs = FivePrayerConfigs(),
    val jumat: JumatSettings = JumatSettings(),
    val ramadhan: RamadhanSettings = RamadhanSettings(),
    val idulFitri: IdulFitriSettings = IdulFitriSettings(),
    val idulAdha: IdulAdhaSettings = IdulAdhaSettings(),
    val infoQurban: InfoQurbanSettings = InfoQurbanSettings(),
    val slideConfig: SlideGlobalConfig = SlideGlobalConfig(),
    val slides: List<SlideItem> = SlideItem.defaultSystemSlides(),
    val slideGroups: List<SlideshowGroup> = SlideshowGroup.defaultGroups(),
    val standbySlideshow: StandbySlideshowSettings = StandbySlideshowSettings(),
    val screensaver: ScreenSaverSettings = ScreenSaverSettings(),
    val live: LiveSettings = LiveSettings(),
    val masjidQr: MasjidQrSettings = MasjidQrSettings(),
    val onlineFinance: OnlineFinanceSettings = OnlineFinanceSettings(),
    val customAudio: CustomAudioSettings = CustomAudioSettings(),
    val switchSettings: SwitchSettings = SwitchSettings(),
    val layoutSettings: LayoutSettings = LayoutSettings(),
    val themeSettings: ThemeSettings = ThemeSettings(),
    val infoSettings: InfoDisplaySettings = InfoDisplaySettings(),
    val media: MediaDisplaySettings = MediaDisplaySettings(),
    val globalVolume: Float = 1.0f,
    val performanceMode: Boolean = true,
    val virtualKeyboardMode: String = "auto", // "auto" | "enabled" | "disabled"
    val floatingOverlays: List<FloatingOverlayItem> = FloatingOverlayItem.defaultItems(),
    val overlayTextList: List<String> = listOf(
        "Selamat datang — Mohon luruskan dan rapatkan shaf serta nonaktifkan dering HP saat sholat berlangsung.",
        "Mari makmurkan masjid dengan sholat berjamaah tepat waktu dan infaq terbaik."
    ),
    val overlayVisibleDuration: Int = 45, // legacy field kept for JSON compatibility
    val overlayHiddenDuration: Int = 10,  // legacy field kept for JSON compatibility
    val overlaySpeed: Int = 50,           // legacy field kept for JSON compatibility
    val overlayPosition: String = "bottom", // legacy field kept for JSON compatibility
    val overlaySlideClearSeconds: Int = 5,  // legacy field kept for JSON compatibility
    val mosqueInfoList: List<InfoCardItem> = listOf(
        InfoCardItem(
            id = "info-1",
            title = "INFO KAJIAN",
            content = "Memahami Makna Shalat",
            subtitle = "Ba'da Maghrib @ Ruang Utama"
        ),
        InfoCardItem(
            id = "info-2",
            title = "INFAQ & SEDEKAH",
            content = "Salurkan Infaq Terbaik Anda",
            subtitle = "Melalui Kotak Amal / QRIS Masjid"
        )
    ),
    val users: List<UserAccount> = listOf(UserAccount())
) {
    /**
     * Resolves enabled [SlideshowGroup]s targeting the specified [state] that contain at least one enabled slide.
     */
    fun resolveGroupsForState(state: SlideshowTargetState): List<SlideshowGroup> {
        return slideGroups.filter { it.enabled && it.matchesState(state) && it.activeSlides.isNotEmpty() }
    }
}

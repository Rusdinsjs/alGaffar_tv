package id.algaffar.jwstv.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 27 Application States of JWS TV Masjid Al-Gaffar.
 * Serial names match 100% with `AppState` in `src/types/appState.ts`.
 */
@Serializable
enum class AppState(val serialName: String, val displayLabel: String) {
    @SerialName("standby")
    STANDBY("standby", "Standby (Layar Utama)"),

    @SerialName("standby_slideshow")
    STANDBY_SLIDESHOW("standby_slideshow", "Slideshow Layar Penuh"),

    @SerialName("screensaver")
    SCREENSAVER("screensaver", "Screen Saver (Mode Malam)"),

    @SerialName("tartil")
    TARTIL("tartil", "Tartil Al-Qur'an"),

    @SerialName("tarhim")
    TARHIM("tarhim", "Shalawat Tarhim"),

    @SerialName("adzan")
    ADZAN("adzan", "Saatnya Adzan"),

    @SerialName("menunggu_iqomah")
    MENUNGGU_IQOMAH("menunggu_iqomah", "Menunggu Iqomah"),

    @SerialName("pelaksanaan_sholat")
    PELAKSANAAN_SHOLAT("pelaksanaan_sholat", "Pelaksanaan Sholat"),

    @SerialName("waktu_jumat")
    WAKTU_JUMAT("waktu_jumat", "Menjelang Khutbah Jum'at"),

    @SerialName("khutbah")
    KHUTBAH("khutbah", "Pelaksanaan Khutbah Jum'at"),

    @SerialName("pelaksanaan_jumat")
    PELAKSANAAN_JUMAT("pelaksanaan_jumat", "Pelaksanaan Sholat Jum'at"),

    @SerialName("tarawih")
    TARAWIH("tarawih", "Pengumuman & Kultum Tarawih"),

    @SerialName("sholat_tarawih")
    SHOLAT_TARAWIH("sholat_tarawih", "Pelaksanaan Sholat Tarawih"),

    @SerialName("idul_fitri")
    IDUL_FITRI("idul_fitri", "Informasi Idul Fitri"),

    @SerialName("idul_fitri_standby")
    IDUL_FITRI_STANDBY("idul_fitri_standby", "Standby Idul Fitri"),

    @SerialName("sholat_idul_fitri")
    SHOLAT_IDUL_FITRI("sholat_idul_fitri", "Pelaksanaan Sholat Idul Fitri"),

    @SerialName("khutbah_idul_fitri")
    KHUTBAH_IDUL_FITRI("khutbah_idul_fitri", "Khutbah Idul Fitri"),

    @SerialName("idul_adha")
    IDUL_ADHA("idul_adha", "Informasi Idul Adha"),

    @SerialName("idul_adha_standby")
    IDUL_ADHA_STANDBY("idul_adha_standby", "Standby Idul Adha"),

    @SerialName("sholat_idul_adha")
    SHOLAT_IDUL_ADHA("sholat_idul_adha", "Pelaksanaan Sholat Idul Adha"),

    @SerialName("khutbah_idul_adha")
    KHUTBAH_IDUL_ADHA("khutbah_idul_adha", "Khutbah Idul Adha"),

    @SerialName("info_qurban")
    INFO_QURBAN("info_qurban", "Informasi Qurban"),

    @SerialName("info_qurban_standby")
    INFO_QURBAN_STANDBY("info_qurban_standby", "Standby Informasi Qurban"),

    @SerialName("live")
    LIVE("live", "Live Streaming"),

    @SerialName("cctv")
    CCTV("cctv", "Pantauan Kamera CCTV"),

    @SerialName("masjid_qr")
    MASJID_QR("masjid_qr", "QR Code Infaq Masjid"),

    @SerialName("online_finance")
    ONLINE_FINANCE("online_finance", "Laporan Keuangan Online");

    val isFullScreen: Boolean
        get() = this != STANDBY &&
            this != IDUL_FITRI_STANDBY &&
            this != IDUL_ADHA_STANDBY &&
            this != INFO_QURBAN_STANDBY

    val isQuietPrayerExecution: Boolean
        get() = this == PELAKSANAAN_SHOLAT ||
            this == KHUTBAH ||
            this == PELAKSANAAN_JUMAT ||
            this == SHOLAT_TARAWIH ||
            this == SHOLAT_IDUL_FITRI ||
            this == KHUTBAH_IDUL_FITRI ||
            this == SHOLAT_IDUL_ADHA ||
            this == KHUTBAH_IDUL_ADHA

    companion object {
        fun fromSerialName(name: String?): AppState {
            if (name.isNullOrBlank()) return STANDBY
            return entries.firstOrNull { it.serialName.equals(name.trim(), ignoreCase = true) }
                ?: STANDBY
        }
    }
}

/**
 * 8 Daily Prayer / Islamic Time Identifiers.
 */
@Serializable
enum class PrayerId(
    val id: String,
    val defaultIndonesianName: String,
    val arabicName: String,
    val isFiveTimeFardhu: Boolean
) {
    @SerialName("imsak")
    IMSAK("imsak", "Imsak", "إمساك", false),

    @SerialName("fajr")
    FAJR("fajr", "Subuh", "الفجر", true),

    @SerialName("sunrise")
    SUNRISE("sunrise", "Syuruq", "الشروق", false),

    @SerialName("dhuha")
    DHUHA("dhuha", "Dhuha", "الضحى", false),

    @SerialName("dhuhr")
    DHUHR("dhuhr", "Dzuhur", "الظهر", true),

    @SerialName("asr")
    ASR("asr", "Ashar", "العصر", true),

    @SerialName("maghrib")
    MAGHRIB("maghrib", "Maghrib", "المغرب", true),

    @SerialName("isha")
    ISHA("isha", "Isya", "العشاء", true);

    fun displayName(isFriday: Boolean = false): String {
        return if (this == DHUHR && isFriday) "Jum'at" else defaultIndonesianName
    }

    companion object {
        val FIVE_FARDHU: List<PrayerId> = entries.filter { it.isFiveTimeFardhu }

        fun fromId(id: String?): PrayerId? {
            if (id.isNullOrBlank()) return null
            return entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) }
        }
    }
}

package id.algaffar.jwstv.config

/**
 * Centralized external endpoints for Al-Gaffar TV (STB Bundling / Kiosk Edition).
 * Update these URLs in one place to point to your official GitHub Releases, Cloudflare R2,
 * or custom domain before building a commercial release APK.
 */
object AppEndpoints {

    const val PRODUCT_NAME = "Al-Gaffar TV"
    const val SUPPORT_WHATSAPP_HINT = "Hubungi CS Al-Gaffar TV"

    /**
     * JSON manifest URL for OTA self-updates (`versionCode`, `versionName`, `changelog`, `apkUrl`, `sha256`).
     */
    const val OTA_MANIFEST_URL =
        "https://raw.githubusercontent.com/algaffartv-ops/releases/main/ota-manifest.json"

    /**
     * JSON manifest URL for 1-click royalty-free Islamic Audio Packs (Tartil, Tarhim, Adzan, Sirine).
     */
    const val AUDIO_PACKS_MANIFEST_URL =
        "https://raw.githubusercontent.com/algaffartv-ops/releases/main/audio-packs.json"

    /**
     * JSON manifest URL for 1-click royalty-free Islamic Image Packs
     * (Logo, Slideshow Posters, Tartil/Tarhim Slides, Do'a Adzan, Khutbah/Ceramah & Sholat Backgrounds).
     */
    const val IMAGE_PACKS_MANIFEST_URL =
        "https://raw.githubusercontent.com/algaffartv-ops/releases/main/image-packs.json"

    /**
     * Public online finance report viewer URL (replaces personal Netlify URL).
     * When blank or offline, the app falls back to the Google Sheet URL directly or local Ktor report.
     */
    const val FINANCE_VIEWER_BASE_URL =
        "https://laporan.algaffartv.id/"

    /**
     * Optional default HLS (.m3u8) live streams for Makkah (Saudi Quran TV) and Madinah (Saudi Sunnah TV).
     * Can be overridden per mosque in `LiveSettings`.
     */
    const val DEFAULT_MAKKAH_HLS_URL = ""
    const val DEFAULT_MADINAH_HLS_URL = ""
}

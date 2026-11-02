# JWS TV Masjid Al-Gaffar (Native Android TV Edition)

Aplikasi **Digital Signage & Jadwal Waktu Sholat (JWS) Pintar** untuk layar televisi masjid yang dirancang beroperasi **24/7** secara stabil dan hemat memori menggunakan **Kotlin 2.0 & Jetpack Compose for TV**.

> **Catatan untuk AI / Antigravity Agent**: Baca [AGENTS.md](./AGENTS.md) untuk konteks serah terima (*handover*), status implementasi per modul, dan instruksi build/test, serta [PRD.md](./PRD.md) untuk spesifikasi fungsional lengkap.

## Fitur Utama
- **27-State Deterministic State Machine** dengan *Past-Prayer-Time Crash/Boot Recovery* (evaluasi 1Hz).
- **Kalkulasi Waktu Sholat Akurat (`Adhan`) & Kalender Hijriah (`Umm al-Qura`)** (+1 hari otomatis saat Maghrib, deteksi Puasa Sunnah & hitung mundur 12 Hari Besar Islam).
- **Kanvas Adaptif `1920x1080`**, **3 Variasi Layout** (`horizontal`, `vertical-left`, `vertical-right`), **17 Tema Bawaan + Tema Kustom**, dan **5 Gaya Jam Artistik** berbasis Compose Canvas.
- **Audio Engine Native (`Media3 ExoPlayer`)** dengan auto-sync durasi Tarhim & Adzan dari panjang MP3, *smooth volume fade-out* 3 detik, **1000Hz Synthesized Beep (`AudioTrack`)**, dan **Sirine Ramadhan**.
- **Transparansi Keuangan & Petugas Ibadah** (Jum'at, Ramadhan, Ta'jil 30 Hari dengan Import/Export Excel `.xlsx`, Idul Fitri, Idul Adha, Info Qurban, serta sinkronisasi Google Sheets).
- **Streaming CCTV Multi-Kamera Native (RTSP/HLS via ExoPlayer + ONVIF Auto-Discovery)** & **YouTube Live (Makkah/Madinah)**.
- **Otomasi 8 Saklar Relay Masjid** via **WiFi HTTP** dan **USB OTG Serial Relay**.
- **Embedded Ktor Server (Port `1421`)** untuk **Mobile Remote Control PWA** via WiFi lokal dengan QR Code Pairing & autentikasi PIN multi-role (`superadmin`, `admin`, `user`).
- **Kompatibilitas Backup `.zip`** dengan aplikasi JWS TV versi lama (Tauri).

## Perintah Cepat
```bash
# Jalankan unit test
./gradlew testDebugUnitTest

# Build APK Debug
./gradlew assembleDebug
```

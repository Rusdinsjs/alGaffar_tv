# AGENTS.md — Handover & Project Context for Antigravity Agent

Dokumen ini dibuat khusus sebagai **catatan serah terima (handover) & konteks arsitektur** bagi Agent Antigravity di PC lain yang melanjutkan pengembangan proyek **JWS TV Masjid Al-Gaffar (Native Android TV Edition)**.

---

## 1. Ringkasan Proyek
- **Nama Aplikasi**: JWS TV Masjid Al-Gaffar (Native Android TV Edition)
- **Application ID / Namespace**: `id.algaffar.jwstv`
- **Tujuan**: Rewrite 100% *feature-parity* dari aplikasi JWS TV berbasis Tauri v2 + React 19 ke **Native Android (Kotlin 2.0 + Jetpack Compose for TV)** agar berjalan ringan (< 180 MB RAM), stabil 24/7 di Android TV / STB / Mini PC / Tablet, mendukung navigasi Hybrid (Remote TV D-Pad + Touchscreen), serta mengaktifkan fitur hardware native (ExoPlayer RTSP CCTV, USB OTG Serial Relay, AudioTrack 1000Hz Beep, Ktor Remote Server port `1421`).
- **Dokumen Spesifikasi Lengkap**: Lihat [PRD.md](./PRD.md) di root repositori untuk detail lengkap spesifikasi fungsional dan arsitektur.

---

## 2. Status Implementasi Saat Ini (100% Fase 1–5 Selesai & Teruji)

Seluruh 5 fase utama dari [PRD.md](./PRD.md) telah diimplementasikan, dikompilasi menjadi APK (`assembleDebug` sukses), dan seluruh **31 Unit Test lulus (`testDebugUnitTest` 100% PASS)**:

### Fase 1: Foundation, Data Layer, Prayer Engine & 27-State Machine
- **Model & Konfigurasi**:
  - `app/src/main/java/id/algaffar/jwstv/domain/model/AppState.kt`: Definisi lengkap **27 `AppState`** (`standby`, `standby_slideshow`, `screensaver`, `tartil`, `tarhim`, `adzan`, `menunggu_iqomah`, `pelaksanaan_sholat`, `waktu_jumat`, `khutbah`, `pelaksanaan_jumat`, `tarawih`, `sholat_tarawih`, `idul_fitri`, `idul_fitri_standby`, `sholat_idul_fitri`, `khutbah_idul_fitri`, `idul_adha`, `idul_adha_standby`, `sholat_idul_adha`, `khutbah_idul_adha`, `info_qurban`, `info_qurban_standby`, `live`, `cctv`, `masjid_qr`, `online_finance`).
  - `app/src/main/java/id/algaffar/jwstv/domain/model/AppStateSettings.kt`: Skema `@Serializable` lengkap yang 100% kompatibel dengan format backup JSON aplikasi Tauri lama (`version = 4`), mencakup `BrandSettings`, `PrayerSettings`, `ThemeSettings`, `LayoutSettings`, `JumatSettings`, `RamadhanSettings`, `IdulFitriSettings`, `IdulAdhaSettings`, `InfoQurbanSettings`, `SwitchSettings`, `CctvSettings`, `LiveSettings`, `MasjidQrSettings`, `OnlineFinanceSettings`, `ScreenSaverSettings`, `SirineSettings`, dan `CustomAudioSettings`.
  - `app/src/main/java/id/algaffar/jwstv/data/repository/SettingsRepository.kt`: Penyimpanan persisten berbasis **Jetpack DataStore** + migrasi otomatis skema lama.
- **Mesin Kalkulasi Waktu Sholat & Hijriah**:
  - `app/src/main/java/id/algaffar/jwstv/domain/engine/PrayerTimeCalculator.kt`: Kalkulasi 8 waktu harian (`Imsak`, `Subuh`, `Syuruq`, `Dhuha`, `Dzuhur/Jum'at`, `Ashar`, `Maghrib`, `Isya`), 12 metode kalkulasi (termasuk Kemenag/Singapore), 2 madzhab, koreksi per waktu, dan pengaman `ihtiyati`.
  - `app/src/main/java/id/algaffar/jwstv/domain/engine/HijriCalculator.kt`: Kalender Hijriah `HijrahDate` (Umm al-Qura) dengan **pergantian hari otomatis +1 saat waktu Maghrib tiba**, deteksi puasa sunnah (Senin-Kamis, Ayyamul Bidh kecuali Hari Tasyrik, Syawal, Tasu'a/Asyura, Arafah), dan hitung mundur 12 PHBI (lookahead 60 hari).
- **Mesin State Deterministik**:
  - `app/src/main/java/id/algaffar/jwstv/domain/engine/JwsStateMachine.kt`: Evaluasi transisi 1Hz dengan **Past-Prayer-Time Persistence & Crash Recovery** (jika TV baru menyala di tengah waktu Adzan/Iqomah/Sholat/Jum'at/Tarawih, otomatis melompat ke state yang sedang berlangsung dengan sisa detik yang akurat), auto-sync durasi Tarhim & Adzan dari durasi file MP3, serta prioritas state Hari Raya/Qurban/Jadwal Khusus.

### Fase 2: Main Standby UI, 3 Layouts, 17 Themes & 5 Artistic Clocks
- `app/src/main/java/id/algaffar/jwstv/ui/components/Responsive1080pCanvas.kt`: Kanvas logis `1920x1080 dp` dengan mode `stretch`, `contain`, dan `cover`.
- `app/src/main/java/id/algaffar/jwstv/ui/theme/ThemeEngine.kt`: 17 tema bawaan + dukungan tema kustom + rotasi tema otomatis 24 jam tanpa pengulangan.
- `app/src/main/java/id/algaffar/jwstv/ui/components/clock/ArtisticClocks.kt`: 5 gaya jam artistik berbasis Compose Canvas (`classic`, `golden`, `glass`, `wood`, `neon`) untuk varian `header` dan `main`.
- `app/src/main/java/id/algaffar/jwstv/ui/screens/standby/`:
  - `StandbyScreen.kt` (3 layout: `horizontal`, `vertical-left`, `vertical-right` + rotasi layout otomatis tiap 4 jam).
  - `HeaderBar.kt`, `InfoRotatorAndNextPrayer.kt`, `MainSlideContainer.kt`, `PrayerTimesCarousel.kt` (circular 4-item window dari 8 waktu sholat), `OverlayRunningText.kt` (animasi marquee 60 FPS + floating QR masjid).

### Fase 3: Fullscreen Modes, Media3 Audio Engine & 1000Hz Beep
- `app/src/main/java/id/algaffar/jwstv/ui/screens/fullscreen/`:
  - `FullScreenRouter.kt`: Router terpusat untuk seluruh 27 state.
  - `DailyPrayerFullscreens.kt`: Layar `tartil`, `tarhim`, `adzan` (dengan siluet masjid Canvas), `menunggu_iqomah` (dengan banner peringatan larangan sholat sunnah pada sisa waktu <= 120 detik), dan `pelaksanaan_sholat` (mode tenang + overlay CCTV opsional).
  - `SpecialAndHolidayFullscreens.kt`: Layar `waktu_jumat`, `khutbah`, `pelaksanaan_jumat`, `tarawih`, `sholat_tarawih`, `idul_fitri`, `idul_adha`, `info_qurban`, `standby_slideshow`, dan `screensaver` (jam melayang anti-burn-in).
  - `CctvAndLiveFullscreens.kt`: Layar matriks CCTV (`1`, `2`, `4`, `8` grid RTSP/MJPEG), YouTube Live, QR Infaq Masjid penuh, dan tabel Keuangan Online Google Sheets.
- `app/src/main/java/id/algaffar/jwstv/data/audio/`:
  - `BeepGenerator.kt`: Sinyal sinus murni `1000Hz` via Android `AudioTrack` (3x sebelum Adzan, 5x sebelum Iqomah).
  - `JwsAudioEngine.kt`: Integrasi `Media3 ExoPlayer`, resolusi hierarki folder playlist audio (`jws-assets/audio/{category}/{prayerId}`), pembacaan durasi file MP3 otomatis untuk sinkronisasi Tarhim/Adzan, *smooth volume fade-out* 3 detik terakhir, Sirine Ramadhan, dan jadwal Custom Audio.

### Fase 4: Hybrid TV Settings UI (D-Pad + Touch), Multi-Role Auth, Excel & ZIP Backup
- `app/src/main/java/id/algaffar/jwstv/data/auth/AuthManager.kt`: Autentikasi PIN (SHA-256) multi-role (`superadmin` default PIN `732911`, `admin`, `user`) dengan timeout sesi otomatis.
- `app/src/main/java/id/algaffar/jwstv/data/excel/TakjilExcelManager.kt`: Pembaca & pembuat template `.xlsx` (FastExcel) untuk jadwal penyumbang Ta'jil 30 hari Ramadhan.
- `app/src/main/java/id/algaffar/jwstv/data/backup/BackupRestoreManager.kt`: Export/Import arsip `.zip` lengkap (termasuk kompatibilitas penuh dengan file backup `.zip` dari aplikasi Tauri versi lama).
- `app/src/main/java/id/algaffar/jwstv/ui/settings/`:
  - `HybridSettingsComponents.kt` & `SettingsModal.kt`: Modal pengaturan 14 tab yang ramah Remote TV D-Pad (indikator fokus jelas, stepper angka D-Pad) sekaligus nyaman di layar sentuh, dilengkapi tab **Simulasi & Debug** untuk menguji lompat ke salah satu dari 27 state secara instan.

### Fase 5: Embedded Ktor Remote Server (Port 1421), Relay WiFi/USB, CCTV & Kiosk Mode
- `app/src/main/java/id/algaffar/jwstv/data/server/KtorRemoteServer.kt` & `app/src/main/assets/remote/remote_html.html`: Server HTTP + WebSocket di port `1421` dengan proteksi Bearer Token, rate-limiter 120 req/menit/IP, QR Code pairing, dan antarmuka PWA Remote Control untuk HP pengurus masjid.
- `app/src/main/java/id/algaffar/jwstv/data/hardware/SwitchController.kt`: Otomasi 8 channel saklar relay berdasarkan offset waktu sholat (`prayer`) atau transisi state (`state`) melalui **WiFi HTTP Relay** (validasi IP privat LAN) dan **USB Serial OTG** (`usb-serial-for-android`).
- `app/src/main/java/id/algaffar/jwstv/data/cctv/CctvAndOnlineFinanceManager.kt`: Auto-discovery kamera ONVIF UDP (`239.255.255.250:3702`) + RTSP probe, dan sinkronisasi CSV/JSON Google Sheets untuk laporan keuangan online dengan cache offline.
- `app/src/main/java/id/algaffar/jwstv/service/`: `JwsForegroundService.kt` & `BootCompletedReceiver.kt` untuk operasi Kiosk 24/7 dan auto-start saat listrik menyala.

---

## 3. Cara Build & Menjalankan Test di PC Baru

1. **Pastikan JDK 17+ dan Android SDK (API 35) terpasang**:
   Buat file `local.properties` di root proyek (otomatis dibuat oleh Android Studio, atau buat manual):
   ```properties
   sdk.dir=C\:\\Users\\<USERNAME>\\AppData\\Local\\Android\\Sdk
   ```
   *(Sesuaikan path SDK dengan OS/PC baru Anda).*

2. **Menjalankan Seluruh Unit Test (31 Test)**:
   ```bash
   ./gradlew testDebugUnitTest
   ```

3. **Membuat APK Debug**:
   ```bash
   ./gradlew assembleDebug
   ```
   Output APK akan berada di: `app/build/outputs/apk/debug/app-debug.apk`.

---

## 4. Panduan Lanjutan untuk Agent Antigravity Berikutnya
Jika pengguna meminta pengembangan lebih lanjut di PC baru, beberapa area yang dapat dieksplorasi atau disempurnakan:
- **Uji Coba di Perangkat Fisik / Emulator Android TV**: Menginstal `app-debug.apk` via `adb install`, menguji navigasi D-Pad pada setiap tab `SettingsModal`, dan menguji koneksi HP ke `http://<IP_TV>:1421/remote`.
- **Integrasi File Picker Sistem (`ActivityResultContracts`)**: Menghubungkan tombol pilih file gambar/audio/excel/zip di UI `SettingsModal` ke Android Storage Access Framework (SAF) jika pengguna ingin memilih file dari Flashdisk USB OTG di STB.
- **Optimasi R8 / Release Signing**: Menyiapkan keystore rilis untuk `assembleRelease` jika siap diproduksi ke perangkat TV Masjid Al-Gaffar.

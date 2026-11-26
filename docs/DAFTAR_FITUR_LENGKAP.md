# Daftar Fitur Lengkap, Detail & Mendalam — JWS TV Masjid Al-Gaffar (Native Android TV Edition)

Dokumen ini merupakan inventarisasi menyeluruh dan mendalam dari seluruh fitur yang **benar-benar terimplementasi** di dalam kode sumber **JWS TV Masjid Al-Gaffar** (`id.algaffar.jwstv`, *schemaVersion 5*, berbasis **Kotlin + Jetpack Compose for TV + Media3 ExoPlayer + Embedded Ktor Server**).

> [!NOTE]
> **Catatan Sinkronisasi Kode Sumber vs. PRD Awal**
> Implementasi terbaru telah berkembang jauh melampaui dokumen awal (`PRD.md` / `AGENTS.md`). Beberapa penambahan besar di produksi meliputi: ekspansi ke **29 `AppState`** (penambahan `doa_adzan`, `ceramah_tarawih`, dan `finance_qr`), **8 Gaya Jam Artistik**, **27 Sub-Tab Pengaturan (5 Kategori)**, **First-Run Onboarding Wizard 4-Langkah**, **Database Offline 38 Provinsi Indonesia**, **No-RTC Clock Integrity Guard**, **Offline Challenge-Response PIN Recovery**, **1-Click CDN Audio & Image Pack Downloader**, **OTA & Offline USB APK Self-Updater**, **Device Owner Kiosk Mode**, **USB Bulk Folder Sync**, serta **Storage Guard & Orphan Cleaner**. Sebaliknya, modul **CCTV** telah dihapus pada rilis native produksi saat ini demi efisiensi memori STB 24/7.

---

## Ringkasan Peta Fitur Utama

| No | Domain Fitur | Kapabilitas Kunci | File Sumber Utama |
| :--- | :--- | :--- | :--- |
| **1** | **Mesin Waktu Sholat & Kalender Islam** | 8 waktu harian, 12 metode hisab, 2 madzhab, ihtiyati, Hijriah Umm al-Qura (+1 hari saat Maghrib), deteksi puasa sunnah/wajib/haram, PHBI & agenda kustom, database offline 38 provinsi Indonesia. | [PrayerTimeCalculator.kt](../app/src/main/java/id/algaffar/jwstv/domain/engine/PrayerTimeCalculator.kt), [HijriCalculator.kt](../app/src/main/java/id/algaffar/jwstv/domain/engine/HijriCalculator.kt), [IndonesiaCityRepository.kt](../app/src/main/java/id/algaffar/jwstv/data/location/IndonesiaCityRepository.kt) |
| **2** | **Orkestrasi 29-State Machine (1Hz)** | Transisi otomatis harian, Jum'at, Ramadhan, Idul Fitri, Idul Adha, Qurban, Live, QR, Keuangan Online, Screensaver + *Past-Prayer-Time Crash Recovery* lintas tengah malam. | [AppState.kt](../app/src/main/java/id/algaffar/jwstv/domain/model/AppState.kt), [JwsStateMachine.kt](../app/src/main/java/id/algaffar/jwstv/domain/engine/JwsStateMachine.kt) |
| **3** | **Layar Standby, Kanvas, Layout & Tema** | Kanvas logis `1920x1080` (`stretch`/`contain`/`cover`), 3 layout + auto-rotate 4 jam, 17 tema bawaan + tema kustom + auto-theme 24 jam, *Performance Mode* Mali-450. | [ThemeEngine.kt](../app/src/main/java/id/algaffar/jwstv/ui/theme/ThemeEngine.kt), [Responsive1080pCanvas.kt](../app/src/main/java/id/algaffar/jwstv/ui/components/Responsive1080pCanvas.kt) |
| **4** | **8 Gaya Jam Artistik & Jam Analog Besar** | `classic`, `golden`, `glass`, `wood`, `neon`, `segmented`, `emerald`, `monochrome` dengan 3 varian ukuran (`HEADER`, `HEADER_CARD`, `MAIN`) + slide Jam Analog Besar. | [ArtisticClocks.kt](../app/src/main/java/id/algaffar/jwstv/ui/components/clock/ArtisticClocks.kt) |
| **5** | **Slideshow Group, Floating Overlay & Media** | Pengelompokan slide multi-grup (Gambar, Video, Teks, Welcome, Jam Analog), *Floating Overlay* 4 sudut otomatis anti-tabrakan dengan Card QR, latar kustom per tahapan ibadah, **1-Click CDN Image Pack Downloader**. | [AppStateSettings.kt](../app/src/main/java/id/algaffar/jwstv/domain/model/AppStateSettings.kt), [FullScreenRouter.kt](../app/src/main/java/id/algaffar/jwstv/ui/screens/fullscreen/FullScreenRouter.kt), [ImagePackDownloader.kt](../app/src/main/java/id/algaffar/jwstv/data/media/ImagePackDownloader.kt) |
| **6** | **Layar Fullscreen Siklus Ibadah & Hari Besar** | Tartil/Tarhim (Ken Burns + Video + Visualizer), Adzan, Do'a Setelah Adzan, Iqomah (peringatan 2 menit), Sholat Tenang, Jum'at, Tarawih/Ceramah, Idul Fitri, Idul Adha, Qurban, Screensaver. | [DailyPrayerFullscreens.kt](../app/src/main/java/id/algaffar/jwstv/ui/screens/fullscreen/DailyPrayerFullscreens.kt), [SpecialAndHolidayFullscreens.kt](../app/src/main/java/id/algaffar/jwstv/ui/screens/fullscreen/SpecialAndHolidayFullscreens.kt) |
| **7** | **Audio Engine, 1000Hz Beep, Sirine & Audio Pack** | Media3 ExoPlayer, auto-sync durasi MP3, fade-out 3 detik, 1000Hz AudioTrack Beep (3x/5x), Sirine Ramadhan + sintetis sweep, Custom Audio live/terjadwal, 1-Click CDN Audio Pack. | [JwsAudioEngine.kt](../app/src/main/java/id/algaffar/jwstv/data/audio/JwsAudioEngine.kt), [BeepGenerator.kt](../app/src/main/java/id/algaffar/jwstv/data/audio/BeepGenerator.kt), [AudioPackDownloader.kt](../app/src/main/java/id/algaffar/jwstv/data/media/AudioPackDownloader.kt) |
| **8** | **Manajemen Jum'at, Ramadhan, Hari Raya & Excel** | Jadwal petugas & buku kas jurnal Jum'at, Ramadhan 30 malam (petugas, kas pria/wanita, Imsakiyah, laporan akhir, **Excel `.xlsx` Ta'jil**), CRUD Grup Qurban (maks. 7 mudhohi/grup). | [TakjilExcelManager.kt](../app/src/main/java/id/algaffar/jwstv/data/excel/TakjilExcelManager.kt), [AppStateSettings.kt](../app/src/main/java/id/algaffar/jwstv/domain/model/AppStateSettings.kt) |
| **9** | **Transparansi Keuangan Online, QR & Live Streaming** | Google Sheets CSV/REST JSON auto-convert + fallback offline, QR Infaq & QR Kas Online, Siaran Langsung Makkah/Madinah/Kustom via **Native HLS (Media3)** & YouTube Embed. | [CctvAndOnlineFinanceManager.kt](../app/src/main/java/id/algaffar/jwstv/data/cctv/CctvAndOnlineFinanceManager.kt), [CctvAndLiveFullscreens.kt](../app/src/main/java/id/algaffar/jwstv/ui/screens/fullscreen/CctvAndLiveFullscreens.kt) |
| **10** | **Otomasi Saklar Hardware (8-Channel Relay)** | 8 channel (Lampu, AC, Audio Luar/Dalam, Saklar 8), pemicu berbasis Waktu Sholat & State, koneksi **WiFi HTTP Relay** (validasi IP privat) & **USB OTG Serial** (CH340/CP210x/FTDI). | [SwitchController.kt](../app/src/main/java/id/algaffar/jwstv/data/hardware/SwitchController.kt) |
| **11** | **Embedded Ktor Remote Server (Port 1421) & PWA** | Aplikasi Remote HP (PWA installable), Virtual D-Pad, kontrol penuh & upload media/APK dari HP, QR Pairing, RBAC, CORS LAN-only, Rate Limiter, Telemetri STB (Suhu/RAM/Storage). | [KtorRemoteServer.kt](../app/src/main/java/id/algaffar/jwstv/data/server/KtorRemoteServer.kt), [JwsRuntimeEngine.kt](../app/src/main/java/id/algaffar/jwstv/runtime/JwsRuntimeEngine.kt) |
| **12** | **Navigasi Hybrid TV, Onboarding & 27 Tab Pengaturan** | Wizard 4 langkah + Mode Teknisi, Menu Cepat Logo (D-Pad UP), 27 sub-tab pengaturan (5 kategori), Built-in TV/USB File Explorer, Deteksi Keyboard Fisik, Auto-Hide Kursor Mouse, Panel Simulasi. | [MainActivity.kt](../app/src/main/java/id/algaffar/jwstv/MainActivity.kt), [SettingsModal.kt](../app/src/main/java/id/algaffar/jwstv/ui/settings/SettingsModal.kt), [FirstRunOnboardingWizard.kt](../app/src/main/java/id/algaffar/jwstv/ui/onboarding/FirstRunOnboardingWizard.kt) |
| **13** | **Keamanan Multi-Role & Pemulihan PIN Offline** | RBAC 3 role (`superadmin`, `admin`, `user`), hashing **PBKDF2-HMAC-SHA256** (65.536 iterasi + salt), Brute-Force Lockout (5x gagal → kunci 60 detik), **Offline HMAC Challenge-Response PIN Recovery**. | [AuthManager.kt](../app/src/main/java/id/algaffar/jwstv/data/auth/AuthManager.kt) |
| **14** | **Manajemen Storage, USB Sync & Backup/Restore ZIP** | Storage Guard (batas aman 300 MB), Auto-Optimizer gambar (1080p/512px), USB Folder Bulk Sync, Orphan Media Cleaner, Atomic `.bak` power-loss resilience, Backup/Restore `.zip` anti Zip-Slip + kompatibel Tauri lama. | [MediaStorageManager.kt](../app/src/main/java/id/algaffar/jwstv/data/media/MediaStorageManager.kt), [BackupRestoreManager.kt](../app/src/main/java/id/algaffar/jwstv/data/backup/BackupRestoreManager.kt), [SettingsRepository.kt](../app/src/main/java/id/algaffar/jwstv/data/repository/SettingsRepository.kt) |
| **15** | **Ketahanan Kiosk 24/7, No-RTC Guard & OTA Update** | Home Launcher & Device Owner LockTask Mode, Auto-Boot, Foreground Service, No-RTC Clock Integrity Guard + Sync Jam dari HP, Scheduled Auto-Restart/Reboot, Memory Watchdog, OTA HTTPS (SHA-256) & Offline USB APK + Silent Install, System Activity Logger. | [JwsDeviceAdminReceiver.kt](../app/src/main/java/id/algaffar/jwstv/service/JwsDeviceAdminReceiver.kt), [ClockIntegrityGuard.kt](../app/src/main/java/id/algaffar/jwstv/domain/engine/ClockIntegrityGuard.kt), [OtaUpdateManager.kt](../app/src/main/java/id/algaffar/jwstv/data/update/OtaUpdateManager.kt), [AppLogger.kt](../app/src/main/java/id/algaffar/jwstv/data/logging/AppLogger.kt) |

---

## 1. Mesin Kalkulasi Waktu Sholat, Lokasi Offline & Kalender Islam

### 1.1. Kalkulasi Jadwal Sholat Presisi (`PrayerTimeCalculator`)
Berbasis pustaka **Adhan** (`com.batoulapps.adhan`) dengan mekanisme *in-memory caching* agar *tick* 1Hz tidak menghitung ulang astronomi setiap detik:
- **8 Waktu Harian Lengkap**:
  1. `Imsak` (`Subuh - 10 menit + koreksi + ihtiyati`)
  2. `Subuh` (*Fajr*)
  3. `Syuruq` (*Sunrise*)
  4. `Dhuha` (`Syuruq + 20 menit + koreksi`)
  5. `Dzuhur` (*Dhuhr* — otomatis berubah label menjadi **`Jum'at`** pada hari Jum'at jika mode Jum'at aktif)
  6. `Ashar` (*Asr*)
  7. `Maghrib`
  8. `Isya` (*Isha*)
- **12 Metode Kalkulasi Astronomi**: `Singapore` (Default/Kemenag RI), `MuslimWorldLeague`, `Egyptian`, `Karachi`, `UmmAlQura`, `Dubai`, `MoonsightingCommittee`, `NorthAmerica`, `Kuwait`, `Qatar`, `Tehran` (17.7° / 14.0°), dan `Turkey` (18.0° / 17.0°).
- **2 Madzhab Ashar**: `Shafi` (Syafi'i/Maliki/Hambali) dan `Hanafi`.
- **Sistem Koreksi Ganda**:
  - **Koreksi per Waktu Sholat**: Pengaturan offset menit terpisah untuk masing-masing dari 8 waktu (`imsak`, `fajr`, `sunrise`, `dhuha`, `dhuhr`, `asr`, `maghrib`, `isha`).
  - **Koreksi Pengaman Global (`ihtiyati`)**: Default `+2 menit`, otomatis ditambahkan pada `Imsak` dan 5 sholat fardhu (tidak mengubah `Syuruq` dan `Dhuha`).
- **Pelacakan Sholat Berikutnya (`NextPrayer`)**: Secara presisi hanya menghitung mundur ke 5 sholat fardhu hari ini; setelah melewati Isya, otomatis mengambil waktu **Subuh besok**.

### 1.2. Database Lokasi Offline 38 Provinsi Indonesia (`IndonesiaCityRepository`)
- Menyimpan database lokal terkompresi di [indonesia_cities.json](../app/src/main/assets/data/indonesia_cities.json) berisi ratusan Kabupaten/Kota di seluruh **38 Provinsi Indonesia** (serta preset wilayah ASEAN) lengkap dengan `latitude`, `longitude`, dan `timezone` (`Asia/Jakarta`, `Asia/Makassar`, `Asia/Jayapura`, dll.).
- Pengurus masjid cukup memilih Provinsi & Kota menggunakan Remote TV D-Pad tanpa perlu mengetik koordinat GPS secara manual (input koordinat manual tetap didukung).

### 1.3. Kalender Hijriah, Puasa Sunnah & PHBI (`HijriCalculator`)
- **Kalender Umm al-Qura + Kuwaiti Fallback**: Menggunakan `java.time.chrono.HijrahChronology` dengan fallback algoritma Kuwaiti dan koreksi manual `hijriAdjustment` (`-3` s/d `+3` hari).
- **Pergantian Hari Otomatis Saat Maghrib**: Begitu jam melewati waktu `Maghrib`, tanggal Hijriah otomatis bertambah **+1 hari** sesuai kaidah kalender Islam.
- **Deteksi Status Puasa (Hari Ini & Besok)**:
  - **Hari Haram Berpuasa**: 1 Syawal (Idul Fitri), 10 Dzulhijjah (Idul Adha), dan Hari Tasyrik (11–13 Dzulhijjah).
  - **Puasa Wajib**: Ramadhan hari ke-1 s/d ke-30.
  - **Puasa Sunnah Tahunan**: Puasa Arafah (9 Dzulhijjah), Puasa Tarwiyah (8 Dzulhijjah), Puasa Tasu'a (9 Muharram), Puasa Asyura (10 Muharram), dan Puasa 6 Hari Syawal (2–7 Syawal).
  - **Puasa Sunnah Rutin**: Puasa Senin-Kamis dan Puasa Ayyamul Bidh (tanggal 13, 14, 15 tiap bulan Hijriah kecuali Hari Tasyrik — mendukung gabungan label otomatis mis. *"Puasa Sunnah Senin & Puasa Ayyamul Bidh"*).
- **Hitung Mundur PHBI & Agenda Kustom (`InfoDisplaySettings`)**:
  - Memindai jendela `lookaheadDays` (default **60 hari**) ke depan untuk menampilkan hitung mundur menuju 10+ Hari Besar Islam bawaan (Tahun Baru Islam, Asyura, Maulid Nabi, Isra Mi'raj, Nisfu Sya'ban, Awal Ramadhan, Nuzulul Qur'an, Idul Fitri, Puasa Arafah, Idul Adha).
  - Mendukung penambahan agenda masjid kustom berbasis **tanggal Hijriah**, **tanggal Masehi (`YYYY-MM-DD`)**, maupun **pengumuman teks bebas**.

---

## 2. Mesin State Deterministik 29-State (`JwsStateMachine`)

Aplikasi dievaluasi setiap **1 detik (`1Hz`)** dengan urutan prioritas ketat: **Manual/Simulasi → Hari Raya/Qurban → Siklus Sholat/Jum'at/Ramadhan + Recovery → Mode Terjadwal (Live/Screensaver) → Mode Idle (`standby` ↔ `standby_slideshow`)**.

### Daftar Lengkap 29 `AppState`

| Kelompok | Kode State (`serialName`) | Fungsi & Perilaku |
| :--- | :--- | :--- |
| **Idle & Standby (3)** | `standby` | Dashboard utama JWS (Header, Jam, Slide, Kartu Info, NextPrayer, Carousel Sholat, Overlay). |
| | `standby_slideshow` | Slideshow poster/video layar penuh bergantian dengan `standby` berdasarkan `durationMain` & `repeatCount`, opsional menampilkan kartu *Next Prayer* melayang menjelang adzan (`nextPrayerThresholdMinutes`). |
| | `screensaver` | Mode hemat daya/malam dengan layar gelap dan jam melayang anti-*burn-in* sesuai jadwal rentang jam (mis. `22:00`–`03:00`). |
| **Siklus Sholat 5 Waktu (6)** | `tartil` | Aktif `tartilDuration` menit sebelum adzan; memutar audio/video murottal Al-Qur'an. |
| | `tarhim` | Aktif `tarhimDuration` menit sebelum adzan; **Auto-Sync durasi dari panjang file MP3** agar selesai tepat pada detik `00:00` Adzan; memicu **3x Beep 1000Hz** pada sisa `3, 2, 1` detik. |
| | `adzan` | Aktif tepat masuk waktu sholat; memutar audio adzan (jika `isAdzan = true`, durasi mengikuti panjang file MP3) atau mode visual sesuai `adzanDuration`. |
| | `doa_adzan` | Layar penuh **Do'a Setelah Adzan** (teks Arab/Latin/Terjemahan atau poster kustom `doaAdzanImage`) selama `doaAdzanDuration` detik (15–120 detik) sebelum masuk Iqomah. |
| | `menunggu_iqomah` | Hitung mundur Iqomah sesuai `iqomahDuration`; menampilkan **banner peringatan merah larangan memulai sholat sunnah** pada sisa waktu `<= 120 detik`; memicu **5x Beep 1000Hz** pada sisa `5..1` detik. |
| | `pelaksanaan_sholat` | Layar tenang pelaksanaan sholat fardhu (nama Imam, pesan *"Rapatkan Shaf & Matikan Gadget"*, gambar latar opsional `sholat5WaktuImage`), mematikan seluruh audio latar. |
| **Khusus Hari Jum'at (3)** | `waktu_jumat` | Menggantikan fase iqomah Dzuhur pada hari Jum'at; merotasi slide Petugas Jum'at, Keuangan Jum'at, dan slide bertanda `showInJumat`. |
| | `khutbah` | Layar tenang Khutbah Jum'at (nama Khotib, pesan khutbah, gambar latar `khutbahJumatImage`). |
| | `pelaksanaan_jumat` | Layar tenang Sholat Jum'at (nama Imam, pesan sholat, gambar latar `sholatJumatImage`). |
| **Khusus Ramadhan (3)** | `tarawih` | Aktif otomatis setelah `pelaksanaan_sholat` Isya di bulan Ramadhan; merotasi Petugas Tarawih malam ke-N, Keuangan Ramadhan, Jadwal Imsakiyah, Penyumbang Ta'jil, atau Laporan Akhir Ramadhan. |
| | `ceramah_tarawih` | Tahap opsional khusus layar tenang **Ceramah/Kultum Tarawih** (`ceramahDuration` menit) dengan nama Penceramah & latar `ceramahTarawihImage`. |
| | `sholat_tarawih` | Layar tenang pelaksanaan Sholat Tarawih & Witir (menampilkan Imam Tarawih dan Imam Witir secara terpisah bila berbeda). |
| **Idul Fitri (4)** | `idul_fitri` ↔ `idul_fitri_standby` | Bergantian antara informasi petugas & kas Idul Fitri dengan layar standby selama `timeSlots` aktif. |
| | `sholat_idul_fitri` → `khutbah_idul_fitri` | Berjalan berurutan otomatis tepat setelah `timeSlots` Idul Fitri berakhir. |
| **Idul Adha (4)** | `idul_adha` ↔ `idul_adha_standby` | Bergantian antara petugas Idul Adha & daftar kelompok Qurban dengan layar standby selama `timeSlots` aktif. |
| | `sholat_idul_adha` → `khutbah_idul_adha` | Berjalan berurutan otomatis setelah `timeSlots` Idul Adha berakhir. |
| **Info Qurban (2)** | `info_qurban` ↔ `info_qurban_standby` | Mode sosialisasi harian daftar kelompok Qurban Sapi/Kambing pada `timeSlots` yang dijadwalkan. |
| **Layar Khusus (4)** | `live` | Siaran langsung Makkah/Madinah/Kustom (Native HLS / YouTube) secara terjadwal (`LiveScheduleTrigger`) maupun *Direct Live* manual. |
| | `masjid_qr` | Tampilan layar penuh QRIS / QR Infaq Masjid beserta informasi rekening bank. |
| | `finance_qr` | Tampilan layar penuh QR Code tautan Laporan Keuangan Online masjid. |
| | `online_finance` | Tampilan layar penuh ringkasan saldo, pemasukan, pengeluaran, dan 5 transaksi terakhir dari Google Sheets / REST API. |

### Fitur Kritis: *Past-Prayer-Time Persistence & Crash Recovery*
- Jika listrik masjid padam atau STB mengalami *reboot* di tengah ibadah (misal TV menyala pukul 12:18 padahal Dzuhur pukul 12:05), `JwsStateMachine` menghitung selisih waktu sejak waktu sholat dan **langsung melompat** ke state yang sedang berlangsung (`adzan`, `doa_adzan`, `menunggu_iqomah`, `pelaksanaan_sholat`, `waktu_jumat`, `khutbah`, `tarawih`, dll.) dengan **sisa detik hitung mundur akurat**.
- Mendukung **Pemulihan Lintas Tengah Malam**: turut mengevaluasi waktu Isya kemarin sehingga pelaksanaan Tarawih larut malam yang melewati pukul `00:00` tetap pulih dengan tepat.

---

## 3. Tampilan Standby, Kanvas Adaptif, Tema & 8 Gaya Jam Artistik

### 3.1. Kanvas Adaptif `1920 x 1080` & Mode Performa
- Seluruh UI dirender pada koordinat logis `1920x1080 dp` (`Responsive1080pCanvas`) sehingga tampilan proporsional di TV HD Ready (`720p`), Full HD (`1080p`), hingga `4K`, dengan 3 mode skala: `stretch`, `contain`, dan `cover`.
- **Performance Mode (`performanceMode = true` default)**: Dioptimalkan khusus untuk STB Android berspesifikasi rendah (GPU Mali-450 / RAM 1–2 GB):
  - Menonaktifkan transisi *crossfade* berat, efek *Ken Burns*, partikel melayang, aurora radial berlapis, dan pola geometris *Rub el Hizb* di latar belakang.
  - Saat `SettingsModal` terbuka, layar `FullScreenRouter` di belakangnya **tidak di-compose sama sekali**, menghilangkan 100% *overdraw GPU 1080p* dan *recomposition* 1Hz.

### 3.2. 3 Tata Letak (Layout) & Rotasi Anti Burn-in
- **`horizontal`**: Header di atas, area slide utama di kiri, 3 kartu info (`InfoRotator` Masjid, `InfoRotator` Kajian/Puasa/PHBI, `NextPrayer`) di kanan, dan carousel 4 kartu waktu sholat horizontal di bawah.
- **`vertical-left`**: Kolom jadwal sholat vertikal di sisi kiri, area slide & 3 kartu info di sisi kanan.
- **`vertical-right`**: Kolom jadwal sholat vertikal di sisi kanan, area slide & 3 kartu info di sisi kiri.
- **Auto-Rotate Layout**: Rotasi otomatis antar ketiga layout setiap **4 jam** untuk mencegah *burn-in* layar TV.
- **Carousel 8 Waktu Sholat**: Menampilkan 4 kartu sekaligus dari 8 waktu dengan interval geser yang dapat dikonfigurasi (`prayerTimeScrollInterval`, default 3 detik) dan sorotan khusus pada sholat berikutnya.

### 3.3. 17 Tema Visual Bawaan + Tema Kustom + Auto-Theme 24 Jam
Tersedia **17 palet tema terkurasi** (termasuk 2 tema terang `isLight = true`):
1. `default` (*Default Slate*)
2. `emerald` (*Emerald Al-Gaffar*)
3. `midnight` (*Midnight Indigo*)
4. `sunset` (*Warm Sunset*)
5. `ocean` (*Ocean Breeze*)
6. `earth` (*Terracotta Earth*)
7. `luxury` (*Obsidian Luxury*)
8. `minimal` (*Minimal Pearl* — Terang)
9. `royal-gold` (*Royal Gold*)
10. `deep-ocean` (*Deep Ocean*)
11. `obsidian-emerald` (*Obsidian Emerald*)
12. `purple-rain` (*Purple Rain*)
13. `aurora-night` (*Aurora Night*)
14. `islamic-marble` (*Islamic Marble* — Terang)
15. `midnight-forest` (*Midnight Forest*)
16. `royal-desert` (*Royal Desert*)
17. `cyber-silk` (*Cyber Silk*)
- **Tema Kustom**: Pengguna dapat menambah palet tema sendiri (kode hex `bgPrimary`, `bgSecondary`, `accent`, `textPrimary`, `textSecondary`).
- **Auto-Theme Rotation 24 Jam**: Rotasi deterministik berbasis `dayOfYear` & jam tanpa pengulangan berurutan, atau berganti setiap masuk waktu sholat.
- **Latar Belakang Islami (`IslamicAmbientBackground`)**: Gradien dinamis dengan pendar aurora dan *watermark* bintang segi-8 Islami (*Rub el Hizb*).

### 3.4. 8 Gaya Jam Artistik (`ArtisticClocks`)
Menggunakan angka *tabular* (`tnum`) agar posisi digit tidak bergetar setiap detik, tersedia dalam 3 varian ukuran (`HEADER`, `HEADER_CARD`, `MAIN`):
1. `classic` — Minimalis tegas mengikuti tema aktif.
2. `golden` — Aksen bingkai emas metalik & font serif elegan.
3. `glass` — Gaya kaca transparan modern.
4. `wood` — Nuansa kayu hangat klasik.
5. `neon` — Pendar garis neon futuristik.
6. `segmented` — Gaya 7-segment digital khas jam dinding masjid.
7. `emerald` — Aksen zamrud khas masjid.
8. `monochrome` — Kontras tinggi hitam-putih minimalis.

---

## 4. Sistem Slideshow Grup, Floating Overlay & Galeri Media Ibadah

### 4.1. Manajemen Multi-Grup Slideshow (`SlideshowGroup`)
- Mendukung banyak grup slide independen (`Group 1`, `Group 2`, dst.) dengan filter penayangan per konteks state: `MainSlideContainer`, `Slide Fullscreen`, `Jum'at`, `Ramadhan`, `Idul Fitri`, dan `Idul Adha`.
- **Tipe Slide yang Didukung (`SlideItem`)**:
  - `welcome` — Ucapan selamat datang dinamis dengan variabel template `{{Nama Masjid}}` & `{{Alamat Masjid}}`.
  - `analog_clock` — Slide Jam Analog Besar beresolusi tajam.
  - `image` — Poster/gambar lokal (auto-resized ke 1080p).
  - `video` — Video promosi/edukasi lokal (`.mp4`, `.mkv`, `.webm`, `.mov`, `.m4v`, maks 250 MB) diputar via Media3 ExoPlayer dengan batas resolusi (`720p`/`1080p`/`4k`/`unlimited`).
  - `text` — Slide pengumuman teks dengan pengaturan gaya (`bold`, `italic`, `fontSize` `base`–`6xl`, warna hex, perataan `left`/`center`/`right`).
- **Floating Overlay & Anti-Tabrakan QR Card**:
  - Setiap grup dapat menautkan beberapa `FloatingOverlayItem` (judul + pesan) yang durasinya dibagi proporsional terhadap total durasi slide grup.
  - Posisi overlay dapat ditempatkan di 4 sudut (`top-left`, `bottom-left`, `top-right`, `bottom-right`).
  - **Penempatan Cerdas Card QR**: Card QR Infaq & QR Kas Online otomatis ditempatkan di **sisi horizontal berlawanan** dari Floating Overlay pada baris vertikal yang sama sehingga tidak pernah saling menumpuk.

### 4.2. Kustomisasi Media Latar per Tahapan Ibadah (`MediaDisplaySettings`)
- **Tartil & Tarhim**: Pilihan latar berupa **Multi-Slide Gambar** (dengan interval rotasi & efek *Ken Burns*) atau **Video Tilawah/Tarhim** layar penuh (jika video digunakan, audio video otomatis menggantikan playlist MP3 agar tidak bertabrakan).
- **Latar Gambar Khusus per State**: Mendukung gambar latar terpisah untuk **Do'a Setelah Adzan**, **Khutbah Jum'at**, **Khutbah Idul Fitri**, **Khutbah Idul Adha**, **Ceramah Tarawih**, **Sholat 5 Waktu**, **Sholat Jum'at**, **Sholat Idul Fitri**, **Sholat Idul Adha**, dan **Sholat Tarawih & Witir**.

### 4.3. 1-Click CDN Image Pack Downloader (`ImagePackDownloader`)
- Mengunduh dan memasang otomatis paket gambar lengkap ke seluruh **15 slot gambar** aplikasi dalam **1 klik** dari menu **Tampilan → Galeri Media** (`Unduh Paket Gambar Standar (1-Klik)` & `Unduh Paket Gambar Haramain`) maupun **Remote PWA HP** (`download_image_pack`):
  - **Identitas & QR (2 slot)**: `logo` (`brand.logo`, auto-resize `512×512`) dan `masjid_qr` (`masjidQr.customQrImage`, auto-resize `1080×1080`) — mendukung flag `overwrite: false` agar tidak menimpa logo/QRIS kustom masjid yang sudah terpasang.
  - **Slideshow Poster (1 slot multi-item)**: `slide_image` (dipasang ke `slideGroups` & `slides` dengan ID deterministik `slide_pack_<packId>_<n>` sehingga unduh ulang bersifat *idempotent* tanpa duplikasi dan tetap menjaga slide manual masjid).
  - **Slide Tartil & Tarhim (2 slot multi-item)**: `tartil_image` (`media.tartilImages`) dan `tarhim_image` (`media.tarhimImages`) — otomatis beralih ke mode `"image"` serta mendukung opsi `replaceExistingLists`.
  - **Do'a Setelah Adzan (1 slot)**: `doa_adzan` (`media.doaAdzanImage` + otomatis mengaktifkan `doaAdzanEnabled`).
  - **Latar Khutbah & Ceramah (4 slot)**: `khutbah_jumat`, `khutbah_idul_fitri`, `khutbah_idul_adha`, dan `ceramah_tarawih`.
  - **Latar Pelaksanaan Sholat (5 slot)**: `sholat_5_waktu`, `sholat_jumat`, `sholat_idul_fitri`, `sholat_idul_adha`, dan `sholat_tarawih_witir`.
- **Keamanan & Optimasi**: Mewajibkan tautan **HTTPS**, memverifikasi checksum **SHA-256** pada byte mentah sebelum kompresi, menjalankan **Auto-Optimizer 1080p / 512px** (batas maks. 25 MB/file), serta melindungi sisa ruang internal dengan **Storage Guard 300 MB**.
- Endpoint katalog dikelola terpusat di `AppEndpoints.IMAGE_PACKS_MANIFEST_URL`, lengkap dengan template manifest siap pakai di [image-packs.example.json](./image-packs.example.json) dan panduan hosting di [PANDUAN_AUDIO_OTA_DAN_HLS_LIVE.md](./PANDUAN_AUDIO_OTA_DAN_HLS_LIVE.md).

---

## 5. Sistem Audio, Beep 1000Hz, Sirine Ramadhan & Audio Pack

### 5.1. Media3 ExoPlayer Audio Engine (`JwsAudioEngine`)
- **Resolusi Hierarki Playlist Otomatis** (untuk `tartil`, `tarhim`, `adzan` pada 5 waktu sholat):
  1. Seluruh file audio di folder `jws-assets/audio/{category}/{prayerId}/` (playlist berurutan/looping untuk Tartil).
  2. File tunggal `jws-assets/audio/{category}/{prayerId}.{mp3|wav|ogg|m4a|aac}`.
  3. Folder default `jws-assets/audio/{category}/default/`.
  4. File default `jws-assets/audio/{category}/default.mp3`.
- **Auto-Sync Durasi MP3**: Memindai durasi file menggunakan `MediaMetadataRetriever` secara asinkron untuk menyelaraskan durasi state `TARHIM` dan `ADZAN` hingga ke detik.
- **Smooth Volume Fade-Out (3 Detik)**: Menurunkan volume secara bertahap (`72%` → `42%` → `16%` → `0%`) pada 3 detik terakhir sebelum transisi state agar bacaan tidak terputus mendadak.
- **Kontrol Volume Global & Pratinjau Audio**: Pengaturan `globalVolume` (`0%`–`100%`) dan tombol putar/henti pratinjau langsung di menu pengaturan.

### 5.2. Zero-Latency 1000Hz Countdown Beep & Sirine Sintetis (`BeepGenerator`)
- Menggunakan `android.media.AudioTrack` mode `MODE_STATIC` (44.1 kHz, 16-bit PCM mono) di *background thread* khusus:
  - **3x Beep 1000Hz (280ms)** pada detik ke-3, 2, 1 sebelum Adzan.
  - **5x Beep 1000Hz (280ms)** pada detik ke-5, 4, 3, 2, 1 sebelum Iqomah berakhir.
- **Sirine Ramadhan**: Memutar file sirine pilihan sebanyak `1–5x` pada 3 momen independen: **Maghrib (Buka Puasa)**, **Sahur** (jam kustom, default `03:00`), dan **Imsak**. Jika belum ada file MP3 sirine yang diunggah, sistem otomatis membangkitkan **sinyal sirine sweep sintetis 440Hz ↔ 880Hz (3 detik)**.

### 5.3. Custom Audio & 1-Click Audio Pack Downloader
- **Custom Audio (Live & Terjadwal)**: Putar file audio kustom secara instan (*Live Play* dengan durasi menit) atau jadwalkan otomatis secara **mingguan** (hari & jam tertentu, mis. kajian rutin Jum'at) atau **sekali jalan** (tanggal & jam tertentu).
- **1-Click CDN Audio Pack Downloader (`AudioPackDownloader`)**: Mengunduh paket audio Tartil/Tarhim/Adzan/Sirine bebas royalti melalui HTTPS dengan verifikasi checksum **SHA-256** dan *Storage Guard* sehingga ukuran APK dasar tetap ringan.

---

## 6. Manajemen Acara Masjid: Jum'at, Ramadhan, Hari Raya & Excel

1. **Manajemen Hari Jum'at**:
   - **Jadwal Petugas Jum'at**: Berbasis tanggal `YYYY-MM-DD` (dengan *auto-snap* ke hari Jum'at terdekat) mencakup Khotib, Imam, dan Muadzin. Data petugas Jum'at mendatang otomatis efektif sejak H-6.
   - **Buku Kas & Jurnal Jum'at**: Mencatat `saldoKasLalu`, entri jurnal pemasukan (`in`) dan pengeluaran (`out`) dengan deskripsi & nominal, serta menghitung `pemasukan`, `pengeluaran`, dan `saldoAkhir` secara otomatis. Dapat dipilih sumber `manual` atau `online`.
2. **Manajemen Ramadhan 30 Malam**:
   - **Auto-Sync Malam Ramadhan**: Opsi sinkronisasi otomatis nomor malam (`1..30`) langsung dari kalender Hijriah bulan Ramadhan (termasuk perpindahan malam saat Maghrib).
   - **Petugas Tarawih (30 Malam)**: Penceramah, Imam Tarawih, dan Imam Witir per malam.
   - **Keuangan Harian Ramadhan**: Saldo awal, penerimaan kotak amal pria, penerimaan kotak amal wanita, pengeluaran, saldo akhir, serta **Laporan Akhir Ramadhan**.
   - **Jadwal Imsakiyah & Berbuka Dinamis**: Mendukung item otomatis dari hisab (`imsak`, `fajr`, `sunrise`, `dhuha`, `dhuhr`, `asr`, `maghrib`, `isha`) maupun item jam kustom manual (`HH:mm`) dengan status sorotan (`highlighted`).
   - **Jadwal Penyumbang Ta'jil (30 Hari) + Export/Import Excel (`.xlsx`)**: Menggunakan pustaka **FastExcel** untuk mengekspor template `.xlsx` siap isi, serta mengimpor file `.xlsx` (mode tabel 30 hari penuh maupun khusus 1 hari tertentu, mendukung pemisah koma/titik koma) dengan **Dialog Pratinjau Impor** sebelum disimpan.
3. **Manajemen Idul Fitri, Idul Adha & Grup Qurban**:
   - Petugas Hari Raya (Khotib, Imam, Bilal), Keuangan Idul Fitri, serta pengaturan `timeSlots` bergantian (`fullscreenDuration` ↔ `standbyDuration`) yang dilanjutkan otomatis dengan Sholat Id & Khutbah Id.
   - **CRUD Penuh Kelompok Qurban (`QurbanGroup`)**: Mendukung kategori `Sapi` dan `Kambing`, penamaan otomatis (`Kelompok 1, 2, ...`), penataan urutan grup & anggota (*reorder up/down*), serta **validasi kapasitas syar'i maksimal 7 mudhohi per kelompok**.

---

## 7. Transparansi Keuangan Online, QR Digital & Siaran Live Streaming

- **Laporan Keuangan Online (`OnlineFinanceManager`)**:
  - Terhubung langsung ke **Google Sheets** (otomatis mengubah tautan `/pubhtml` menjadi `/pub?output=csv` dan `/edit` menjadi `/export?format=csv`) atau endpoint **REST JSON**.
  - Mem-parsing format nominal Rupiah (`1.500.000`), menghitung total pemasukan, pengeluaran, saldo bersih, dan menampilkan **5 transaksi terakhir**.
  - **Offline Fallback Otomatis**: Jika internet terputus, otomatis menampilkan data lokal Kas Jum'at terakhir tanpa error layar.
- **QR Code Infaq & QR Laporan Keuangan (`MasjidQrSettings`, `OnlineFinanceSettings`)**:
  - Generator QR vektor tajam via **ZXing** maupun unggahan gambar QRIS statis kustom.
  - Dilengkapi tampilan **Nama Bank, Nomor Rekening, dan Atas Nama** baik di kartu melayang layar utama maupun layar penuh (`masjid_qr` dan `finance_qr`).
- **Siaran Langsung (Live Streaming) Hybrid**:
  - Preset siap pakai **Masjidil Haram (Makkah)** dan **Masjid Nabawi (Madinah)** serta dukungan URL/ID kustom.
  - **Prioritas Native HLS (Media3 ExoPlayer)**: Mendukung stream `.m3u8` / HLS secara hardware-accelerated tanpa WebView (lebih hemat RAM), dengan fallback otomatis ke **YouTube No-Cookie Embed** untuk Video ID YouTube.
  - Mendukung mode **Direct Live** (mulai instan dengan timer menit atau non-stop) dan **Jadwal Live Otomatis** relatif terhadap waktu sholat/tartil (`LiveScheduleTrigger`), lengkap dengan overlay header masjid, jam, dan carousel jadwal sholat di bagian bawah.

---

## 8. Otomasi Saklar Listrik & Audio Masjid (8-Channel Relay)

`SwitchController` mengendalikan hingga **8 Channel Saklar** (default: Pin 1 `LAMPU DALAM`, Pin 2 `LAMPU LUAR`, Pin 3–5 `AC SATU..TIGA`, Pin 6 `AUDIO LUAR / TOA`, Pin 7 `AUDIO DALAM`, Pin 8 `SAKLAR 8`):
- **2 Mode Pemicu Otomatis**:
  1. **Berbasis Waktu Sholat (`prayer`)**: Jadwal `onOffset` (menit sebelum sholat) dan `offOffset` (menit setelah sholat) yang dapat dikonfigurasi **berbeda-beda untuk masing-masing 5 sholat** (termasuk evaluasi lintas hari `-1..+1` hari agar lampu malam Maghrib→Subuh berjalan mulus; jika bentrok waktu, `OFF` diprioritaskan demi keamanan).
  2. **Berbasis State Aplikasi (`state`)**: Menyalakan/mematikan perangkat saat masuk state tertentu + offset menit (contoh bawaan: `AUDIO LUAR` menyala saat `tartil` dan mati saat `pelaksanaan_sholat`; `AUDIO DALAM` menyala saat `pelaksanaan_sholat`).
- **2 Mode Koneksi Hardware**:
  1. **WiFi HTTP Relay (ESP8266 / ESP32 / Tasmota / Sonoff)**: Mengirim `GET http://{wifiIp}/relay/{pin}/{on|off}` dengan **validasi keamanan IP privat LAN** (mencegah SSRF ke internet publik).
  2. **USB OTG Serial Relay**: Mengirim protokol serial `{pin}:{1|0}\n` langsung melalui port USB Android TV Box (`usb-serial-for-android`, mendukung chip CH340/CP210x/FTDI dengan pilihan baud rate `9600`–`115200` dan permintaan izin USB native).
- **Kontrol Manual & Uji Pin**: Setiap pin (1–8) dapat diuji ON/OFF secara manual dari menu Pengaturan TV maupun Remote HP.

---

## 9. Embedded Ktor Remote Server (Port 1421) & Aplikasi PWA HP

Server HTTP `Ktor CIO` tertanam langsung di *process-lifetime runtime engine* (`0.0.0.0:1421`) dan bekerja **100% lokal via WiFi/LAN tanpa internet**:
- **Smart Remote PWA (`http://<IP_TV>:1421/remote`)**:
  - Dilengkapi `/manifest.json` dan Service Worker (`/sw.js`) sehingga pengurus masjid dapat menginstalnya ke *Home Screen* HP layaknya aplikasi native.
  - **QR Code Pairing Sekali Pindai**: QR Code di layar TV berisi URL + Bearer Token sehingga pengurus tidak perlu mengetik IP atau token.
  - **Virtual D-Pad Remote**: Mengirim tombol `Up`, `Down`, `Left`, `Right`, `Enter/OK`, `Back` langsung ke *key pipeline* Android Activity.
  - **Kontrol & Manajemen Penuh via HP**: Ganti tema/layout/jam, simulasi state, edit identitas masjid, running text/floating overlay, jadwal sholat, petugas & jurnal kas Jum'at, Ramadhan 30 malam + Ta'jil + Sirine, Idul Fitri, Idul Adha + CRUD Grup Qurban, Screensaver, 8-Channel Relay, Live Streaming, unduh **Paket Audio & Paket Gambar 1-Klik (`download_audio_pack` / `download_image_pack`)**, hingga menyelesaikan **Setup Awal (Onboarding)** langsung dari HP.
  - **Direct Streaming Upload dari HP (`/api/upload`)**: Unggah langsung dari galeri HP ke STB untuk Logo, Poster Slide, Video Slide (maks 250 MB), 10 slot Latar Media Ibadah, Gambar/Video Tartil & Tarhim, Audio Sholat/Kustom (maks 50 MB), hingga **Pembaruan APK Offline (maks 180 MB)**.
  - **Download Backup JSON (`/api/backup`)** & **Sinkronisasi Jam TV dari HP (`/api/sync_time`)**.
- **Dashboard Telemetri Hardware Real-Time (`/api/status`)**:
  - Melaporkan suhu CPU/SoC perangkat STB (`thermalTempC` dari `/sys/class/thermal/`), penggunaan RAM aplikasi & sistem, *uptime*, status sinkronisasi jam No-RTC, daftar 25 log terbaru, serta rincian kapasitas penyimpanan internal (gambar, audio, video, jumlah & ukuran file *orphan*).
- **Keamanan Berlapis**:
  - Autentikasi **Bearer Token** persisten (dapat di-regenerate kapan saja) atau autentikasi PIN `superadmin`/`admin` (tanpa backdoor hardcoded).
  - **Role-Based Access Control (RBAC)** API: Aksi kritis (`update_admin_pin`, `regenerate_remote_token`, `clear_logs`, `factory_reset`, `ota_install`, `check_ota_update`, upload `ota_apk`) dibatasi ketat hanya untuk `SUPERADMIN`.
  - **LAN-Only Origin CORS Guard**: Memblokir request lintas situs dari domain internet publik.
  - **IP Sliding-Window Rate Limiter**: Maksimal 180 request/menit/IP + sanitasi XSS/HTML pada seluruh input.

---

## 10. Antarmuka Hybrid TV (D-Pad + Touch), Onboarding & 27 Tab Pengaturan

### 10.1. First-Run Onboarding Wizard (4 Langkah)
Tampil otomatis saat pertama kali STB dinyalakan (`onboardingCompleted = false`), mendukung operasi paralel lewat **Remote TV D-Pad** maupun **scan QR dari HP**:
1. **Langkah 1/4**: Identitas Masjid (Nama, Alamat, Telepon).
2. **Langkah 2/4**: Pilih Provinsi & Kabupaten/Kota (38 Provinsi Indonesia) + Metode Kalkulasi dengan **Pratinjau Jadwal Sholat Langsung**.
3. **Langkah 3/4**: Buat PIN Pengurus DKM baru (minimal 6 digit, di-hash PBKDF2).
4. **Langkah 4/4**: Pilih Tema, Gaya Jam, dan tombol **Uji Suara Beep 1000Hz**.
- Dilengkapi tombol **Mode Teknisi (Lewati)** untuk instalasi cepat oleh distributor/teknisi.

### 10.2. Navigasi Cerdas & Kenyamanan Penggunaan TV
- **Logo Quick Action Menu**: Tekan tombol **D-Pad UP** di remote TV (atau klik Logo Masjid) untuk membuka menu cepat: Ganti Tema, Ganti Layout, Ganti Gaya Jam, Panel Simulasi, dan Pengaturan.
- **Panel Simulasi & Debug**: Lompat instan ke salah satu dari seluruh `AppState` dengan timer otomatis, uji Beep 1000Hz, uji Sirine Ramadhan, serta pill kontrol mengambang *"Kembali ke Standby"* saat simulasi aktif.
- **Built-in TV & USB File Explorer (`TvFilePickerDialog`)**: Penjelajah berkas internal khusus Remote TV (mendukung Memori Internal & Flashdisk USB OTG) sebagai solusi perangkat STB yang tidak memiliki aplikasi *System Document Picker* bawaan, dengan fallback otomatis ke Android SAF atau simpan langsung ke folder `Downloads`.
- **Deteksi Keyboard Fisik & Kebijakan Keyboard Virtual**: Mendeteksi otomatis keyboard USB/Bluetooth fisik dan menyediakan opsi `auto` / `enabled` / `disabled` agar keyboard virtual Android TV tidak mengganggu saat menggunakan keyboard fisik.
- **Auto-Hide Kursor Mouse**: Menyembunyikan kursor mouse secara otomatis setelah **3 detik** tidak digerakkan agar layar masjid selalu bersih.

### 10.3. Struktur Lengkap 27 Sub-Tab Pengaturan (5 Kategori)
Navigasi pengaturan dirancang dengan *Top Header Bar*, *Scrollable Content*, *Elevated Sub-Menu Bar*, dan *5-Category Bottom Bar*:
1. **Kategori Masjid**: `Identitas Masjid`, `QR Infaq Digital`, `Keuangan Online`.
2. **Kategori Sholat**: `Waktu & Tahapan Sholat`, `Audio Sholat`, `Audio Kustom`.
3. **Kategori Tampilan**: `Tema Visual`, `Tata Letak`, `Gaya Jam (8)`, `Overlay Text`, `Slideshow Info`, `Kartu Info & PHBI`, `Galeri Media`, `Mode Standby`.
4. **Kategori Acara**: `Jum'at`, `Ramadhan`, `Idul Fitri`, `Idul Adha & Qurban`, `Screensaver & State`, `Siaran Live`, `Hardware Saklar`.
5. **Kategori Sistem**: `Remote & Jaringan`, `Keyboard & Input`, `Kelola Admin`, `Data & Backup`, `Log Aktivitas`, `Panduan Remote`.

---

## 11. Keamanan Multi-Role & Pemulihan PIN 100% Offline

- **3 Tingkatan Peran (RBAC)**: `Super Admin` (akses penuh 27 tab), `Admin DKM` (operasional harian & konfigurasi), dan `Operator Harian` (akses terbatas).
- **Kriptografi PIN Modern**: Menggunakan **PBKDF2-HMAC-SHA256** (65.536 iterasi, salt acak 16-byte) dengan *auto-upgrade* transparan dari hash SHA-256 lama maupun PIN bawaan saat login pertama.
- **Proteksi Brute-Force (`PinLockoutTracker`)**: Salah memasukkan PIN **5 kali** berturut-turut otomatis mengunci login selama **60 detik**.
- **Offline Challenge-Response PIN Recovery (`OfflinePinRecoveryEngine`)**: Jika pengurus lupa PIN Superadmin di masjid tanpa internet, TV menampilkan **Device ID 8-karakter + Challenge Code 6-karakter**; CS dapat menghasilkan **Kode Pemulihan 6-digit berbasis HMAC-SHA256** (dengan toleransi ±1 hari) untuk mereset PIN secara 100% offline.

---

## 12. Manajemen Penyimpanan, USB Bulk Sync & Backup/Restore `.zip`

- **Storage Health Guard (`MediaStorageManager`)**: Memeriksa sisa ruang disk secara berkala tiap 2 MB saat streaming upload dan menolak file jika sisa memori internal TV akan turun di bawah ambang aman **300 MB**.
- **Image Auto-Optimizer**: Mengubah ukuran & mengompresi gambar yang masuk secara otomatis agar pas dengan kanvas `1920x1080` (atau `512x512` untuk Logo, JPEG kualitas 85% / mempertahankan PNG) demi hemat RAM & ruang penyimpanan.
- **USB Folder Bulk Sync (1-Klik)**: Menyalin seluruh isi satu folder dari Flashdisk USB OTG ke memori internal TV (mendukung struktur folder `jws-assets/` maupun folder datar; otomatis mengenali `logo`, `slides`, `videos`, dan audio berawalan `tarhim_fajr.mp3` / `adzan-maghrib.mp3`), lalu membuatkan slide otomatis sehingga flashdisk aman langsung dicabut.
- **Orphan Media Cleaner**: Memindai folder `images/` dan `videos/` untuk menemukan dan menghapus file media tidak terpakai (*orphan*) serta file `.tmp` sisa gagal upload (tidak pernah menyentuh koleksi `audio/`).
- **Ketahanan Data Mati Listrik (`SettingsRepository`)**: Penulisan file konfigurasi atomik + salinan ganda `.bak`. Jika `jws_settings.json` rusak akibat listrik mati mendadak, file rusak otomatis dikarantina (`.corrupt.<timestamp>.json`) dan pengaturan dipulihkan otomatis dari `.bak`.
- **Full Backup & Restore `.zip` (`BackupRestoreManager`)**: Mengekspor dan memulihkan `settings.json` + seluruh media `jws-assets/` dalam satu file `.zip` dengan proteksi keamanan **anti Zip-Slip** serta **kompatibilitas impor penuh dari file backup aplikasi versi lama (Tauri)** termasuk migrasi skema otomatis hingga `schemaVersion = 5`.

---

## 13. Ketahanan Kiosk 24/7, No-RTC STB Guard, Watchdog & OTA Update

- **Arsitektur Process-Lifetime 24/7**: `JwsRuntimeEngine` hidup di level `AppContainer` dan dijaga oleh `JwsForegroundService` (`mediaPlayback`) + `FLAG_KEEP_SCREEN_ON` sehingga 1Hz ticker, server Ktor 1421, audio sholat, dan relay tidak pernah mati walau Activity di-recreate.
- **Auto-Boot & Home Launcher**: Terdaftar sebagai `LEANBACK_LAUNCHER` sekaligus `HOME` Launcher dan menerima `BOOT_COMPLETED` / `QUICKBOOT_POWERON` agar langsung menyala ke layar JWS saat listrik masjid hidup kembali.
- **Full OS Kiosk Mode (`JwsDeviceAdminReceiver`)**: Jika diprovisioning sebagai **Device Owner** (`dpm set-device-owner`), mengaktifkan **Lock Task Mode** secara senyap (tombol Home/Recent terkunci total), dilengkapi tombol **Buka Kiosk Sementara (Maintenance)** serta pintasan ke *Android Settings* dan *Wi-Fi Settings* dari dalam Pengaturan.
- **Scheduled Kiosk Auto-Restart / Auto-Reboot (`KioskMaintenanceSettings`)**: Jadwal otomatis restart aplikasi atau reboot OS TV Box (harian atau mingguan pada jam/hari pilihan, mis. pukul 03:00) yang hanya dieksekusi saat state sedang `STANDBY` agar tidak pernah mengganggu ibadah.
- **No-RTC STB Clock Integrity Guard (`ClockIntegrityGuard`)**:
  - Mendeteksi STB murah tanpa baterai CMOS/RTC yang jamnya melompat kembali ke tahun `< 2025` atau mundur `> 1 jam` setelah mati lampu.
  - Menampilkan **Banner Peringatan Merah** di bagian atas TV dengan tautan Remote HP.
  - Menerapkan *software clock offset* dari jam HP pengurus (`/api/sync_time`), menyimpan *anchor* waktu ke disk tiap 1 jam, dan otomatis menormalkan offset begitu NTP internet Android kembali aktif.
- **Proactive Memory Watchdog**: Memeriksa kesehatan memori setiap 30 detik secara otomatis di latar belakang untuk mencegah *Out-Of-Memory* pada operasional berbulan-bulan.
- **OTA Online & Offline USB APK Self-Updater (`OtaUpdateManager`)**:
  - Cek & unduh pembaruan manifest JSON via **HTTPS wajib** + verifikasi **SHA-256** file APK.
  - Mendukung instalasi APK offline dari **Flashdisk USB OTG** maupun **unggah lewat Remote HP** (validasi *magic header* ZIP/APK & batas 180 MB).
  - **Silent Auto-Install**: Otomatis menginstal pembaruan tanpa popup konfirmasi jika perangkat berstatus **Device Owner** (`PackageInstaller.Session`), atau meluncurkan installer sistem via `FileProvider` pada STB biasa.
- **Real-Time Activity Log Viewer (`AppLogger`)**: Mencatat hingga 300 entri log terstruktur (`INFO`, `WARN`, `ERROR`, `DEBUG` dengan tag `SYSTEM`, `STATE`, `REMOTE`, `MEDIA`, `CLOCK`, `KIOSK`, `AUTH`, `OTA`, `RAMADHAN`) yang dapat dipantau langsung di layar TV maupun di Remote PWA HP.

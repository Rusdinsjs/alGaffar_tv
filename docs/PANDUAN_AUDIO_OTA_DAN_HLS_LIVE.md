# Panduan Lengkap Paket Audio, OTA Self-Updater & HLS Live — Al-Gaffar TV

Dokumen ini berisi panduan praktis untuk mengelola **Paket Audio (Tartil, Tarhim, Adzan)**, **OTA Self-Updater**, serta **Live Streaming HLS (`.m3u8`)** pada aplikasi **JWS TV Masjid Al-Gaffar (`id.algaffar.jwstv`)**.

---

## 1. Paket Audio (HTTPS 1-Klik, Flashdisk USB, & Remote PWA)

Aplikasi tidak membundel file MP3 di dalam APK agar ukuran APK tetap ringan. Seluruh file audio disimpan di penyimpanan internal aplikasi pada folder `jws-assets/audio/`.

### A. Mengunggah Audio dari HP / PC via Remote PWA
1. Buka Remote PWA di browser HP/PC: `http://<IP_TV>:1421/remote` (atau pindai QR Code di **Pengaturan TV → Tab Sistem → Remote & Jaringan**).
2. Buka **Tab `Sholat`**, lalu gulir ke kartu **"Upload Audio Tartil / Tarhim / Adzan"**.
3. Pilih:
   - **Kategori Audio**: `tartil`, `tarhim`, `adzan`, atau `custom` (Murottal).
   - **Slot Waktu Sholat**: `fajr` (Subuh), `dhuhr` (Dzuhur), `asr` (Ashar), `maghrib`, `isha` (Isya), `jumat`, atau `default` (dipakai untuk semua waktu yang belum punya file khusus).
4. Pilih file audio (`.mp3`, `.wav`, `.m4a`, `.ogg`, maksimal **50 MB**) → klik **📤 UNGGAH AUDIO KE TV**.

### B. Mengisi Audio Massal via Flashdisk USB
1. Di PC/Laptop, buat struktur folder berikut di Flashdisk USB:
   ```text
   jws-assets/
   └── audio/
       ├── tartil/
       │   ├── default.mp3
       │   └── fajr.mp3        (opsional per waktu sholat)
       ├── tarhim/
       │   └── default.mp3
       ├── adzan/
       │   ├── fajr.mp3
       │   └── default.mp3
       └── custom/
           └── murottal_pagi.mp3
   ```
   *Nama slot yang didukung: `fajr`, `dhuhr`, `asr`, `maghrib`, `isha`, `jumat`, `default`.*
2. Colokkan Flashdisk ke port USB STB/TV.
3. Di TV, buka **Pengaturan → Tab `Sholat`** (untuk pilih file per slot) atau **Tab `Sistem` → Sync Massal Folder USB** untuk menyalin seluruh folder ke memori internal TV.

### C. Membuat Paket Audio Online (HTTPS) untuk Tombol 1-Klik di TV
Tombol **"Unduh Paket Audio Standar (1-Klik)"** dan **"Unduh Paket Adzan Makkah & Madinah"** di **Pengaturan TV → Tab `Sholat`** membaca katalog JSON dari `AppEndpoints.AUDIO_PACKS_MANIFEST_URL`. Jika URL belum diisi/diunggah, TV akan menampilkan error `HTTP 404`.

Langkah mengaktifkannya:
1. Unggah file `.mp3` ke hosting HTTPS (GitHub Releases, GitHub repo, atau Cloudflare R2).
2. Buat file JSON **`audio-packs.json`** dengan ID `standard_pack` dan `haramain_pack`:
   ```json
   {
     "packs": [
       {
         "id": "standard_pack",
         "title": "Paket Audio Standar",
         "description": "Tartil, Tarhim & Adzan standar masjid",
         "sizeLabel": "15 MB",
         "tracks": [
           { "category": "tartil", "slot": "default", "url": "https://domain-anda.com/audio/tartil_default.mp3", "sha256": "" },
           { "category": "tarhim", "slot": "default", "url": "https://domain-anda.com/audio/tarhim_default.mp3", "sha256": "" },
           { "category": "adzan", "slot": "fajr", "url": "https://domain-anda.com/audio/adzan_subuh.mp3", "sha256": "" },
           { "category": "adzan", "slot": "default", "url": "https://domain-anda.com/audio/adzan_umum.mp3", "sha256": "" }
         ]
       },
       {
         "id": "haramain_pack",
         "title": "Paket Adzan Makkah & Madinah",
         "description": "Adzan Masjidil Haram & Masjid Nabawi",
         "sizeLabel": "12 MB",
         "tracks": [
           { "category": "adzan", "slot": "default", "url": "https://domain-anda.com/audio/adzan_makkah.mp3", "sha256": "" }
         ]
       }
     ]
   }
   ```
   *(Catatan: URL trek wajib `https://`. Kolom `sha256` opsional; jika diisi 64-karakter SHA-256, aplikasi otomatis memverifikasi keaslian file setelah diunduh.)*
3. Unggah `audio-packs.json` ke server HTTPS Anda, lalu perbarui `AUDIO_PACKS_MANIFEST_URL` di `app/src/main/java/id/algaffar/jwstv/config/AppEndpoints.kt` dan build ulang APK.

---

## 2. OTA Self-Updater (`OtaUpdateManager`)

OTA Self-Updater memungkinkan pembaruan aplikasi Al-Gaffar TV di STB/TV tanpa mencabut perangkat atau lewat Play Store.

### Lokasi Menu di Aplikasi
- **Pengaturan TV → Tab `Sistem` → Pembaruan Al-Gaffar TV (OTA) & Setup Perdana**:
  - **Cek Pembaruan Online (OTA)**
  - **Update APK dari Flashdisk USB**
- **Remote Server (`KtorRemoteServer`)**: Mendukung endpoint `POST /api/upload?category=ota_apk` (khusus role **Super Admin**), sementara UI pengelolaannya di TV tersedia di Tab `Sistem`.

### A. Panduan Update OTA Jalur Internet (HTTPS + SHA-256)
1. Naikkan `versionCode` (misal `2` → `3`) dan `versionName` di `app/build.gradle.kts`.
2. Build APK rilis dengan keystore produksi Anda:
   ```bash
   ./gradlew assembleRelease
   ```
3. Hitung checksum SHA-256 dari file APK rilis:
   ```bash
   sha256sum app/build/outputs/apk/release/app-release.apk
   ```
4. Unggah `app-release.apk` ke GitHub Releases / hosting HTTPS Anda.
5. Buat file JSON **`ota-manifest.json`**:
   ```json
   {
     "versionCode": 3,
     "versionName": "2.2.0-stb",
     "changelog": "Pembaruan fitur dan perbaikan stabilitas",
     "apkUrl": "https://github.com/Rusdinsjs/alGaffar_tv/releases/download/v2.2.0/app-release.apk",
     "sha256": "TEMPEL_64_KARAKTER_SHA256_DI_SINI",
     "minAndroidSdk": 26
   }
   ```
   *(Catatan: `apkUrl` wajib `https://`. Aplikasi akan menolak APK jika `sha256` tidak cocok.)*
6. Unggah `ota-manifest.json` ke server HTTPS dan arahkan `OTA_MANIFEST_URL` di `AppEndpoints.kt` ke URL raw JSON tersebut.
7. Di TV, klik **Cek Pembaruan Online (OTA)** → aplikasi otomatis mengunduh, memverifikasi SHA-256, dan membuka installer Android.

### B. Panduan Update OTA Jalur Flashdisk USB (Offline)
1. Salin file `.apk` versi baru (harus ditandatangani dengan *keystore* yang sama dengan aplikasi terpasang, ukuran valid `100 KB – 180 MB`) ke Flashdisk USB.
2. Colokkan Flashdisk ke STB/TV.
3. Buka **Pengaturan TV → Tab `Sistem`** → klik **"Update APK dari Flashdisk USB"** → pilih file `.apk` → ikuti konfirmasi instalasi di layar TV.

---

## 3. Live Streaming HLS (`.m3u8`) & Fallback YouTube

### Kapan URL HLS Dikatakan Kosong?
Kosong atau tidaknya HLS **bukan** ditentukan oleh spesifikasi TV Box/TV Android, melainkan oleh **pengaturan teks URL di aplikasi** (`LiveSettings.activeHlsUrl`):
1. **Default bawaan aplikasi memang kosong (`""`)**: `makkahHlsUrl`, `madinahHlsUrl`, dan `customStreamUrl` bernilai kosong hingga Anda mengisinya.
2. **Format URL `custom` tidak valid**: Untuk sumber `custom`, URL dianggap kosong jika tidak diawali `http://`/`https://` atau tidak mengandung `.m3u8` / `/hls/`.
3. **Dinonaktifkan manual**: Toggle **HLS Native** (`preferNativeHls`) dimatikan.

> **Perilaku Otomatis**:
> - Jika `preferNativeHls` aktif **dan** URL `.m3u8` terisi → TV memutar stream menggunakan **Media3 `ExoPlayer`** (ringan untuk STB RAM 2 GB, tanpa iklan/UI browser).
> - Jika URL HLS kosong atau `preferNativeHls` nonaktif → TV otomatis **fallback ke `WebView` YouTube Live** (`makkahVideoId`, `madinahVideoId`, atau `customVideoId`).

### Cara Mengisi Link HLS (`.m3u8`)
1. **Lewat Pengaturan TV**: Buka **Pengaturan → Tab `Acara` → Live Streaming**, aktifkan **Gunakan Player HLS Native**, lalu isi URL `.m3u8`.
2. **Lewat Remote PWA (HP/PC)**:
   - Buka `http://<IP_TV>:1421/remote` → **Tab `Utama`** → kartu **"Preset Live Streaming (YouTube & HLS .m3u8)"**.
   - Isi kolom **Makkah HLS**, **Madinah HLS**, atau **Custom HLS**.
   - Pastikan tombol **📡 HLS NATIVE: AKTIF** menyala, lalu klik **💾 SIMPAN PRESET LIVE & HLS** (atau langsung klik **▶ Putar**).

### Contoh Link Stream `.m3u8` Resmi
- **Makkah Live (Saudi Quran TV / Masjidil Haram)**:
  - HTTPS (Globecast CDN): `https://cdn-globecast.akamaized.net/live/eds/saudi_quran/hls_roku/index.m3u8`
  - HTTP (SBA): `http://m.live.net.sa:1935/live/quran/playlist.m3u8`
- **Madinah Live (Saudi Sunnah TV / Masjid Nabawi)**:
  - HTTPS (Globecast CDN): `https://cdn-globecast.akamaized.net/live/eds/saudi_sunnah/hls_roku/index.m3u8`
  - HTTP (SBA): `http://m.live.net.sa:1935/live/sunnah/playlist.m3u8`

*(Catatan: Link CDN publik penyedia siaran dapat berubah sewaktu-waktu. Jika stream `.m3u8` gagal dimuat, kosongkan kolom HLS agar aplikasi kembali memutar siaran YouTube Live.)*

---

## 4. Ringkasan Dukungan Hardware Relay USB OTG
Aplikasi mendukung kontrol relay 8-channel melalui **WiFi HTTP Relay** maupun **USB Serial OTG**:
- Chip USB-to-Serial yang terdaftar otomatis di `res/xml/usb_device_filter.xml`: **WCH CH340/CH341**, **Silicon Labs CP210x**, **FTDI FT232R/FT231X**, **Prolific PL2303**, dan **Arduino CDC ACM**.
- Jika modul USB dicolok dan belum memiliki izin Android USB Host, aplikasi otomatis memunculkan dialog permintaan izin USB OTG di layar TV melalui `SwitchController.requestUsbRelayPermission()`.

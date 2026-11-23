# SOP Produksi, Provisioning & QC — Paket Bundling STB Al-Gaffar TV

Dokumen ini menjadi panduan standar bagi tim produksi, teknisi instalasi, dan Customer Service (CS) **Al-Gaffar TV** saat menyiapkan unit STB Android TV sebelum dikirim ke masjid pembeli.

---

## 1. Spesifikasi Hardware STB yang Direkomendasikan
- **OS**: Android TV / AOSP Android **9.0 s/d 14** (minSdk 26 / Android 8.0).
- **RAM / Storage**: Minimal **RAM 2 GB / Storage 16 GB** (Chipset Amlogic S905W2 / S905X3 / S905X4 atau Rockchip RK3528).
- **Konektivitas**: WiFi 2.4GHz/5GHz + Port LAN RJ45 + minimal 2 Port USB (1 untuk Flashdisk/Keyboard, 1 untuk Modul USB Relay OTG jika pakai paket IoT).
- **Aksesori Bundling**: Adaptor 5V/2A berkualitas (stabil 24/7), Kabel HDMI 1.5m–3m, Remote TV standar (atau Air Mouse), serta opsional Modul Relay 8-Channel (WiFi ESP32 atau USB Serial CH340).

---

## 2. Persiapan Build Release Produksi (Sebelum Flashing Massal)
1. **Ganti Master Recovery Secret** (WAJIB 1x sebelum produksi massal):
   - Ubah `MASTER_RECOVERY_SECRET` di `app/src/main/java/id/algaffar/jwstv/data/auth/AuthManager.kt` menjadi kunci rahasia unik milik bisnis Anda.
   - Samakan nilai default input `secret` pada `tools/pin_recovery_generator.html` (simpan file HTML ini hanya di laptop/HP internal CS, **jangan pernah dikirim ke pembeli**).
2. **Konfigurasi Endpoint Resmi**:
   - Buka `app/src/main/java/id/algaffar/jwstv/config/AppEndpoints.kt` dan arahkan `OTA_MANIFEST_URL`, `AUDIO_PACKS_MANIFEST_URL`, dan `FINANCE_VIEWER_BASE_URL` ke repositori/domain resmi Al-Gaffar TV Anda.
3. **Siapkan Keystore Release**:
   - Buat file `keystore.properties` di root proyek (sudah masuk `.gitignore`, tidak ikut ter-commit):
     ```properties
     storeFile=keystore/algaffar-tv-release.jks
     storePassword=PASSWORD_KEYSTORE_ANDA
     keyAlias=algaffartv
     keyPassword=PASSWORD_KEY_ANDA
     ```
   - Jalankan `./gradlew assembleRelease` untuk menghasilkan APK rilis terkompresi R8 (`app/build/outputs/apk/release/app-release.apk`).

---

## 3. Alur Flashing & Provisioning per Unit STB (5 Menit / Unit)
1. **Instalasi APK via ADB / Flashdisk**:
   ```bash
   adb install -r app/build/outputs/apk/release/app-release.apk
   ```
2. **Aktifkan Izin Kiosk & Auto-Start Android 10–15**:
   - Beri izin **Tampil di Atas Aplikasi Lain** (*Display over other apps* / `SYSTEM_ALERT_WINDOW`):
     ```bash
     adb shell appops set id.algaffar.jwstv SYSTEM_ALERT_WINDOW allow
     ```
   - Nonaktifkan optimasi baterai agar layanan 24/7 tidak pernah dimatikan OS:
     ```bash
     adb shell dumpsys deviceidle whitelist +id.algaffar.jwstv
     ```
   - (Opsional Kiosk Penuh) Tetapkan **Al-Gaffar TV** sebagai **Default Home Launcher** saat menekan tombol Home di remote STB.
3. **Pre-load Paket Audio Standar** *(Opsional)*:
   - Hubungkan flashdisk master produksi berisi folder `jws-assets/audio/{tartil,tarhim,adzan}` -> jalankan **Sync Massal 1 Folder USB** atau klik **Unduh Paket Audio Standar (1-Klik)**.
4. **Pilihan Mode Serah Terima ke Pembeli**:
   - **Pembeli Setup Mandiri (Default)**: Biarkan aplikasi pada layar **First-Run Onboarding Wizard** agar takmir tinggal memilih Provinsi/Kota, mengetik Nama Masjid, dan membuat PIN baru saat TV dinyalakan di masjid.
   - **Pre-Configured sesuai Pesanan**: Teknisi mengisi Nama Masjid, Alamat, Logo, dan Kota pesanan pembeli di Wizard (atau lewat Remote PWA), lalu menyerahkan PIN awal ke pembeli agar segera diganti setelah diterima.

---

## 4. Checklist Quality Control (QC) Wajib Sebelum Kirim
- [ ] **Boot Auto-Start**: Cabut colokan listrik STB -> colok kembali -> pastikan Al-Gaffar TV otomatis terbuka fullscreen tanpa menyentuh remote.
- [ ] **Uji Suara & Beep**: Jalankan *Uji Bunyi Beep 1000Hz* dan pratinjau audio Tarhim/Adzan lewat speaker TV/HDMI.
- [ ] **Uji Remote PWA HP**: Scan QR Code di menu *Pengaturan → Remote & Jaringan*, pastikan HP langsung terhubung ke port `1421` dan tombol D-Pad virtual bekerja.
- [ ] **Uji No-RTC Clock Guard**: Pastikan jam STB akurat; bila STB tidak punya baterai RTC, catat panduan *"Samakan Jam via Remote HP"* pada kartu panduan dalam kotak.
- [ ] **Uji Relay (Khusus Paket IoT)**: Uji colok modul USB CH340 (klik *Deteksi & Minta Izin USB OTG* lalu tes Pin 1–8) atau uji IP WiFi Relay lokal.
- [ ] **Catat Device ID STB**: Catat 8-karakter **Device ID** unit (tertera di Onboarding & menu Remote) ke buku garansi/database pelanggan untuk layanan CS & pemulihan PIN offline.

---

## 5. SOP Layanan CS: Pemulihan PIN Superadmin Offline (Lupa PIN)
Jika pengurus masjid lupa PIN Superadmin 6 bulan kemudian:
1. Minta pengurus membuka kunci PIN di TV -> klik tombol **"Lupa PIN? (Pemulihan CS)"**.
2. Minta pengurus mengirimkan foto layar TV yang menampilkan:
   - **Device ID STB** (8 karakter)
   - **Kode Tantangan** (6 karakter)
   - **Tanggal TV** (`YYYY-MM-DD`)
3. CS membuka file internal `tools/pin_recovery_generator.html` di HP/laptop CS -> masukkan ketiga data tersebut -> klik **"Hasilkan Kode Pemulihan 6-Digit"**.
4. Kirimkan 6 angka tersebut ke WhatsApp pengurus -> pengurus mengetikkannya di TV -> layar langsung membuka form **Buat PIN Superadmin Baru** tanpa menghapus data masjid (100% offline!).

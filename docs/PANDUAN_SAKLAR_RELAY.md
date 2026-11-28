# Panduan Lengkap Penggunaan & Konektivitas Saklar Relay Otomatis (ESP32 & Wemos D1 Mini)

Dokumen ini menjelaskan cara merakit, mengunggah firmware (`.ino`), memilih mode koneksi (**USB OTG**, **WiFi Online**, atau **WiFi Offline AP**), serta cara mengganti **Nama WiFi & Password Masjid secara dinamis** tanpa perlu mengedit kode program lagi dan **tanpa mematikan fitur YouTube Live / Keuangan Online** di JWS TV Masjid Al-Gaffar.

---

## 1. Pilihan Board & Pemetaan Pin (Wiring)

Aplikasi JWS TV mendukung 3 profil board yang dapat dipilih di menu **Pengaturan -> Acara & Saklar -> Saklar Otomatis (Relay)**:

### A. Profil `ESP32 (8 Pin)`
Menggunakan file sketch: `docs/relay_sketches/esp32_relay_8ch/esp32_relay_8ch.ino`

| Nomor Pin di JWS TV | Perangkat Default | Pin GPIO ESP32 | Keterangan |
| :---: | :--- | :---: | :--- |
| **PIN 1** | LAMPU DALAM | **GPIO 16** | Bebas dari glitch saat boot |
| **PIN 2** | LAMPU LUAR | **GPIO 17** | Bebas dari glitch saat boot |
| **PIN 3** | AC SATU | **GPIO 18** | Bebas dari glitch saat boot |
| **PIN 4** | AC DUA | **GPIO 19** | Bebas dari glitch saat boot |
| **PIN 5** | AC TIGA | **GPIO 21** | Bebas dari glitch saat boot |
| **PIN 6** | AUDIO LUAR (TOA) | **GPIO 22** | Otomatis ON saat Tartil/Tarhim/Adzan |
| **PIN 7** | AUDIO DALAM | **GPIO 23** | Otomatis ON saat Sholat/Khutbah |
| **PIN 8** | SAKLAR 8 (CADANGAN) | **GPIO 25** | Bebas dari glitch saat boot |
| **VCC & GND** | Daya Modul Relay | **5V (VIN) & GND** | Gunakan adaptor 5V 2A jika memakai 8 relay |

### B. Profil `Wemos D1 Mini (4 Pin)` atau `Wemos D1 Mini (2 Pin)`
Menggunakan file sketch: `docs/relay_sketches/wemos_d1_mini_relay/wemos_d1_mini_relay.ino`

| Nomor Pin di JWS TV | Label Pin di Board Wemos | Pin GPIO ESP8266 | Aktif pada Profil |
| :---: | :---: | :---: | :--- |
| **PIN 1** | **D1** | GPIO 5 | Wemos 4 Pin & 2 Pin |
| **PIN 2** | **D2** | GPIO 4 | Wemos 4 Pin & 2 Pin |
| **PIN 3** | **D5** | GPIO 14 | Wemos 4 Pin |
| **PIN 4** | **D6** | GPIO 12 | Wemos 4 Pin |

---

## 2. Perbandingan 3 Mode Konektivitas (Solusi Agar Fitur Live Tetap Jalan)

Firmware v2.0 pada `esp32_relay_8ch.ino` dan `wemos_d1_mini_relay.ino` mendukung **3 metode koneksi sekaligus** tanpa perlu mengunggah ulang kode program:

| Skenario Masjid | Mode yang Dipilih | Cara Kerja | Apakah Fitur YouTube Live & Kas Online Jalan? |
| :--- | :--- | :--- | :---: |
| **1. Modul Relay Dekat dengan TV Box** | **USB OTG Serial** | ESP32/Wemos dicolok dengan kabel data USB langsung ke port USB TV Box. | **YA (100% Jalan)** — WiFi/LAN TV Box bebas dipakai untuk Internet & YouTube Live. |
| **2. Masjid Punya WiFi Router (Ada Internet maupun LAN Lokal)** | **WiFi Mode ONLINE** *(Dual AP+STA)* | ESP32/Wemos ikut tersambung ke WiFi Router Masjid (sama dengan TV Box) dan mengunci IP Statis otomatis di **`192.168.x.250`**. | **YA (100% Jalan)** — TV Box dan Relay berada di WiFi Masjid yang sama. |
| **3. Masjid TIDAK Punya Router WiFi & Relay Jauh dari TV Box** | **WiFi Mode OFFLINE** *(Hotspot AP Mandiri)* | ESP32/Wemos memancarkan WiFi **`JWS_Relay_AP`** (sandi: `algaffar123`) dengan IP tetap **`192.168.4.1`**. | **TETAP BISA JALAN** jika:<br>• TV Box memakai **Kabel LAN** untuk internet dan WiFi untuk Relay, **ATAU**<br>• Nyalakan **Hotspot Portable di TV Box/HP**, lalu atur Relay ke Mode ONLINE agar Relay yang konek ke Hotspot TV Box! |

---

## 3. Cara Mengatur Nama WiFi & Password Masjid Secara Dinamis (Tanpa Laptop / Tanpa Edit Sketch)

Setiap masjid memiliki Nama WiFi (SSID) dan Password yang berbeda-beda. Dengan firmware v2.0, Anda **cukup meng-upload sketch `.ino` satu kali saja saat produksi**. Selanjutnya di lokasi masjid, ada **2 cara mudah** untuk memasukkan WiFi masjid:

### Cara A: Lewat HP Pengurus Masjid (Web Portal `192.168.4.1`)
Saat ESP32 / Wemos menyala, board **selalu** memancarkan Hotspot Setup bernama **`JWS_Relay_AP`**:
1. Buka pengaturan WiFi di HP Anda, sambungkan ke WiFi:
   - **Nama WiFi (SSID)**: `JWS_Relay_AP`
   - **Password**: `algaffar123`
2. Buka browser Chrome/Safari di HP, ketik alamat:
   ```text
   http://192.168.4.1
   ```
3. Pada kartu **"Pengaturan Mode & WiFi Masjid"**:
   - **Pilih Mode**:
     - Pilih **`ONLINE - Gabung ke WiFi Masjid / Hotspot TV Box`** jika di masjid ada WiFi router (agar TV Box tetap bisa memakai fitur YouTube Live).
     - Pilih **`OFFLINE - Hotspot AP Mandiri (192.168.4.1)`** jika masjid sama sekali tidak memiliki router WiFi.
   - **Nama WiFi Masjid (SSID)**: Ketik nama WiFi masjid (contoh: `WiFi_Masjid_AlGaffar`).
   - **Password WiFi Masjid**: Ketik password WiFi masjid.
   - **Kunci Akhiran IP Statis Otomatis**: Biarkan **`250`** (default).
     > **Mengapa angka 250?** Agar ESP32/Wemos otomatis mengunci alamat IP-nya di ujung `.250` sesuai subnet router masjid (misal `192.168.1.250` atau `192.168.100.250`). Dengan demikian, meskipun listrik masjid padam dan menyala lagi, IP Relay tidak akan pernah berubah!
4. Tekan tombol **"Simpan & Hubungkan WiFi"**.
5. Halaman web akan menampilkan status:
   `Status WiFi Masjid: TERHUBUNG (192.168.1.250)`. Catat IP tersebut untuk dimasukkan ke pengaturan JWS TV.

---

### Cara B: Langsung dari Layar Pengaturan JWS TV (Via Kabel USB atau WiFi)
Anda juga bisa mengatur Nama WiFi & Password Relay langsung dari aplikasi **JWS TV**:
1. Colokkan ESP32/Wemos ke port USB TV Box (atau sambungkan WiFi TV Box ke `JWS_Relay_AP`).
2. Buka **Pengaturan JWS TV -> Acara & Saklar -> Saklar Otomatis (Relay) -> ⚙ Konfigurasi Hardware**.
3. Pilih **Mode Koneksi: `WiFi HTTP Relay`**.
4. Pilih **Mode Jaringan WiFi Relay**:
   - **`ONLINE (Gabung WiFi Masjid • Fitur Live Aktif)`**, lalu isi:
     - **Nama WiFi Masjid (SSID)**
     - **Password WiFi Masjid**
     - **Kunci Akhiran IP (`250`)**
   - Atau pilih **`OFFLINE (Hotspot AP Mandiri 192.168.4.1)`**.
5. Klik tombol **"Kirim Pengaturan WiFi ke ESP32/Wemos (via USB/WiFi)"**.
6. Aplikasi JWS TV akan otomatis mengirimkan konfigurasi tersebut ke ESP32/Wemos (lewat kabel USB jika tercolok, atau lewat HTTP jika terhubung via WiFi) dan menyimpannya secara permanen di memori Flash/EEPROM mikrokontroler!

---

## 4. Solusi Khusus: Masjid Tanpa Router WiFi Tapi Ingin Sesekali Memakai Fitur Live (Hotspot HP)

Bagaimana jika masjid sehari-hari **tidak punya WiFi internet** (menggunakan relay secara offline), tetapi pada acara tertentu pengurus ingin menyalakan **YouTube Live / Keuangan Online** menggunakan Hotspot HP?

Gunakan **Trik Nama Hotspot Sama**:
1. Atur ESP32/Wemos ke **Mode ONLINE** dengan:
   - **SSID**: `AlGaffar_Net`
   - **Password**: `masjid12345`
   - **Akhiran IP**: `250`
2. **Untuk pemakaian harian tanpa internet** (jika tidak memakai kabel USB):
   - Aktifkan fitur **Portable Hotspot di TV Box (STB)** dengan nama `AlGaffar_Net` dan sandi `masjid12345`, **ATAU** sambungkan WiFi TV Box ke Hotspot `JWS_Relay_AP` (`192.168.4.1`).
3. **Saat ingin memakai Fitur Live dengan Hotspot HP**:
   - Nyalakan Hotspot di HP pengurus dengan nama `AlGaffar_Net` dan sandi `masjid12345`.
   - Baik TV Box maupun ESP32/Wemos akan otomatis tersambung ke Hotspot HP tersebut! TV Box mendapatkan internet untuk YouTube Live, dan ESP32 tetap bisa dikontrol oleh TV Box.

---

## 5. Cara Menguji & Mengatur Jadwal Otomatis Saklar di JWS TV

Setelah koneksi USB atau WiFi siap:
1. Buka **Pengaturan -> Acara & Saklar -> Saklar Otomatis (Relay)**.
2. Pastikan **"Aktifkan Otomasi Saklar Relay"** dalam posisi **ON**.
3. Pilih salah satu tab perangkat di bagian atas, misalnya **`PIN 1: LAMPU DALAM`** atau **`PIN 6: AUDIO LUAR`**.
4. Tekan tombol **`Test Relay: OFF / ON`**:
   - Jika koneksi benar, lampu indikator pada modul relay akan menyala dan terdengar bunyi *"klik"*.
5. Pilih metode pemicu otomatis:
   - **Berdasarkan Waktu Sholat (`prayer`)**:
     - Contoh untuk **AC / Lampu**: Isi `Menyala Sebelum (Menit) = 30` pada waktu Subuh, dan `Mati Sesudah (Menit) = 60` pada waktu Subuh.
     - Anda juga bisa menyambung lintas waktu sholat: misalnya menyala 30 menit sebelum **Maghrib** (`ON = 30`, `OFF = kosong`) dan baru mati 60 menit sesudah **Isya** (`ON = kosong`, `OFF = 60`).
   - **Berdasarkan Status Aplikasi (`state`)**:
     - Cocok untuk **Amplifier Toa Luar (`PIN 6`)** dan **Audio Dalam (`PIN 7`)**.
     - Contoh Audio Luar: Menyala saat masuk state **`Tartil`** (offset `-1` menit agar ampli siap sebelum suara keluar) dan mati saat masuk state **`Sholat`**.

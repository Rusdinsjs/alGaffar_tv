---
trigger: always_on
description: Konteks proyek dan catatan handover untuk aplikasi JWS TV Masjid Al-Gaffar (Native Android Compose)
---

# Konteks Proyek: JWS TV Masjid Al-Gaffar (`id.algaffar.jwstv`)

Saat bekerja di repositori ini, baca selalu file `AGENTS.md` dan `PRD.md` di root workspace untuk memahami arsitektur lengkap aplikasi:
1. Seluruh **Fase 1 hingga Fase 5** dari spesifikasi rewrite Native Android (Kotlin 2.0 + Jetpack Compose for TV) telah selesai diimplementasikan dan lulus **31 Unit Test** (`./gradlew testDebugUnitTest`) serta berhasil di-build menjadi APK (`./gradlew assembleDebug`).
2. Aplikasi menggunakan kanvas adaptif `1920x1080` (`Responsive1080pCanvas`), mesin state deterministik 27-state (`JwsStateMachine`) dengan *Past-Prayer-Time Recovery*, kalkulasi waktu sholat `Adhan` (`PrayerTimeCalculator`), kalender Hijriah Umm al-Qura +1 hari saat Maghrib (`HijriCalculator`), 17 tema (`ThemeEngine`), 5 jam artistik (`ArtisticClocks`), audio engine `Media3 ExoPlayer` + `1000Hz AudioTrack Beep` (`JwsAudioEngine`, `BeepGenerator`), server remote lokal `Ktor` di port `1421` (`KtorRemoteServer`), kontrol saklar relay WiFi & USB OTG (`SwitchController`), serta backup/restore `.zip` yang kompatibel dengan aplikasi versi lama (Tauri).
3. Pastikan setiap perubahan baru tetap mempertahankan kompatibilitas navigasi Hybrid (**Remote TV D-Pad** + **Touchscreen**) dan lulus unit test.

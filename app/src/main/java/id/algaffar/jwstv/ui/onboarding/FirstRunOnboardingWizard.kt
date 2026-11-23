package id.algaffar.jwstv.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.data.auth.AuthManager
import id.algaffar.jwstv.data.location.CityPreset
import id.algaffar.jwstv.data.location.IndonesiaCityRepository
import id.algaffar.jwstv.domain.engine.PrayerTimeCalculator
import id.algaffar.jwstv.domain.model.AppStateSettings
import id.algaffar.jwstv.domain.model.PrayerId
import id.algaffar.jwstv.domain.model.UserAccount
import id.algaffar.jwstv.ui.JwsRuntimeSnapshot
import id.algaffar.jwstv.ui.screens.standby.generateQrBitmap
import id.algaffar.jwstv.ui.settings.HybridButton
import id.algaffar.jwstv.ui.settings.HybridInfoBanner
import id.algaffar.jwstv.ui.settings.HybridSectionCard
import id.algaffar.jwstv.ui.settings.HybridTextInput
import id.algaffar.jwstv.ui.settings.TvFocusAmber
import id.algaffar.jwstv.ui.theme.LocalJwsTheme
import id.algaffar.jwstv.ui.theme.ThemeEngine

/**
 * 4-Step Hybrid First-Run Onboarding Wizard for Al-Gaffar TV (STB Bundling Edition).
 * - Designed for 100% TV D-Pad remote operation AND simultaneous smartphone QR setup.
 * - Includes Technician / Factory Provisioning skip mode so resellers/installers can pre-configure quickly.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FirstRunOnboardingWizard(
    snapshot: JwsRuntimeSnapshot,
    onTestBeep: () -> Unit,
    onCompleteOnboarding: ((AppStateSettings) -> AppStateSettings) -> Unit
) {
    val context = LocalContext.current
    val palette = LocalJwsTheme.current
    val settings = snapshot.settings

    val allCities = remember(context) { IndonesiaCityRepository.loadCities(context) }
    val provinces = remember(allCities) { IndonesiaCityRepository.getProvinces(allCities) }

    var step by remember { mutableIntStateOf(0) } // 0..3
    var mosqueName by remember { mutableStateOf(if (settings.brand.name == "MASJID ANDA") "" else settings.brand.name) }
    var mosqueAddress by remember {
        mutableStateOf(
            if (settings.brand.address.startsWith("Atur Alamat")) "" else settings.brand.address
        )
    }
    var mosquePhone by remember { mutableStateOf(settings.brand.phone) }

    var selectedProvince by remember {
        mutableStateOf(settings.prayerSettings.provinceName.ifBlank { provinces.firstOrNull() ?: "DKI Jakarta" })
    }
    val citiesInProvince = remember(allCities, selectedProvince) {
        IndonesiaCityRepository.getCitiesInProvince(allCities, selectedProvince)
    }
    var selectedCity by remember {
        mutableStateOf<CityPreset?>(
            allCities.firstOrNull { it.city == settings.prayerSettings.cityName }
                ?: citiesInProvince.firstOrNull()
        )
    }
    var calcMethod by remember { mutableStateOf(settings.prayerSettings.calculationMethod.ifBlank { "Singapore" }) }

    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf("") }

    var selectedThemeId by remember { mutableStateOf(settings.themeSettings.currentThemeId) }
    var selectedClockStyle by remember { mutableStateOf(settings.brand.clockStyle) }

    val qrBitmap = remember(snapshot.remoteQrPairingUrl) {
        if (snapshot.remoteQrPairingUrl.isNotBlank()) generateQrBitmap(snapshot.remoteQrPairingUrl, 340) else null
    }

    val previewSchedule = remember(selectedCity, calcMethod, snapshot.now.toLocalDate()) {
        val city = selectedCity
        if (city != null) {
            PrayerTimeCalculator().calculateSchedule(
                now = snapshot.now,
                settings = settings.prayerSettings.copy(
                    latitude = city.latitude,
                    longitude = city.longitude,
                    timezone = city.timezone,
                    calculationMethod = calcMethod
                )
            )
        } else {
            snapshot.schedule
        }
    }

    fun finishAndSave(skipCustomPin: Boolean) {
        val city = selectedCity
        onCompleteOnboarding { prev ->
            val updatedUsers = if (!skipCustomPin && newPin.length >= 6) {
                val hash = AuthManager.hashPinPbkdf2(newPin)
                val list = prev.users.toMutableList()
                val idx = list.indexOfFirst { it.role.equals("superadmin", ignoreCase = true) }
                if (idx >= 0) {
                    list[idx] = list[idx].copy(pinHash = hash)
                } else {
                    list.add(
                        UserAccount(
                            id = "superadmin-1",
                            username = AuthManager.DEFAULT_SUPERADMIN_USERNAME,
                            pinHash = hash,
                            role = "superadmin"
                        )
                    )
                }
                list
            } else {
                prev.users
            }

            prev.copy(
                onboardingCompleted = true,
                brand = prev.brand.copy(
                    name = mosqueName.trim().ifBlank { prev.brand.name },
                    address = mosqueAddress.trim().ifBlank {
                        city?.let { "${it.city}, ${it.province}" } ?: prev.brand.address
                    },
                    phone = mosquePhone.trim(),
                    clockStyle = selectedClockStyle
                ),
                prayerSettings = if (city != null) {
                    prev.prayerSettings.copy(
                        provinceName = city.province,
                        cityName = city.city,
                        latitude = city.latitude,
                        longitude = city.longitude,
                        timezone = city.timezone,
                        calculationMethod = calcMethod
                    )
                } else {
                    prev.prayerSettings.copy(calculationMethod = calcMethod)
                },
                themeSettings = prev.themeSettings.copy(currentThemeId = selectedThemeId),
                users = updatedUsers
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF050A14), Color(0xFF0A1628), Color(0xFF071A17))
                )
            )
            .padding(36.dp),
        horizontalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        // ==================== LEFT PANEL: 4-STEP WIZARD ====================
        Column(
            modifier = Modifier
                .weight(1.65f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(palette.bgPrimary.copy(alpha = 0.94f))
                .border(2.dp, palette.accent.copy(alpha = 0.55f), RoundedCornerShape(28.dp))
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header + Stepper Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AL-GAFFAR TV — SETUP PERDANA MASJID",
                        color = palette.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.6.sp
                    )
                    Text(
                        text = when (step) {
                            0 -> "Langkah 1/4 — Identitas Masjid"
                            1 -> "Langkah 2/4 — Pilih Provinsi & Kabupaten/Kota"
                            2 -> "Langkah 3/4 — Buat PIN Pengurus DKM Baru"
                            else -> "Langkah 4/4 — Pilih Tema, Jam & Uji Suara"
                        },
                        color = palette.textPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                HybridButton(
                    label = "Mode Teknisi (Lewati)",
                    onClick = { finishAndSave(skipCustomPin = true) }
                )
            }

            // Progress Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf("1. Masjid", "2. Kota Sholat", "3. PIN Admin", "4. Tampilan").forEachIndexed { idx, lbl ->
                    val active = idx == step
                    val done = idx < step
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    active -> palette.accent.copy(alpha = 0.25f)
                                    done -> Color(0xFF10B981).copy(alpha = 0.16f)
                                    else -> palette.bgSecondary
                                }
                            )
                            .border(
                                width = if (active) 2.dp else 1.dp,
                                color = when {
                                    active -> TvFocusAmber
                                    done -> Color(0xFF10B981)
                                    else -> palette.border
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lbl,
                            color = if (active || done) Color.White else palette.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Step Body (Scrollable)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (step) {
                    0 -> {
                        HybridSectionCard(
                            title = "Profil Masjid / Musholla",
                            subtitle = "Nama dan alamat ini akan tampil pada Header Utama, Slide Selamat Datang, serta layar Ibadah"
                        ) {
                            HybridTextInput(
                                label = "Nama Masjid / Musholla",
                                value = mosqueName,
                                placeholder = "Contoh: MASJID JAMI' AL-IKHLAS",
                                onValueChange = { mosqueName = it }
                            )
                            HybridTextInput(
                                label = "Alamat Lengkap Masjid",
                                value = mosqueAddress,
                                placeholder = "Contoh: Jl. Merdeka No. 12, Kec. Sukajadi, Bandung",
                                onValueChange = { mosqueAddress = it }
                            )
                            HybridTextInput(
                                label = "Nomor HP / WhatsApp DKM (Opsional)",
                                value = mosquePhone,
                                placeholder = "Contoh: 0812-xxxx-xxxx",
                                onValueChange = { mosquePhone = it }
                            )
                        }
                        HybridInfoBanner(
                            text = "Tips: Anda juga bisa mengetik lebih cepat dari HP dengan memindai QR Code di sebelah kanan!"
                        )
                    }

                    1 -> {
                        HybridSectionCard(
                            title = "Pilih Provinsi (38 Provinsi Indonesia & ASEAN)",
                            subtitle = "Koordinat dan zona waktu (WIB/WITA/WIT) otomatis terisi tanpa perlu mengetik angka manual"
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                provinces.forEach { prov ->
                                    HybridButton(
                                        label = prov,
                                        isPrimary = (selectedProvince == prov),
                                        onClick = {
                                            selectedProvince = prov
                                            selectedCity = IndonesiaCityRepository.getCitiesInProvince(allCities, prov).firstOrNull()
                                        }
                                    )
                                }
                            }
                        }

                        HybridSectionCard(
                            title = "Pilih Kabupaten / Kota di $selectedProvince",
                            subtitle = selectedCity?.let {
                                "Terpilih: ${it.city} (${it.timezone}) • Lat ${it.latitude}, Lng ${it.longitude}"
                            } ?: "Pilih kota terdekat dari masjid Anda"
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                citiesInProvince.forEach { city ->
                                    HybridButton(
                                        label = city.city,
                                        isPrimary = (selectedCity?.city == city.city),
                                        onClick = { selectedCity = city }
                                    )
                                }
                            }
                        }

                        HybridSectionCard(
                            title = "Metode Hisab & Pratinjau Jadwal Hari Ini",
                            subtitle = "Standar default menggunakan Kemenag RI (Sudut Subuh 20°, Isya 18°)"
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                HybridButton(
                                    label = "Kemenag RI / MUIS (Standar)",
                                    isPrimary = (calcMethod == "Singapore"),
                                    onClick = { calcMethod = "Singapore" }
                                )
                                HybridButton(
                                    label = "Muhammadiyah / MWL (Subuh 18°)",
                                    isPrimary = (calcMethod == "MuslimWorldLeague"),
                                    onClick = { calcMethod = "MuslimWorldLeague" }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PrayerId.FIVE_FARDHU.forEach { pid ->
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(palette.bgPrimary)
                                            .border(1.dp, palette.border, RoundedCornerShape(12.dp))
                                            .padding(vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(pid.defaultIndonesianName, color = palette.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(
                                            previewSchedule.formattedFor(pid),
                                            color = palette.accentSecondary,
                                            fontSize = 20.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        HybridSectionCard(
                            title = "Buat PIN Superadmin Baru (Wajib)",
                            subtitle = "Demi keamanan QRIS Infaq & pengaturan masjid, buat PIN rahasia baru minimal 6 digit angka (jangan gunakan PIN bawaan pabrik)."
                        ) {
                            HybridTextInput(
                                label = "PIN Superadmin Baru (6–8 Digit Angka)",
                                value = newPin,
                                placeholder = "Masukkan 6 digit angka rahasia",
                                onValueChange = {
                                    pinError = ""
                                    newPin = it.filter { c -> c.isDigit() }.take(8)
                                },
                                keyboardType = KeyboardType.Number
                            )
                            HybridTextInput(
                                label = "Konfirmasi PIN Superadmin Baru",
                                value = confirmPin,
                                placeholder = "Ketik ulang PIN baru Anda",
                                onValueChange = {
                                    pinError = ""
                                    confirmPin = it.filter { c -> c.isDigit() }.take(8)
                                },
                                keyboardType = KeyboardType.Number
                            )
                            if (pinError.isNotBlank()) {
                                Text(pinError, color = Color(0xFFF87171), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        HybridInfoBanner(
                            text = "Catat PIN ini baik-baik. Jika suatu hari lupa PIN, tersedia menu 'Lupa PIN (Pemulihan CS)' 100% offline dengan Device ID: ${snapshot.deviceCode}."
                        )
                    }

                    else -> {
                        HybridSectionCard(
                            title = "Pilih Tema Visual Awal",
                            subtitle = "Tersedia 17 tema mewah + auto-rotasi 24 jam yang bisa diubah kapan saja"
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ThemeEngine.BuiltInThemes.take(8).forEach { theme ->
                                    HybridButton(
                                        label = theme.name,
                                        isPrimary = (selectedThemeId == theme.id),
                                        onClick = { selectedThemeId = theme.id }
                                    )
                                }
                            }
                        }

                        HybridSectionCard(
                            title = "Gaya Jam Header & Uji Suara Speaker TV",
                            subtitle = "Pastikan kabel audio HDMI / jack speaker masjid sudah terhubung"
                        ) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf(
                                    "classic" to "Classic",
                                    "golden" to "Golden",
                                    "glass" to "Glass",
                                    "emerald" to "Emerald",
                                    "neon" to "Neon"
                                ).forEach { (id, label) ->
                                    HybridButton(
                                        label = label,
                                        isPrimary = (selectedClockStyle == id),
                                        onClick = { selectedClockStyle = id }
                                    )
                                }
                            }
                            HybridButton(
                                label = "Uji Bunyi Beep 1000Hz Speaker TV",
                                icon = Icons.Default.VolumeUp,
                                onClick = onTestBeep
                            )
                        }
                    }
                }
            }

            // Bottom Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                HybridButton(
                    label = "Sebelumnya",
                    icon = Icons.Default.ArrowBack,
                    enabled = step > 0,
                    onClick = { if (step > 0) step-- }
                )

                if (step < 3) {
                    HybridButton(
                        label = "Lanjut Langkah Berikutnya",
                        icon = Icons.Default.ArrowForward,
                        isPrimary = true,
                        onClick = {
                            if (step == 2) {
                                when {
                                    newPin.length < 6 -> pinError = "PIN Superadmin wajib minimal 6 digit angka."
                                    newPin == AuthManager.DEFAULT_SUPERADMIN_PIN ->
                                        pinError = "Demi keamanan, buat PIN yang berbeda dari PIN bawaan pabrik."
                                    newPin != confirmPin -> pinError = "Konfirmasi PIN tidak sama."
                                    else -> {
                                        pinError = ""
                                        step++
                                    }
                                }
                            } else {
                                step++
                            }
                        }
                    )
                } else {
                    HybridButton(
                        label = "Selesai & Mulai Tayangkan Al-Gaffar TV",
                        icon = Icons.Default.Check,
                        isPrimary = true,
                        onClick = { finishAndSave(skipCustomPin = false) }
                    )
                }
            }
        }

        // ==================== RIGHT PANEL: QR SETUP FROM SMARTPHONE ====================
        Column(
            modifier = Modifier
                .weight(0.75f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF0E1729))
                .border(1.5.dp, palette.border, RoundedCornerShape(28.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = null,
                tint = TvFocusAmber,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "Setup Lebih Cepat dari HP",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Sambungkan HP ke WiFi yang sama dengan STB ini, lalu pindai QR Code berikut untuk mengetik Nama Masjid, Alamat, Logo, dan Jadwal langsung dari layar HP:",
                color = Color(0xFFCBD5E1),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )

            if (qrBitmap != null) {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .border(4.dp, palette.accent, RoundedCornerShape(24.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Setup Al-Gaffar TV",
                        modifier = Modifier.size(224.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF09101E))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("ALAMAT REMOTE LOKAL", color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = snapshot.remoteServerUrl,
                    color = palette.accent,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.12f))
                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("DEVICE ID STB", color = Color(0xFF6EE7B7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = snapshot.deviceCode.ifBlank { "-" },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Simpan kode ini untuk layanan dukungan & pemulihan CS Al-Gaffar TV",
                    color = Color(0xFFA7F3D0),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

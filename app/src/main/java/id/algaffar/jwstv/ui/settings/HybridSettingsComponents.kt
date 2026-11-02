package id.algaffar.jwstv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.algaffar.jwstv.data.auth.AuthManager
import id.algaffar.jwstv.domain.model.UserAccount
import id.algaffar.jwstv.ui.theme.LocalJwsTheme

/**
 * Reusable Hybrid (TV D-Pad Focus + Tablet Touchscreen) Settings UI Primitives.
 */

@Composable
fun HybridSectionCard(
    title: String,
    subtitle: String = "",
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = LocalJwsTheme.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(palette.bgPrimary.copy(alpha = 0.65f))
            .border(1.5.dp, palette.border, RoundedCornerShape(18.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(
                text = title,
                color = palette.accent,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = palette.textSecondary,
                    fontSize = 13.sp
                )
            }
        }
        content()
    }
}

@Composable
fun HybridButton(
    label: String,
    onClick: () -> Unit,
    isPrimary: Boolean = false,
    isDanger: Boolean = false,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    var isFocused by remember { mutableStateOf(false) }

    val bgColor = when {
        isDanger -> if (isFocused) Color(0xFFEF4444) else Color(0xFF991B1B)
        isPrimary -> if (isFocused) palette.accentSecondary else palette.accent
        isFocused -> palette.accent.copy(alpha = 0.35f)
        else -> palette.bgSecondary
    }
    val textColor = when {
        isDanger -> Color.White
        isPrimary -> palette.bgPrimary
        else -> palette.textPrimary
    }
    val borderColor = if (isFocused) palette.accentSecondary else palette.border

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(if (isFocused) 2.5.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun HybridToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isFocused) palette.accent.copy(alpha = 0.18f)
                else palette.bgSecondary.copy(alpha = 0.6f)
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) palette.accentSecondary else palette.border,
                shape = RoundedCornerShape(14.dp)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = palette.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (checked) "AKTIF" else "NONAKTIF",
                color = if (checked) palette.accent else palette.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Icon(
                imageVector = if (checked) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                contentDescription = null,
                tint = if (checked) palette.accent else palette.textSecondary,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
fun HybridNumberStepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    min: Int = -60,
    max: Int = 999,
    step: Int = 1,
    unit: String = "menit",
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    var decFocused by remember { mutableStateOf(false) }
    var incFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSecondary.copy(alpha = 0.6f))
            .border(1.dp, palette.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = palette.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (decFocused) palette.accent else palette.bgPrimary)
                    .border(
                        width = if (decFocused) 2.dp else 1.dp,
                        color = if (decFocused) palette.accentSecondary else palette.border,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .onFocusChanged { decFocused = it.isFocused }
                    .focusable()
                    .clickable {
                        val next = (value - step).coerceAtLeast(min)
                        onValueChange(next)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Kurangi",
                    tint = if (decFocused) palette.bgPrimary else palette.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = "$value $unit",
                color = palette.accentSecondary,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.width(96.dp)
            )

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (incFocused) palette.accent else palette.bgPrimary)
                    .border(
                        width = if (incFocused) 2.dp else 1.dp,
                        color = if (incFocused) palette.accentSecondary else palette.border,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .onFocusChanged { incFocused = it.isFocused }
                    .focusable()
                    .clickable {
                        val next = (value + step).coerceAtMost(max)
                        onValueChange(next)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah",
                    tint = if (incFocused) palette.bgPrimary else palette.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun HybridTextInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier
) {
    val palette = LocalJwsTheme.current
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = palette.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = palette.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            ),
            cursorBrush = SolidColor(palette.accent),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.bgSecondary)
                .border(
                    width = if (isFocused) 2.dp else 1.dp,
                    color = if (isFocused) palette.accent else palette.border,
                    shape = RoundedCornerShape(12.dp)
                )
                .onFocusChanged { isFocused = it.isFocused }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(
                            text = placeholder,
                            color = palette.textSecondary.copy(alpha = 0.45f),
                            fontSize = 15.sp
                        )
                    }
                    innerTextField()
                }
            }
        )
    }
}

/**
 * Multi-Role PIN Login Guard with On-Screen TV D-Pad & Touch Numeric Keypad.
 * Default Superadmin: username `admin`, PIN `732911`.
 */
@Composable
fun PinLoginGuardScreen(
    users: List<UserAccount>,
    onAuthenticated: (UserAccount) -> Unit,
    onCancel: () -> Unit
) {
    val palette = LocalJwsTheme.current
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    fun attemptLogin(candidatePin: String) {
        val matched = AuthManager.authenticate(
            username = "",
            rawPin = candidatePin,
            users = users
        )
        if (matched != null) {
            onAuthenticated(matched)
        } else {
            errorMessage = "PIN tidak sesuai (Default Superadmin: 732911)"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(520.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(palette.bgPrimary)
                .border(2.dp, palette.accent, RoundedCornerShape(24.dp))
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = palette.accentSecondary,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = "Autentikasi PIN Pengurus DKM",
                color = palette.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Masukkan 6 digit PIN menggunakan Remote TV (D-Pad) atau sentuh tombol angka",
                color = palette.textSecondary,
                fontSize = 14.sp
            )

            // Masked PIN Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.bgSecondary)
                    .border(1.5.dp, palette.accent, RoundedCornerShape(14.dp))
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (pinInput.isEmpty()) "• • • • • •" else "• ".repeat(pinInput.length).trim(),
                    color = palette.accentSecondary,
                    fontSize = 30.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFF87171),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // 3x4 On-Screen Numeric Keypad (D-Pad & Touch friendly)
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("HAPUS", "0", "MASUK")
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        r.forEach { key ->
                            HybridButton(
                                label = key,
                                isPrimary = (key == "MASUK"),
                                isDanger = (key == "HAPUS"),
                                onClick = {
                                    errorMessage = ""
                                    when (key) {
                                        "HAPUS" -> {
                                            if (pinInput.isNotEmpty()) {
                                                pinInput = pinInput.dropLast(1)
                                            }
                                        }
                                        "MASUK" -> attemptLogin(pinInput)
                                        else -> {
                                            if (pinInput.length < 8) {
                                                pinInput += key
                                                if (pinInput.length == 6) {
                                                    val autoCheck = AuthManager.authenticate("", pinInput, users)
                                                    if (autoCheck != null) {
                                                        onAuthenticated(autoCheck)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HybridButton(
                    label = "Login Cepat Default (732911)",
                    onClick = { attemptLogin(AuthManager.DEFAULT_SUPERADMIN_PIN) }
                )
                HybridButton(
                    label = "Batal / Tutup",
                    onClick = onCancel
                )
            }
        }
    }
}

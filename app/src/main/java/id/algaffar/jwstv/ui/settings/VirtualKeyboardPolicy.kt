package id.algaffar.jwstv.ui.settings

import android.content.Context
import android.content.res.Configuration
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.InterceptPlatformTextInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.awaitCancellation

/**
 * Policy controlling whether the Android on-screen Virtual Keyboard (IME) is permitted to appear.
 *
 * Modes:
 * - `"auto"` (default): Automatically disables virtual keyboard whenever a physical PC/USB/Bluetooth
 *   QWERTY keyboard is detected, and enables it when operating with TV Remote / Touchscreen only.
 * - `"enabled"`: Always allows the virtual keyboard to appear.
 * - `"disabled"`: Always suppresses the virtual keyboard (physical keyboard / Remote PWA typing only).
 */
data class VirtualKeyboardPolicy(
    val mode: String = MODE_AUTO,
    val hasPhysicalKeyboard: Boolean = false,
    val detectedKeyboardNames: List<String> = emptyList()
) {
    val normalizedMode: String
        get() = when (mode.lowercase()) {
            MODE_ENABLED -> MODE_ENABLED
            MODE_DISABLED -> MODE_DISABLED
            else -> MODE_AUTO
        }

    val allowVirtualKeyboard: Boolean
        get() = when (normalizedMode) {
            MODE_ENABLED -> true
            MODE_DISABLED -> false
            else -> !hasPhysicalKeyboard
        }

    companion object {
        const val MODE_AUTO = "auto"
        const val MODE_ENABLED = "enabled"
        const val MODE_DISABLED = "disabled"
    }
}

val LocalVirtualKeyboardPolicy = staticCompositionLocalOf { VirtualKeyboardPolicy() }

/**
 * Scans connected Android [InputDevice]s and [Configuration] for physical alphabetic/PC keyboards,
 * filtering out TV remotes (NON_ALPHABETIC) and virtual keyboards.
 */
fun detectPhysicalKeyboards(context: Context): List<String> {
    val names = linkedSetOf<String>()
    runCatching {
        for (id in InputDevice.getDeviceIds()) {
            val device = InputDevice.getDevice(id) ?: continue
            if (device.isVirtual) continue
            val isKeyboardSource = (device.sources and InputDevice.SOURCE_KEYBOARD) == InputDevice.SOURCE_KEYBOARD
            val isAlphabetic = device.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC
            if (isKeyboardSource && isAlphabetic) {
                val label = device.name?.trim().orEmpty().ifBlank { "Keyboard Fisik #$id" }
                names.add(label)
            }
        }
    }
    if (names.isEmpty()) {
        val cfg = context.resources.configuration
        val hasHardQwerty = (cfg.keyboard == Configuration.KEYBOARD_QWERTY ||
            cfg.keyboard == Configuration.KEYBOARD_12KEY) &&
            cfg.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO
        if (hasHardQwerty) {
            names.add("Hardware QWERTY Keyboard")
        }
    }
    return names.toList()
}

/**
 * Observes physical keyboard attach/detach events live via [InputManager.InputDeviceListener]
 * and merges with live hardware key activity detected by the host Activity.
 */
@Composable
fun rememberPhysicalKeyboardNames(livePhysicalKeyDetected: Boolean): List<String> {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    var scannedNames by remember(context, configuration.keyboard, configuration.hardKeyboardHidden) {
        mutableStateOf(detectPhysicalKeyboards(context))
    }

    DisposableEffect(context) {
        val inputManager = context.getSystemService(Context.INPUT_SERVICE) as? InputManager
        val listener = object : InputManager.InputDeviceListener {
            override fun onInputDeviceAdded(deviceId: Int) {
                scannedNames = detectPhysicalKeyboards(context)
            }

            override fun onInputDeviceRemoved(deviceId: Int) {
                scannedNames = detectPhysicalKeyboards(context)
            }

            override fun onInputDeviceChanged(deviceId: Int) {
                scannedNames = detectPhysicalKeyboards(context)
            }
        }
        runCatching {
            inputManager?.registerInputDeviceListener(listener, Handler(Looper.getMainLooper()))
        }
        onDispose {
            runCatching { inputManager?.unregisterInputDeviceListener(listener) }
        }
    }

    return remember(scannedNames, livePhysicalKeyDetected) {
        if (scannedNames.isNotEmpty()) {
            scannedNames
        } else if (livePhysicalKeyDetected) {
            listOf("Keyboard PC / Fisik (Terdeteksi dari Input)")
        } else {
            emptyList()
        }
    }
}

/**
 * Suppresses Android soft keyboard (IME) launch when [allowVirtualKeyboard] is false while keeping
 * Compose [androidx.compose.foundation.text.BasicTextField] focus, cursor, and physical PC keyboard
 * key input 100% functional.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VirtualKeyboardGuard(
    allowVirtualKeyboard: Boolean = LocalVirtualKeyboardPolicy.current.allowVirtualKeyboard,
    content: @Composable () -> Unit
) {
    if (allowVirtualKeyboard) {
        content()
    } else {
        InterceptPlatformTextInput(
            interceptor = { _, _ -> awaitCancellation() },
            content = content
        )
    }
}

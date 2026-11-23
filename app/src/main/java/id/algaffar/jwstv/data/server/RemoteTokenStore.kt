package id.algaffar.jwstv.data.server

import android.content.Context
import android.provider.Settings
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Persists security credentials for the local Ktor Remote Server and STB Device ID:
 * - 32-character cryptographically random Bearer token (survives TV reboots, can be regenerated on demand)
 * - Stable 8-character Device ID (used for Offline Challenge-Response PIN Recovery & future licensing)
 */
class RemoteTokenStore(
    private val storageDir: File,
    private val context: Context? = null
) {
    private val tokenFile = File(storageDir, ".jws_remote_token")
    private val deviceIdFile = File(storageDir, ".jws_device_code")
    private val secureRandom = SecureRandom()

    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var cachedDeviceCode: String? = null

    @Synchronized
    fun getOrCreateRemoteToken(): String {
        cachedToken?.let { if (it.length >= MIN_TOKEN_LENGTH) return it }
        val existing = runCatching {
            if (tokenFile.exists()) tokenFile.readText(Charsets.UTF_8).trim() else ""
        }.getOrDefault("")

        if (existing.length >= MIN_TOKEN_LENGTH && existing.all { it in TOKEN_ALPHABET }) {
            cachedToken = existing
            return existing
        }
        return regenerateRemoteToken()
    }

    @Synchronized
    fun regenerateRemoteToken(): String {
        val fresh = generateSecureToken(DEFAULT_TOKEN_LENGTH)
        writePrivateText(tokenFile, fresh)
        cachedToken = fresh
        return fresh
    }

    @Synchronized
    fun getOrCreateDeviceCode(): String {
        cachedDeviceCode?.let { if (it.length == DEVICE_CODE_LENGTH) return it }
        val existing = runCatching {
            if (deviceIdFile.exists()) deviceIdFile.readText(Charsets.UTF_8).trim().uppercase() else ""
        }.getOrDefault("")
        if (existing.length == DEVICE_CODE_LENGTH && existing.all { it in DEVICE_CODE_ALPHABET }) {
            cachedDeviceCode = existing
            return existing
        }

        val androidId = runCatching {
            context?.let { Settings.Secure.getString(it.contentResolver, Settings.Secure.ANDROID_ID) }
        }.getOrNull().orEmpty().trim()

        val generated = if (androidId.isNotBlank() && androidId != "9774d56d682e549c") {
            deriveDeviceCodeFromSeed("ALGAFFAR-STB|$androidId")
        } else {
            generateFromAlphabet(DEVICE_CODE_ALPHABET, DEVICE_CODE_LENGTH)
        }
        writePrivateText(deviceIdFile, generated)
        cachedDeviceCode = generated
        return generated
    }

    private fun deriveDeviceCodeFromSeed(seed: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(seed.toByteArray(Charsets.UTF_8))
        return buildString(DEVICE_CODE_LENGTH) {
            for (i in 0 until DEVICE_CODE_LENGTH) {
                val idx = (digest[i].toInt() and 0xFF) % DEVICE_CODE_ALPHABET.length
                append(DEVICE_CODE_ALPHABET[idx])
            }
        }
    }

    private fun generateFromAlphabet(alphabet: String, length: Int): String {
        return buildString(length) {
            repeat(length) {
                append(alphabet[secureRandom.nextInt(alphabet.length)])
            }
        }
    }

    private fun writePrivateText(file: File, value: String) {
        runCatching {
            storageDir.mkdirs()
            val tmp = File(storageDir, "${file.name}.tmp")
            tmp.writeText(value, Charsets.UTF_8)
            tmp.setReadable(false, false)
            tmp.setReadable(true, true)
            tmp.setWritable(false, false)
            tmp.setWritable(true, true)
            if (!tmp.renameTo(file)) {
                file.writeText(value, Charsets.UTF_8)
                tmp.delete()
            }
            file.setReadable(false, false)
            file.setReadable(true, true)
        }
    }

    companion object {
        const val DEFAULT_TOKEN_LENGTH = 32
        const val MIN_TOKEN_LENGTH = 16
        const val DEVICE_CODE_LENGTH = 8
        private const val TOKEN_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789"
        private const val DEVICE_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        fun generateSecureToken(length: Int = DEFAULT_TOKEN_LENGTH): String {
            val random = SecureRandom()
            return buildString(length) {
                repeat(length) {
                    append(TOKEN_ALPHABET[random.nextInt(TOKEN_ALPHABET.length)])
                }
            }
        }
    }
}

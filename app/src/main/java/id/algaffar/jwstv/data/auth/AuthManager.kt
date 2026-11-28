package id.algaffar.jwstv.data.auth

import id.algaffar.jwstv.domain.model.UserAccount
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.crypto.Mac
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

enum class UserRole(val id: String, val displayLabel: String) {
    SUPERADMIN("superadmin", "Super Admin (Akses Penuh)"),
    ADMIN("admin", "Admin DKM"),
    USER("user", "Operator Harian");

    companion object {
        fun fromId(id: String?): UserRole = entries.firstOrNull {
            it.id.equals(id?.trim(), ignoreCase = true)
        } ?: USER
    }
}

/**
 * Tracks failed PIN login attempts and enforces a temporary cooldown lockout
 * to protect the mosque display from brute-force attempts.
 */
class PinLockoutTracker(
    val maxFailedAttempts: Int = 5,
    val lockoutDurationMs: Long = 60_000L
) {
    private var failedAttempts: Int = 0
    private var lockedUntilMs: Long = 0L

    @Synchronized
    fun remainingLockoutSeconds(nowMs: Long = System.currentTimeMillis()): Long {
        if (lockedUntilMs <= nowMs) {
            if (lockedUntilMs != 0L) {
                lockedUntilMs = 0L
                failedAttempts = 0
            }
            return 0L
        }
        return ((lockedUntilMs - nowMs) + 999L) / 1000L
    }

    @Synchronized
    fun isLocked(nowMs: Long = System.currentTimeMillis()): Boolean =
        remainingLockoutSeconds(nowMs) > 0L

    @Synchronized
    fun recordFailure(nowMs: Long = System.currentTimeMillis()): Int {
        if (lockedUntilMs > nowMs) return 0
        failedAttempts += 1
        if (failedAttempts >= maxFailedAttempts) {
            lockedUntilMs = nowMs + lockoutDurationMs
            return 0
        }
        return (maxFailedAttempts - failedAttempts).coerceAtLeast(0)
    }

    @Synchronized
    fun recordSuccess() {
        failedAttempts = 0
        lockedUntilMs = 0L
    }

    @Synchronized
    fun currentFailedAttempts(): Int = failedAttempts
}

/**
 * Offline Challenge-Response PIN Recovery for CS Al-Gaffar TV.
 * Allows mosque caretakers who forgot their Superadmin PIN to reset it 100% offline
 * by reading a 6-character Challenge Code + 8-character Device ID to CS WhatsApp.
 */
object OfflinePinRecoveryEngine {

    /**
     * Master HMAC key shared with `tools/pin_recovery_generator.html`.
     * Change this value before commercial mass-production builds.
     */
    internal const val MASTER_RECOVERY_SECRET = "ALGAFFAR-TV-RECOVERY-V1-9F4C2E8A71B0"

    private const val CHALLENGE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val secureRandom = SecureRandom()

    fun generateChallengeCode(length: Int = 6): String {
        return buildString(length) {
            repeat(length) {
                append(CHALLENGE_ALPHABET[secureRandom.nextInt(CHALLENGE_ALPHABET.length)])
            }
        }
    }

    fun computeRecoveryCode(
        deviceCode: String,
        challengeCode: String,
        date: LocalDate = LocalDate.now(),
        secret: String = MASTER_RECOVERY_SECRET
    ): String {
        val canonicalPayload = "${deviceCode.trim().uppercase()}|${challengeCode.trim().uppercase()}|${date.format(DATE_FMT)}"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val digest = mac.doFinal(canonicalPayload.toByteArray(Charsets.UTF_8))
        val value = ((digest[0].toInt() and 0x7F) shl 24) or
            ((digest[1].toInt() and 0xFF) shl 16) or
            ((digest[2].toInt() and 0xFF) shl 8) or
            (digest[3].toInt() and 0xFF)
        return "%06d".format(value % 1_000_000)
    }

    fun verifyRecoveryCode(
        deviceCode: String,
        challengeCode: String,
        inputCode: String,
        date: LocalDate = LocalDate.now(),
        secret: String = MASTER_RECOVERY_SECRET
    ): Boolean {
        val cleaned = inputCode.trim()
        if (cleaned.length != 6 || !cleaned.all { it.isDigit() }) return false
        val expectedToday = computeRecoveryCode(deviceCode, challengeCode, date, secret)
        if (MessageDigest.isEqual(cleaned.toByteArray(Charsets.UTF_8), expectedToday.toByteArray(Charsets.UTF_8))) {
            return true
        }
        // Allow +/- 1 day tolerance in case STB clock is slightly shifted across midnight
        val expectedYesterday = computeRecoveryCode(deviceCode, challengeCode, date.minusDays(1), secret)
        if (MessageDigest.isEqual(cleaned.toByteArray(Charsets.UTF_8), expectedYesterday.toByteArray(Charsets.UTF_8))) {
            return true
        }
        val expectedTomorrow = computeRecoveryCode(deviceCode, challengeCode, date.plusDays(1), secret)
        return MessageDigest.isEqual(cleaned.toByteArray(Charsets.UTF_8), expectedTomorrow.toByteArray(Charsets.UTF_8))
    }
}

/**
 * Multi-role PIN authentication manager (`superadmin`, `admin`, `user`).
 * Uses salted PBKDF2-HMAC-SHA256 for all new/updated PINs while supporting transparent
 * one-way verification & upgrade from legacy SHA-256 hashes.
 */
object AuthManager {

    const val DEFAULT_SUPERADMIN_USERNAME = "admin"
    const val DEFAULT_SUPERADMIN_PIN = "732911"

    private const val PBKDF2_PREFIX = "pbkdf2"
    private const val PBKDF2_ITERATIONS = 65_536
    private const val PBKDF2_KEY_BITS = 256
    private const val SALT_BYTES = 16
    private val secureRandom = SecureRandom()

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.trim().toByteArray(Charsets.UTF_8))
        return bytes.toHexString()
    }

    /**
     * Hashes a raw PIN with a unique 16-byte random salt using PBKDF2WithHmacSHA256.
     * Stored format: `pbkdf2$<iterations>$<saltHex>$<hashHex>`.
     */
    fun hashPinPbkdf2(rawPin: String, iterations: Int = PBKDF2_ITERATIONS): String {
        val clean = rawPin.trim()
        val salt = ByteArray(SALT_BYTES)
        secureRandom.nextBytes(salt)
        val derived = derivePbkdf2(clean, salt, iterations)
        return "$PBKDF2_PREFIX$$iterations$${salt.toHexString()}$${derived.toHexString()}"
    }

    fun needsHashUpgrade(storedHash: String): Boolean =
        !storedHash.trim().startsWith("$PBKDF2_PREFIX$")

    fun verifyPin(rawPin: String, storedHash: String): Boolean {
        val cleanPin = rawPin.trim()
        val cleanStored = storedHash.trim()
        if (cleanPin.isEmpty() || cleanStored.isEmpty()) return false

        if (cleanStored.startsWith("$PBKDF2_PREFIX$")) {
            val parts = cleanStored.split('$')
            if (parts.size != 4) return false
            val iterations = parts[1].toIntOrNull()?.coerceIn(10_000, 300_000) ?: return false
            val salt = hexToBytes(parts[2]) ?: return false
            val expectedHash = hexToBytes(parts[3]) ?: return false
            val actualHash = derivePbkdf2(cleanPin, salt, iterations)
            return MessageDigest.isEqual(actualHash, expectedHash)
        }

        // Legacy 64-char SHA-256 hex compatibility
        if (cleanStored.length == 64 && cleanStored.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
            val hashedInput = sha256Hex(cleanPin)
            return MessageDigest.isEqual(
                hashedInput.lowercase().toByteArray(Charsets.UTF_8),
                cleanStored.lowercase().toByteArray(Charsets.UTF_8)
            )
        }

        // Initial pre-migration plain default PIN compatibility (upgraded on first login)
        return MessageDigest.isEqual(
            cleanPin.toByteArray(Charsets.UTF_8),
            cleanStored.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * Authenticates a user by [username] and [rawPin] against the configured [users] list.
     * If [username] is blank, checks if [rawPin] matches any account (for fast TV D-Pad PIN keypad login).
     */
    fun authenticate(
        username: String,
        rawPin: String,
        users: List<UserAccount>
    ): UserAccount? {
        val cleanPin = rawPin.trim()
        if (cleanPin.isEmpty()) return null

        val effectiveUsers = users.ifEmpty {
            listOf(
                UserAccount(
                    id = "superadmin-1",
                    username = DEFAULT_SUPERADMIN_USERNAME,
                    pinHash = sha256Hex(DEFAULT_SUPERADMIN_PIN),
                    role = UserRole.SUPERADMIN.id
                )
            )
        }

        val cleanUser = username.trim()
        return effectiveUsers.firstOrNull { account ->
            val usernameMatches = cleanUser.isEmpty() ||
                account.username.equals(cleanUser, ignoreCase = true)
            usernameMatches && verifyPin(cleanPin, account.pinHash)
        }
    }

    private fun derivePbkdf2(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, PBKDF2_KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray? {
        if (hex.length % 2 != 0) return null
        return runCatching {
            ByteArray(hex.length / 2) { idx ->
                hex.substring(idx * 2, idx * 2 + 2).toInt(16).toByte()
            }
        }.getOrNull()
    }

    private val RESTRICTED_FOR_USER = setOf(
        "users_security",
        "userManagement",
        "audio_hardware",
        "prayer",
        "prayerLocation",
        "prayerStages",
        "audio",
        "customAudio",
        "state",
        "switches",
        "network",
        "backup",
        "backupUpdate",
        "logs",
        "logsDevice",
        "hardware"
    )

    fun canAccessTab(role: UserRole, tabCategory: String): Boolean {
        return when (role) {
            UserRole.SUPERADMIN -> true
            UserRole.ADMIN -> tabCategory != "users_security" && tabCategory != "userManagement"
            UserRole.USER -> tabCategory !in RESTRICTED_FOR_USER
        }
    }
}

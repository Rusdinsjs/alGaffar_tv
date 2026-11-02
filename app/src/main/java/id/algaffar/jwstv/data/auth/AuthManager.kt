package id.algaffar.jwstv.data.auth

import id.algaffar.jwstv.domain.model.UserAccount
import java.security.MessageDigest

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
 * Multi-role PIN authentication manager (`superadmin`, `admin`, `user`).
 * Supports SHA-256 hashed PINs as well as transparent verification of legacy/default plain PINs
 * (such as the default superadmin `admin` / `732911`).
 * Mirrors `src/contexts/AuthContext.tsx` and `src/types/authTypes.ts`.
 */
object AuthManager {

    const val DEFAULT_SUPERADMIN_USERNAME = "admin"
    const val DEFAULT_SUPERADMIN_PIN = "732911"

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.trim().toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Authenticates a user by [username] and [rawPin] against the configured [users] list.
     * If [username] is blank, checks if [rawPin] uniquely matches any account (for fast TV D-Pad PIN keypad login).
     */
    fun authenticate(
        username: String,
        rawPin: String,
        users: List<UserAccount>
    ): UserAccount? {
        val cleanPin = rawPin.trim()
        if (cleanPin.isEmpty()) return null
        val hashedInput = sha256Hex(cleanPin)

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
            val pinMatches = account.pinHash.equals(hashedInput, ignoreCase = true) ||
                account.pinHash == cleanPin
            usernameMatches && pinMatches
        }
    }

    fun canAccessTab(role: UserRole, tabCategory: String): Boolean {
        return when (role) {
            UserRole.SUPERADMIN -> true
            UserRole.ADMIN -> tabCategory != "users_security"
            UserRole.USER -> tabCategory in setOf(
                "brand",
                "jumat",
                "ramadhan",
                "holiday_qurban",
                "slides_overlay"
            )
        }
    }
}

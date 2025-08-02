package io.github.toberocat.improvedfactions.modules.streamchat.auth

import java.security.SecureRandom
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class TokenManager {
    private val tokens = ConcurrentHashMap<String, TokenInfo>()
    private val random = SecureRandom()
    private val TOKEN_EXPIRY = 24 * 60 * 60 * 1000L  // 24 horas en milisegundos

    data class TokenInfo(
        val userId: String,
        val createdAt: Long,
        val expiresAt: Long
    )

    fun generateToken(userId: String): String {
        val tokenBytes = ByteArray(32)
        random.nextBytes(tokenBytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes)

        val now = System.currentTimeMillis()
        tokens[token] = TokenInfo(
            userId = userId,
            createdAt = now,
            expiresAt = now + TOKEN_EXPIRY
        )

        return token
    }

    fun validateToken(token: String): Boolean {
        val tokenInfo = tokens[token] ?: return false
        val now = System.currentTimeMillis()

        if (now > tokenInfo.expiresAt) {
            tokens.remove(token)
            return false
        }

        return true
    }

    fun getUserIdFromToken(token: String): String? {
        return tokens[token]?.userId
    }

    fun cleanExpiredTokens() {
        val now = System.currentTimeMillis()
        tokens.entries.removeIf { it.value.expiresAt < now }
    }
}
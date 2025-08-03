
package io.github.toberocat.improvedfactions.modules.streamchat.message

import com.google.gson.Gson
import java.time.Instant
import java.time.format.DateTimeFormatter

data class ChatMessage(
    val username: String,
    val userColor: String = "#FFFFFF",
    val badges: List<String> = emptyList(),
    val message: String,
    val time: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
    val type: String = "message"
) {
    companion object {
        private val gson = Gson()
        private val emotePattern = Regex("\\[emote:\\d*:.*?\\]|\\[emote:\\*:\\*\\]")

        fun fromJson(json: String): ChatMessage? = try {
            gson.fromJson(json, ChatMessage::class.java)?.let { chatMessage ->
                chatMessage.copy(message = cleanMessage(chatMessage.message))
            }
        } catch (e: Exception) {
            null
        }

        private fun cleanMessage(message: String): String {
            return message.replace(emotePattern, "").trim()
        }
    }

    fun toJson(): String = gson.toJson(this)

    // Función para obtener el mensaje limpio sin crear un nuevo objeto
    fun getCleanMessage(): String = message.replace(emotePattern, "").trim()
}
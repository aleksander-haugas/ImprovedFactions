package io.github.toberocat.improvedfactions.modules.protection.notification

import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class NotificationPreferences {
    private val fireNotificationsEnabled = ConcurrentHashMap<UUID, Boolean>()

    fun toggleFireNotifications(player: Player): Boolean {
        val current = !isFireNotificationsEnabled(player)
        fireNotificationsEnabled[player.uniqueId] = current
        return current
    }

    fun isFireNotificationsEnabled(player: Player): Boolean {
        return fireNotificationsEnabled.getOrDefault(player.uniqueId, true) // Activado por defecto
    }

    fun removePlayer(player: Player) {
        fireNotificationsEnabled.remove(player.uniqueId)
    }
}
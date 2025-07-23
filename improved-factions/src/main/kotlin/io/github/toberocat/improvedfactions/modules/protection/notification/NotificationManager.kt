package io.github.toberocat.improvedfactions.modules.protection.notification

import io.github.toberocat.improvedfactions.factions.Faction
import org.bukkit.ChatColor
import org.bukkit.Location

class NotificationManager {
    fun notifyExplosionBlocked(faction: Faction, explosionType: String, location: Location) {
        val message = StringBuilder().apply {
            append("${ChatColor.RED}[Explosion Blocked] ")
            append("${ChatColor.YELLOW}Type: $explosionType ")
            append("${ChatColor.GRAY}at X: ${location.blockX}, Y: ${location.blockY}, Z: ${location.blockZ} ")
            append("${ChatColor.GRAY}in ${location.world?.name}")
        }.toString()

        notifyFactionAdmins(faction, message)
    }

    fun notifyLockdownViolation(faction: Faction, details: String) {
        faction.members().forEach { member ->
            member.player()?.sendMessage("${ChatColor.RED}¡Violación de lockdown detectada! $details")
        }
    }

    private fun notifyFactionAdmins(faction: Faction, message: String) {
        faction.members().forEach { member ->
            if (member.uniqueId == faction.owner || member.hasPermission("factions.admin")) {
                member.player()?.sendMessage(message)
            }
        }
    }

    fun notifyFactionMembers(faction: Faction, eventType: String, details: String, message: String) {
        faction.members().forEach { member ->
            if (member.uniqueId == faction.members() || member.hasPermission("factions.members")) {
                member.player()?.sendMessage(message)
            }
        }
    }
}
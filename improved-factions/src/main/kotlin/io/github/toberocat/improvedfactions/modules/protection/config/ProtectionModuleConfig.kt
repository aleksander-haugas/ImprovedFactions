
package io.github.toberocat.improvedfactions.modules.protection.config

import org.bukkit.configuration.file.FileConfiguration

class ProtectionModuleConfig (
    var preventCreeperGriefingInClaims: Boolean = true,
    var preventTntGriefingInClaims: Boolean = true,
    var preventCreeperGriefingInWarzone: Boolean = true,
    var preventTntGriefingInWarzone: Boolean = true,
    // Nuevas configuraciones para el lockdown
    var lockdownSupervisionPeriod: Long = 3600, // 1 hora en segundos
    var lockdownAllowedDurations: List<Long> = listOf(
        3600,      // 1 hora
        10800,     // 3 horas
        43200,     // 12 horas
        86400,     // 1 día
        259200,    // 3 días
        604800     // 7 días
    )
){
    private val configPath = "factions.protection"

    fun reload(config: FileConfiguration) {
        preventCreeperGriefingInClaims = config.getBoolean("$configPath.prevent-creeper-griefing-in-claims", true)
        preventTntGriefingInClaims = config.getBoolean("$configPath.prevent-tnt-griefing-in-claims", true)
        preventCreeperGriefingInWarzone = config.getBoolean("$configPath.prevent-creeper-griefing-in-warzone", true)
        preventTntGriefingInWarzone = config.getBoolean("$configPath.prevent-tnt-griefing-in-warzone", true)
        lockdownSupervisionPeriod = config.getLong("$configPath.lockdown-supervision-period", 3600)
        lockdownAllowedDurations = config.getLongList("$configPath.lockdown-allowed-durations")
            ?: listOf(3600, 10800, 43200, 86400, 259200, 604800)
    }
}
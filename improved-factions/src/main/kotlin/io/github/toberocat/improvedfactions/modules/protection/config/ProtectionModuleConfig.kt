
package io.github.toberocat.improvedfactions.modules.protection.config

import org.bukkit.configuration.file.FileConfiguration

class ProtectionModuleConfig(
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
) {
    private val configPath = "factions.protection-settings"

    fun reload(config: FileConfiguration) {
        preventCreeperGriefingInClaims = config.getBoolean("$configPath.prevent-creeper-griefing-in-claims", preventCreeperGriefingInClaims)
        preventTntGriefingInClaims = config.getBoolean("$configPath.prevent-tnt-griefing-in-claims", preventTntGriefingInClaims)
        preventCreeperGriefingInWarzone = config.getBoolean("$configPath.prevent-creeper-griefing-in-warzone", preventCreeperGriefingInWarzone)
        preventTntGriefingInWarzone = config.getBoolean("$configPath.prevent-tnt-griefing-in-warzone", preventTntGriefingInWarzone)

        // Usar el valor de la configuración o el valor por defecto si no está definido
        lockdownSupervisionPeriod = config.getLong("$configPath.lockdown-supervision-period", lockdownSupervisionPeriod)

        // Para las duraciones permitidas, usar la lista de la configuración o la lista por defecto si está vacía
        val configDurations = config.getLongList("$configPath.lockdown-allowed-durations")
        lockdownAllowedDurations = if (configDurations.isEmpty()) {
            listOf(
                3600,      // 1 hora
                10800,     // 3 horas
                43200,     // 12 horas
                86400,     // 1 día
                259200,    // 3 días
                604800     // 7 días
            )
        } else {
            configDurations
        }
    }
}
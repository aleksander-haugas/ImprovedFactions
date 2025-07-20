package io.github.toberocat.improvedfactions.modules.protection.config
import org.bukkit.configuration.file.FileConfiguration

// --- CLASE DE CONFIGURACIÓN DEL MÓDULO ---
// Esta clase leerá las opciones específicas de tu módulo desde el config.yml
// Necesitarías añadir una sección 'protection-settings' en tu config.yml
// para que estas opciones sean configurables.
class ProtectionModuleConfig (
    var preventCreeperGriefingInClaims: Boolean = true,
    var preventTntGriefingInClaims: Boolean = true,
    var preventCreeperGriefingInWarzone: Boolean = true,
    var preventTntGriefingInWarzone: Boolean = true
){
    private val configPath = "factions.protection"
    // Metodo para cargar la configuración desde el archivo principal del plugin
    fun reload(config: FileConfiguration) {
        // Asumiendo que las opciones están bajo 'protection-settings' en el config.yml principal
        preventCreeperGriefingInClaims = config.getBoolean("$configPath.prevent-creeper-griefing-in-claims", true)
        preventTntGriefingInClaims = config.getBoolean("$configPath.prevent-tnt-griefing-in-claims", true)
        preventCreeperGriefingInWarzone = config.getBoolean("$configPath.prevent-creeper-griefing-in-warzone", true)
        preventTntGriefingInWarzone = config.getBoolean("$configPath.prevent-tnt-griefing-in-warzone", true)
        // Puedes añadir más opciones aquí si las necesitas
        // minecart tnt
        // fireballs
        // fire spreading
        // mobs griefing
    }
}
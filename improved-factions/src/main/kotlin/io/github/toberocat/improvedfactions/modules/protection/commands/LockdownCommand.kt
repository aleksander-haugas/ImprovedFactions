
package io.github.toberocat.improvedfactions.modules.protection.commands

import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.database.DatabaseManager.loggedTransaction
import io.github.toberocat.improvedfactions.factions.Faction
import io.github.toberocat.improvedfactions.modules.protection.lockdown.FactionLockdown
import io.github.toberocat.improvedfactions.modules.protection.lockdown.FactionLockdownViolation
import io.github.toberocat.improvedfactions.modules.protection.lockdown.FactionLockdownViolations
import io.github.toberocat.improvedfactions.modules.protection.lockdown.FactionLockdowns
import io.github.toberocat.improvedfactions.modules.protection.lockdown.LockdownManager
import io.github.toberocat.improvedfactions.modules.protection.lockdown.LockdownState
import io.github.toberocat.improvedfactions.user.factionUser
import io.github.toberocat.improvedfactions.utils.command.CommandCategory
import io.github.toberocat.improvedfactions.utils.command.CommandMeta
import io.github.toberocat.improvedfactions.utils.options.FactionPermissionOption
import io.github.toberocat.improvedfactions.utils.options.InFactionOption
import io.github.toberocat.toberocore.command.PlayerSubCommand
import io.github.toberocat.toberocore.command.arguments.Argument
import io.github.toberocat.toberocore.command.options.Options
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import kotlin.time.Duration

@CommandMeta(
    category = CommandCategory.MANAGE_CATEGORY,
    description = "protection.commands.lockdown.description",
    module = "protection"
)

class LockdownCommand(
    private val plugin: ImprovedFactionsPlugin,
    private val lockdownManager: LockdownManager
) : PlayerSubCommand("lockdown") {

    override fun options(): Options = Options.getFromConfig(plugin, label) { options, _ ->
        options.cmdOpt(InFactionOption(true))
        options.cmdOpt(FactionPermissionOption("lockdown.manage"))
    }

    override fun arguments(): Array<Argument<*>> = emptyArray()

    override fun handle(player: Player, args: Array<String>): Boolean {
        if (args.isEmpty()) {
            player.sendMessage("${ChatColor.RED}Uso: /f lockdown <activate/start> <duration>")
            return true
        }

        val faction = getFactionOfPlayer(player)
        if (faction == null) {
            player.sendMessage("${ChatColor.RED}Debes pertenecer a una facción para usar este comando.")
            return true
        }

        // Verificar que el jugador tenga el permiso necesario
        loggedTransaction {
            if (!player.factionUser().hasPermission("lockdown.manage")) {
                player.sendMessage("${ChatColor.RED}No tienes permiso para gestionar el lockdown.")
                return@loggedTransaction true
            }
        }

        when (args[0].lowercase()) {
            "activate" -> handleActivate(player, faction, args.getOrNull(1))
            "start" -> handleStartSupervision(player, faction)
            "violations" -> handleViolations(player, faction)
            "status" -> handleStatus(player, faction)
            else -> {
                player.sendMessage("${ChatColor.RED}Subcomando desconocido. Usa 'activate', 'start', 'status' o 'violations'.")
            }
        }
        return true
    }

    // Agregar esta nueva función a la clase:
    private fun handleStatus(player: Player, faction: Faction) {
        val status = lockdownManager.getLockdownStatus(faction)

        player.sendMessage("${ChatColor.YELLOW}=== Estado del Lockdown ===")

        when (status.state) {
            LockdownState.NONE -> {
                player.sendMessage("${ChatColor.RED}No hay ningún lockdown activo o en supervisión.")
                player.sendMessage("${ChatColor.GRAY}Usa '/f lockdown start' para iniciar el período de supervisión.")
            }
            LockdownState.IN_SUPERVISION -> {
                player.sendMessage("${ChatColor.GREEN}Estado: ${ChatColor.WHITE}En período de supervisión")
                player.sendMessage("${ChatColor.GREEN}Tiempo restante: ${ChatColor.WHITE}${formatDuration(status.remainingTime)}")
                if (status.hasViolations) {
                    player.sendMessage("${ChatColor.RED}¡Atención! Se han detectado violaciones durante la supervisión.")
                    player.sendMessage("${ChatColor.RED}No podrás activar el lockdown hasta iniciar una nueva supervisión.")
                } else {
                    player.sendMessage("${ChatColor.GREEN}No se han detectado violaciones.")
                    player.sendMessage("${ChatColor.GRAY}Podrás activar el lockdown cuando termine el período de supervisión.")
                }
            }
            LockdownState.ACTIVE -> {
                player.sendMessage("${ChatColor.GREEN}Estado: ${ChatColor.WHITE}Lockdown Activo")
                player.sendMessage("${ChatColor.GREEN}Tipo: ${ChatColor.WHITE}${status.lockdownType?.displayName}")
                player.sendMessage("${ChatColor.GREEN}Tiempo restante: ${ChatColor.WHITE}${formatDuration(status.remainingTime)}")
            }
        }
    }

    private fun formatDuration(duration: Duration?): String {
        if (duration == null) return "0 segundos"

        val seconds = duration.inWholeSeconds
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60

        return buildString {
            if (days > 0) append("$days días ")
            if (hours > 0) append("$hours horas ")
            if (minutes > 0) append("$minutes minutos ")
            if (remainingSeconds > 0) append("$remainingSeconds segundos")
        }.trim()
    }


    // Modificamos la función hasViolations
    private fun handleViolations(player: Player, faction: Faction) {
        loggedTransaction {
            val lockdown = FactionLockdown.find {
                (FactionLockdowns.factId eq faction.id.toString()) and
                        (FactionLockdowns.endTime greater Clock.System.now()
                            .toLocalDateTime(TimeZone.currentSystemDefault()))
            }.firstOrNull()

            if (lockdown == null) {
                player.sendMessage("${ChatColor.RED}Tu facción no tiene un período de lockdown activo.")
                return@loggedTransaction
            }

            player.sendMessage("${ChatColor.YELLOW}=== Violaciones del Lockdown ===")

            FactionLockdownViolation
                .find { FactionLockdownViolations.lockdownId eq lockdown.id }
                .orderBy(FactionLockdownViolations.timestamp to SortOrder.DESC)
                .forEach { violation ->
                    player.sendMessage(
                        "${ChatColor.RED}${violation.violationType}: " +
                                "${ChatColor.WHITE}${violation.details} " +
                                "${ChatColor.GRAY}(${violation.timestamp})"
                    )
                }
        }
    }

    // Activamos el estado de la lockdown
    private fun handleActivate(player: Player, faction: Faction, durationStr: String?) {
        val duration = parseDuration(durationStr) ?: run {
            player.sendMessage("${ChatColor.RED}Duración inválida. Opciones disponibles: 1h, 3h, 12h, 1d, 3d, 7d")
            return
        }

        if (lockdownManager.activateLockdown(faction, duration)) {
            player.sendMessage("${ChatColor.GREEN}¡Lockdown activado exitosamente!")
        } else {
            player.sendMessage("${ChatColor.RED}No se puede activar el lockdown. Asegúrate de haber completado el período de supervisión sin violaciones.")
        }
    }

    private fun handleStartSupervision(player: Player, faction: Faction) {
        lockdownManager.startSupervision(faction)
        player.sendMessage("${ChatColor.GREEN}Período de supervisión iniciado. Complétalo sin violaciones para poder activar el lockdown.")
    }

    private fun parseDuration(durationStr: String?): Long? {
        return when (durationStr?.lowercase()) {
            "1h" -> 3600L
            "3h" -> 10800L
            "12h" -> 43200L
            "1d" -> 86400L
            "3d" -> 259200L
            "7d" -> 604800L
            else -> null
        }
    }

    private fun getFactionOfPlayer(player: Player): Faction? {
        return loggedTransaction {
            player.factionUser().faction()
        }
    }
}
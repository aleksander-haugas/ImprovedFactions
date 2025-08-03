
package io.github.toberocat.improvedfactions.modules.protection.lockdown

import io.github.toberocat.improvedfactions.ImprovedFactionsPlugin
import io.github.toberocat.improvedfactions.database.DatabaseManager.loggedTransaction
import io.github.toberocat.improvedfactions.factions.Faction
import io.github.toberocat.improvedfactions.modules.protection.config.ProtectionModuleConfig
import io.github.toberocat.improvedfactions.modules.protection.effects.LockdownParticleEffects
import kotlinx.datetime.*
import org.jetbrains.exposed.sql.and
import kotlin.time.Duration

enum class LockdownType(val duration: Long, val displayName: String) {
    SHORT_TERM(3600L, "Protección Corta"),      // 1h
    MEDIUM_TERM(43200L, "Protección Media"),     // 12h
    LONG_TERM(259200L, "Protección Extendida"), // 3d
    FULL_PROTECTION(604800L, "Protección Total") // 7d
}

data class LockdownStatus(
    val state: LockdownState = LockdownState.NONE,
    val remainingTime: Duration? = null,
    val lockdownType: LockdownType? = null,
    val hasViolations: Boolean = false
)


class LockdownManager(
    private val plugin: ImprovedFactionsPlugin,
    private val config: ProtectionModuleConfig
) {
    private val bossBar = LockdownBossBar(plugin, config)
    private val particleEffects = LockdownParticleEffects(plugin)



    fun startSupervision(faction: Faction) {
        loggedTransaction {
            // Primero limpiar cualquier lockdown existente y sus violaciones
            val existingLockdown = FactionLockdown.find {
                FactionLockdowns.factId eq faction.id.toString()
            }.firstOrNull()

            if (existingLockdown != null) {
                // Eliminar violaciones anteriores
                FactionLockdownViolation.find {
                    FactionLockdownViolations.lockdownId eq existingLockdown.id
                }.forEach { it.delete() }

                // Actualizar el lockdown existente
                existingLockdown.supervisionStartTime = getCurrentDateTime()
                existingLockdown.startTime = getCurrentDateTime()
                existingLockdown.endTime = getCurrentDateTime()
            } else {
                createNewLockdown(faction)
            }

            // Reiniciar el bossbar
            bossBar.cleanup() // Limpiar barra anterior si existe
            bossBar.showBar(faction)

            // Reproducir sonido para todos los miembros de la facción
            faction.members().forEach { member ->
                member.player()?.let { player ->
                    // Reproducir sonido del dragón
                    player.playSound(
                        player.location,
                        org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL,
                        1.0f,  // volumen
                        1.0f   // pitch
                    )
                }
            }
            // Reproducir efectos para todos los miembros de la facción
            faction.members().forEach { member ->
                member.player()?.let { player ->
                    // Sonido
                    player.playSound(
                        player.location,
                        org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL,
                        1.0f,
                        1.0f
                    )
                    // Efecto de partículas
                    particleEffects.playSupervisionStartEffect(player)
                }
            }
        }
    }

    fun activateLockdown(faction: Faction, duration: Long): Boolean {
        if (!config.lockdownAllowedDurations.contains(duration)) return false

        val result = loggedTransaction {
            val lockdown = FactionLockdown.find {
                FactionLockdowns.factId eq faction.id.toString()
            }.firstOrNull() ?: return@loggedTransaction null

            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

            // Verificar que el período de supervisión haya terminado
            val supervisionEndTime = lockdown.supervisionStartTime
                .toInstant(TimeZone.currentSystemDefault())
                .plus(config.lockdownSupervisionPeriod, DateTimeUnit.SECOND)
                .toLocalDateTime(TimeZone.currentSystemDefault())

            if (now < supervisionEndTime) {
                return@loggedTransaction null
            }

            if (hasViolations(faction)) {
                return@loggedTransaction null
            }

            if (isActive(lockdown)) {
                return@loggedTransaction null
            }

            lockdown.startTime = now
            lockdown.endTime = now.toInstant(TimeZone.currentSystemDefault())
                .plus(duration, DateTimeUnit.SECOND)
                .toLocalDateTime(TimeZone.currentSystemDefault())

            // Obtener los jugadores dentro de la transacción
            val members = faction.members().mapNotNull { member ->
                member.player()
            }

            members // Retornamos la lista de jugadores
        }

        // Si result es null, significa que hubo un error o no se cumplieron las condiciones
        if (result == null) return false

        // Aplicar efectos fuera de la transacción
        result.forEach { player ->
            // Sonido
            player.playSound(
                player.location,
                org.bukkit.Sound.ENTITY_WITHER_SPAWN,
                1.0f,
                0.8f
            )
            // Efecto de partículas
            particleEffects.playLockdownActivateEffect(player)
        }

        return true
    }

    // Y para el método startProtectionParticles, necesitamos importar BukkitRunnable:
    fun startProtectionParticles(faction: Faction) {
        object : org.bukkit.scheduler.BukkitRunnable() {
            override fun run() {
                val hasActiveLockdown = loggedTransaction {
                    FactionLockdown.find {
                        (FactionLockdowns.factId eq faction.id.toString()) and
                                (FactionLockdowns.startTime less Clock.System.now()
                                    .toLocalDateTime(TimeZone.currentSystemDefault())) and
                                (FactionLockdowns.endTime greater Clock.System.now()
                                    .toLocalDateTime(TimeZone.currentSystemDefault())) and
                                (FactionLockdowns.startTime greater FactionLockdowns.supervisionStartTime)
                    }.any()
                }

                if (!hasActiveLockdown) {
                    cancel()
                    return
                }

                loggedTransaction {
                    // Obtener todos los chunks reclamados por la facción
                    faction.claims().forEach { claim ->
                        val world = plugin.server.getWorld(claim.world)
                        if (world != null) {
                            val location = org.bukkit.Location(
                                world,
                                claim.chunkX * 16.0,
                                world.getHighestBlockYAt(claim.chunkX * 16, claim.chunkZ * 16).toDouble(),
                                claim.chunkZ * 16.0
                            )
                            particleEffects.playProtectionActiveEffect(location)
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 100L) // Cada 5 segundos
    }


    // Función para registrar una violación de contrato para la proteccion
    fun registerViolation(lockdown: FactionLockdown, type: ViolationType, details: String) {
        FactionLockdownViolation.new {
            this.lockdown = lockdown
            this.violationType = type
            this.timestamp = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            this.details = details
        }
    }

    // Reformateamos el time
    private fun getCurrentDateTime() = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())

    // Creamos una nueva auditoria para la lockdown
    private fun createNewLockdown(faction: Faction) {
        val now = getCurrentDateTime()
        FactionLockdown.new {
            factId = faction.id.toString()
            supervisionStartTime = now
            startTime = now
            endTime = now
        }
    }

    fun getLockdownStatus(faction: Faction): LockdownStatus {
        return loggedTransaction {
            val lockdown = FactionLockdown.find {
                FactionLockdowns.factId eq faction.id.toString()
            }.firstOrNull() ?: return@loggedTransaction LockdownStatus()

            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

            when {
                // En período de supervisión
                isInSupervision(lockdown) -> {
                    val supervisionEndTime = lockdown.supervisionStartTime
                        .toInstant(TimeZone.currentSystemDefault())
                        .plus(config.lockdownSupervisionPeriod, DateTimeUnit.SECOND)
                        .toLocalDateTime(TimeZone.currentSystemDefault())

                    LockdownStatus(
                        state = LockdownState.IN_SUPERVISION,
                        remainingTime = calculateRemainingTime(now, supervisionEndTime),
                        hasViolations = hasViolations(faction)
                    )
                }
                // Lockdown activo
                isActive(lockdown) -> {
                    val type = determineLockdownType(lockdown)
                    LockdownStatus(
                        state = LockdownState.ACTIVE,
                        remainingTime = calculateRemainingTime(now, lockdown.endTime),
                        lockdownType = type
                    )
                }
                // Sin lockdown
                else -> LockdownStatus()
            }
        }
    }

    private fun isInSupervision(lockdown: FactionLockdown): Boolean {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val supervisionEndTime = lockdown.supervisionStartTime
            .toInstant(TimeZone.currentSystemDefault())
            .plus(config.lockdownSupervisionPeriod, DateTimeUnit.SECOND)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        // Modificada la condición para verificar correctamente el período de supervisión
        return now >= lockdown.supervisionStartTime && now <= supervisionEndTime
    }

    private fun hasViolations(faction: Faction): Boolean {
        return loggedTransaction {
            val lockdown = FactionLockdown.find {
                FactionLockdowns.factId eq faction.id.toString()
            }.firstOrNull() ?: return@loggedTransaction true

            // Verificar violaciones solo durante el período de supervisión
            val supervisionStart = lockdown.supervisionStartTime
            val supervisionEnd = supervisionStart.toInstant(TimeZone.currentSystemDefault())
                .plus(config.lockdownSupervisionPeriod, DateTimeUnit.SECOND)
                .toLocalDateTime(TimeZone.currentSystemDefault())

            val violationsExist = FactionLockdownViolation
                .find {
                    (FactionLockdownViolations.lockdownId eq lockdown.id) and
                            (FactionLockdownViolations.timestamp greaterEq supervisionStart) and
                            (FactionLockdownViolations.timestamp lessEq supervisionEnd)
                }.any()

            violationsExist
        }
    }

    private fun isActive(lockdown: FactionLockdown): Boolean {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return now < lockdown.endTime && lockdown.startTime > lockdown.supervisionStartTime
    }

    private fun determineLockdownType(lockdown: FactionLockdown): LockdownType {
        val duration = lockdown.endTime.toInstant(TimeZone.currentSystemDefault())
            .minus(lockdown.startTime.toInstant(TimeZone.currentSystemDefault()))
            .inWholeSeconds

        return LockdownType.values().firstOrNull { it.duration == duration }
            ?: LockdownType.SHORT_TERM
    }

    private fun calculateRemainingTime(now: LocalDateTime, end: LocalDateTime): Duration {
        return end.toInstant(TimeZone.currentSystemDefault())
            .minus(now.toInstant(TimeZone.currentSystemDefault()))
    }
}
enum class LockdownState {
    NONE,
    IN_SUPERVISION,
    ACTIVE
}

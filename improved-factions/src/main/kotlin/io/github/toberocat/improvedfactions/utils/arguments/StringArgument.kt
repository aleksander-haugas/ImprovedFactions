package io.github.toberocat.improvedfactions.utils.arguments

import io.github.toberocat.toberocore.command.arguments.Argument
import org.bukkit.entity.Player

open class StringArgument(private val usage: String,
                          private val descriptionKey: String,
    private val tabComplete: List<String> = emptyList()) : Argument<String> {
    override fun parse(player: Player, arg: String): String {
        return arg
    }

    override fun tab(p0: Player): List<String> = tabComplete

    override fun descriptionKey(): String = descriptionKey

    override fun usage(): String = usage
}
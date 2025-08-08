package io.github.toberocat.improvedfactions.modules.bluemap.impl

import com.flowpowered.math.vector.Vector2i
import org.spongepowered.configurate.objectmapping.meta.Comment
import org.spongepowered.configurate.objectmapping.ConfigSerializable


@ConfigSerializable
data class Vector2(
    @Comment("X coordinate")
    var x: Float? = null,

    @Comment("Y coordinate")
    var y: Float? = null
) {
    fun checkInvalid(): Boolean {
        return x == null || y == null
    }

    fun toVector2i(): Vector2i {
        return if (x == null || y == null) Vector2i() else Vector2i(x!!.toInt(), y!!.toInt())
    }

    override fun toString(): String {
        return "Vector2 { x: $x, y: $y }"
    }
}

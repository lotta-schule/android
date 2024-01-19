package net.einsa.lotta.serializer

import android.graphics.Color
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

fun String.toColor(): Color {
    return Color.valueOf(Color.parseColor(this))
    // val matches =
    //     Regex("#([0-9a-fA-F]{1,2})([0-9a-fA-F]{1,2})([0-9a-fA-F]{1,2})([0-9a-fA-F]{0,2})").matchEntire(
    //         this
    //     )
    //         ?: throw IllegalArgumentException("Invalid color string: $this")
    // val red = matches.groupValues[1].toInt(16) / 255f
    // val green = matches.groupValues[2].toInt(16) / 255f
    // val blue = matches.groupValues[3].toInt(16) / 255f
    // val alpha =
    //     if (matches.groupValues[4].isEmpty()) 1f else matches.groupValues[4].toInt(16) / 255f

    // return Color.valueOf(red, green, blue, alpha)

}

object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Color) {
        val red = (value.red() * 255).toInt().toString(16).padStart(2, '0')
        val green = (value.green() * 255).toInt().toString(16).padStart(2, '0')
        val blue = (value.blue() * 255).toInt().toString(16).padStart(2, '0')
        val alpha = (value.alpha() * 255).toInt().toString(16).padStart(2, '0')

        encoder.encodeString("#$red$green$blue$alpha")
    }

    override fun deserialize(decoder: Decoder): Color {
        return decoder.decodeString().toColor()
    }
}

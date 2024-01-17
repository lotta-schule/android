package net.einsa.lotta.serializer

import com.auth0.jwt.JWT
import com.auth0.jwt.interfaces.DecodedJWT
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

object JWTSerializer : KSerializer<DecodedJWT> {
    @OptIn(ExperimentalSerializationApi::class, InternalSerializationApi::class)
    override val descriptor: SerialDescriptor =
        SerialDescriptor("JWT", DecodedJWT::class.serializer().descriptor)

    override fun serialize(encoder: Encoder, value: DecodedJWT) {
        val token = value.token
        encoder.encodeSerializableValue(String.serializer(), token)
    }

    override fun deserialize(decoder: Decoder): DecodedJWT {
        val token = decoder.decodeSerializableValue(String.serializer())
        return JWT.decode(token)
    }
}

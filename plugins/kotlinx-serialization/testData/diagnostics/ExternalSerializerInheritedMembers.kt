// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement

// `JsonContentPolymorphicSerializer` already implements `descriptor`, `serialize` and `deserialize`, so the
// plugin has nothing to generate and @Serializer has no effect. This used to crash the resolve extension with
// "Zero or multiple overrides found for serialize". See KT-74077.
@Serializable(Field.Companion::class)
sealed class Field {
    @OptIn(ExperimentalSerializationApi::class)
    <!EXTERNAL_SERIALIZER_USELESS!>@Serializer(forClass = Field::class)<!>
    companion object : JsonContentPolymorphicSerializer<Field>(Field::class) {
        override fun selectDeserializer(element: JsonElement): DeserializationStrategy<Field> {
            throw SerializationException("Unknown Message type")
        }
    }
}

@Serializable
data class StringField(@SerialName("\$type") val type: String, val id: String, val value: String) : Field()

// A plain external serializer that leaves everything to the plugin — @Serializer is meaningful here.
class Target(val i: Int)

@Serializer(forClass = Target::class)
object TargetSerializer : KSerializer<Target>

// All three members are written by hand — already reported before KT-74077.
class Other(val i: Int)

<!EXTERNAL_SERIALIZER_USELESS!>@Serializer(forClass = Other::class)<!>
object OtherSerializer : KSerializer<Other> {
    override val descriptor: SerialDescriptor get() = TODO()
    override fun serialize(encoder: Encoder, value: Other) {}
    override fun deserialize(decoder: Decoder): Other = TODO()
}

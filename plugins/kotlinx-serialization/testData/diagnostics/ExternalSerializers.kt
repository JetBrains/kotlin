// FIR_IDENTICAL
// SKIP_TXT

// FILE: test.kt
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

class Foo(i: Int, val j: Int)

<!EXTERNAL_CLASS_NOT_SERIALIZABLE!>@Serializer(forClass = Foo::class)<!>
object ExternalSerializer

<!EXTERNAL_SERIALIZER_USELESS!>@Serializer(forClass = Foo::class)<!>
object UselessExternalSerializer : KSerializer<Foo> {
    override val descriptor: SerialDescriptor
        get() {
            TODO()
        }

    override fun serialize(encoder: Encoder, value: Foo) {
        TODO()
    }

    override fun deserialize(decoder: Decoder): Foo {
        TODO()
    }
}

// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

class ClassA

object ClassASerializer : KSerializer<ClassA> {
    override val descriptor: SerialDescriptor get() = TODO()
    override fun serialize(encoder: Encoder, value: ClassA) {}
    override fun deserialize(decoder: Decoder): ClassA = TODO()
}

// The reproducer from the ticket: the intrinsic ignores type annotations, so the default serializer is used.
class Converters {
    fun fromString(value: String): List<<!SERIALIZABLE_WITH_ON_TYPE_HAS_NO_EFFECT!>@Serializable(with = ClassASerializer::class) ClassA<!>> =
        Json.decodeFromString(value)

    fun toString(value: List<<!SERIALIZABLE_WITH_ON_TYPE_HAS_NO_EFFECT!>@Serializable(with = ClassASerializer::class) ClassA<!>>): String =
        Json.encodeToString(value)
}

fun local() {
    val a: <!SERIALIZABLE_WITH_ON_TYPE_HAS_NO_EFFECT!>@Serializable(with = ClassASerializer::class) ClassA<!> = ClassA()
    println(a)
}

val topLevel: <!SERIALIZABLE_WITH_ON_TYPE_HAS_NO_EFFECT!>@Serializable(with = ClassASerializer::class) ClassA<!> = ClassA()

// Honoured positions — the plugin does pass the serializer on for these.
@Serializable
class Holder(
    val fromCtor: @Serializable(with = ClassASerializer::class) ClassA,
    val nested: List<@Serializable(with = ClassASerializer::class) ClassA>,
) {
    var inBody: @Serializable(with = ClassASerializer::class) ClassA = ClassA()
}

// Not a serializable class, so nothing is generated for its properties either.
class PlainHolder(val a: <!SERIALIZABLE_WITH_ON_TYPE_HAS_NO_EFFECT!>@Serializable(with = ClassASerializer::class) ClassA<!>)

// Binding a serializer through a typealias is the documented way to handle a type one does not own; the annotation
// survives the expansion and is honoured at every use site.
typealias ClassAAlias = @Serializable(with = ClassASerializer::class) ClassA

typealias NestedInAlias = List<@Serializable(with = ClassASerializer::class) ClassA>

@Serializable
class UsesAliases(val a: ClassAAlias, val list: NestedInAlias)

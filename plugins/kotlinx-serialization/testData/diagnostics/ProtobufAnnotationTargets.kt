// WITH_STDLIB

// MODULE: lib
// FILE: libtest.kt

package kotlinx.serialization.protobuf

import kotlinx.serialization.*

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class ProtoPacked

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class ProtoOneOf

public enum class ProtoIntegerType { DEFAULT, SIGNED, FIXED }

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class ProtoType(public val type: ProtoIntegerType)

// MODULE: main(lib)
// FILE: test.kt

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

@Serializable
class Packed(
    @ProtoPacked val list: List<Int>,
    @ProtoPacked val set: Set<Int>,
    @ProtoPacked val intArray: IntArray,
    @ProtoPacked val refArray: Array<String>,
    @ProtoPacked val uintArray: UIntArray,
    @ProtoPacked val custom: MyList,
)

@Serializable
class PackedWrong(
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoPacked<!> val i: Int,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoPacked<!> val s: String,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoPacked<!> val map: Map<Int, Int>,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoPacked<!> val nested: Nested,
)

@Serializable
class Typed(
    @ProtoType(ProtoIntegerType.SIGNED) val i: Int,
    @ProtoType(ProtoIntegerType.SIGNED) val l: Long,
    @ProtoType(ProtoIntegerType.SIGNED) val nullable: Int?,
    @ProtoType(ProtoIntegerType.SIGNED) val u: UInt,
    @ProtoType(ProtoIntegerType.SIGNED) val b: Byte,
    // Documented as affected by @ProtoType.
    @ProtoType(ProtoIntegerType.SIGNED) val c: Char,
    // RepeatedEncoder hands the property's tag to each element, so the integer type reaches them.
    @ProtoType(ProtoIntegerType.SIGNED) val list: List<Int>,
    @ProtoType(ProtoIntegerType.SIGNED) val intArray: IntArray,
    @ProtoType(ProtoIntegerType.SIGNED) val refArray: Array<Long>,
    // A @Serializable custom collection is encoded as a nested message, so the annotation is in fact dead here.
    // Accepted as a false negative rather than guessed at from the declared supertype.
    @ProtoType(ProtoIntegerType.SIGNED) val custom: MyList,
)

@Serializable
class TypedWrong(
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoType(ProtoIntegerType.SIGNED)<!> val s: String,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoType(ProtoIntegerType.SIGNED)<!> val d: Double,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoType(ProtoIntegerType.SIGNED)<!> val strings: List<String>,
)

// Boolean goes through encodeTaggedInt, so FIXED really does change its wire type.
@Serializable
class Booleans(
    @ProtoType(ProtoIntegerType.FIXED) val bool: Boolean,
    @ProtoType(ProtoIntegerType.FIXED) val bools: List<Boolean>,
    @ProtoType(ProtoIntegerType.FIXED) val boolArray: BooleanArray,
)

// A custom serializer decides the descriptor, so nothing can be concluded from the declared type.
@Serializable
class WithCustomSerializer(
    @ProtoType(ProtoIntegerType.SIGNED) @Serializable(NestedAsIntSerializer::class) val custom: Nested,
    // The serializer is attached to the type's own class rather than to this property.
    @ProtoType(ProtoIntegerType.SIGNED) val classLevel: CustomSerialized,
    // @Contextual is deliberately not consulted, so the declared type is judged as it stands.
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoPacked<!> @Contextual val contextual: Nested,
)

object NestedAsIntSerializer : KSerializer<Nested> by TODO()

object CustomSerializedSerializer : KSerializer<CustomSerialized> by TODO()

@Serializable(CustomSerializedSerializer::class)
class CustomSerialized(val i: Int)

// MapRepeatedEncoder derives the key and value tags from the property's own tag, so @ProtoType reaches map entries.
@Serializable
class Maps(
    @ProtoType(ProtoIntegerType.SIGNED) val intToInt: Map<Int, Int>,
    @ProtoType(ProtoIntegerType.SIGNED) val stringToInt: Map<String, Int>,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoType(ProtoIntegerType.SIGNED)<!> val stringToString: Map<String, String>,
    // Packing is ignored for maps.
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoPacked<!> val packed: Map<Int, Int>,
    // A Map subtype gets a MAP-kind serializer too, so it is found through the supertype walk.
    @ProtoType(ProtoIntegerType.SIGNED) val hashMap: HashMap<Int, Int>,
)

// A type-parameter entry is unknown while checking the class, so nothing is reported.
@Serializable
class GenericMaps<T>(
    @ProtoType(ProtoIntegerType.SIGNED) val stringToT: Map<String, T>,
    @ProtoType(ProtoIntegerType.SIGNED) val tToString: Map<T, String>,
)

// A value class forwards the property's tag to the value it wraps.
@Serializable
@JvmInline
value class MyId(val raw: Long)

@Serializable
class ValueClasses(
    @ProtoType(ProtoIntegerType.FIXED) val id: MyId,
    @ProtoType(ProtoIntegerType.FIXED) val ids: List<MyId>,
)

// The actual type of a type parameter is unknown while checking the class.
@Serializable
class Generic<T>(
    @ProtoPacked val t: T,
    @ProtoType(ProtoIntegerType.SIGNED) val u: T,
)

// Inherited properties belong to the supertype and are reported there, not once per subclass.
@Serializable
open class ProtoBase(<!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoPacked<!> val inherited: Int)

@Serializable
class ProtoDerived(val own: Int) : ProtoBase(own)

@Serializable
class OneOf(
    @ProtoOneOf val sealed: Base,
    @ProtoOneOf val nested: Nested,
)

@Serializable
class OneOfWrong(
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoOneOf<!> val i: Int,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoOneOf<!> val s: String,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoOneOf<!> val e: MyEnum,
)

@Serializable
class MyList(val backing: List<Int>) : Collection<Int> by backing

@Serializable
class Nested(val i: Int)

@Serializable
sealed class Base

@Serializable
enum class MyEnum { A, B }

// FILE: crossFile.kt

import kotlinx.serialization.*

// Subclassing from another file must not re-report the supertype's annotation at this file's offsets.
@Serializable
class ProtoDerivedElsewhere(val own: Int) : ProtoBase(own)

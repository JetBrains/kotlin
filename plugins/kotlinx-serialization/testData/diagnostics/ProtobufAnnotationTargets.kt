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
)

@Serializable
class TypedWrong(
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoType(ProtoIntegerType.SIGNED)<!> val s: String,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoType(ProtoIntegerType.SIGNED)<!> val d: Double,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoType(ProtoIntegerType.SIGNED)<!> val bool: Boolean,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoType(ProtoIntegerType.SIGNED)<!> val list: List<Int>,
)

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

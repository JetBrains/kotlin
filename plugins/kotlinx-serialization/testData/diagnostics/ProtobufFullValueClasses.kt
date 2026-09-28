// LANGUAGE: +FullValueClasses
// WITH_STDLIB

// MODULE: lib
// FILE: libtest.kt

package kotlinx.serialization.protobuf

import kotlinx.serialization.*

public enum class ProtoIntegerType { DEFAULT, SIGNED, FIXED }

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class ProtoType(public val type: ProtoIntegerType)

// MODULE: main(lib)
// FILE: test.kt

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

@Serializable
@JvmInline
value class InlineId(val raw: Long)

@Serializable
value class SingleFieldId(val raw: Long)

@Serializable
value class MultiFieldId(val high: Long, val low: Long)

@Serializable
value object ValueObjectId

@Serializable
class RegularId(val raw: Long)

@Serializable
object RegularObjectId

@Serializable
class Ids(
    @ProtoType(ProtoIntegerType.FIXED) val inline: InlineId,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoType(ProtoIntegerType.FIXED)<!> val singleField: SingleFieldId,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoType(ProtoIntegerType.FIXED)<!> val multiField: MultiFieldId,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoType(ProtoIntegerType.FIXED)<!> val valueObject: ValueObjectId,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoType(ProtoIntegerType.FIXED)<!> val regular: RegularId,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoType(ProtoIntegerType.FIXED)<!> val regularObject: RegularObjectId,
)

@Serializable
class IdCollections(
    @ProtoType(ProtoIntegerType.FIXED) val inlines: List<InlineId>,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoType(ProtoIntegerType.FIXED)<!> val singleFields: List<SingleFieldId>,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoType(ProtoIntegerType.FIXED)<!> val multiFields: List<MultiFieldId>,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoType(ProtoIntegerType.FIXED)<!> val regulars: List<RegularId>,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoType(ProtoIntegerType.FIXED)<!> val multiFieldValues: Map<String, MultiFieldId>,
    <!PROTOBUF_ANNOTATION_INAPPLICABLE_TYPE!>@ProtoType(ProtoIntegerType.FIXED)<!> val regularValues: Map<String, RegularId>,
)

// FIR_IDENTICAL
// WITH_STDLIB
// SKIP_TXT

// MODULE: lib
// FILE: libtest.kt

package kotlinx.serialization.protobuf

import kotlinx.serialization.*

public class ProtoUnknownFieldHolder private constructor() {
    companion object {
        val Empty = ProtoUnknownFieldHolder()
    }
}

@Serializable
public <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class ProtoUnknownFieldHolderSerializer<!> : KSerializer<ProtoUnknownFieldHolder> {
    public override val descriptor: <!SERIALIZER_NOT_FOUND, UNRESOLVED_REFERENCE!>SerialDescriptor<!> = <!UNRESOLVED_REFERENCE!>buildClassSerialDescriptor<!>("ProtoUnknownFieldHolder")
    public <!NOTHING_TO_OVERRIDE!>override<!> fun serialize(encoder: <!UNRESOLVED_REFERENCE!>Encoder<!>, value: ProtoUnknownFieldHolder) {}
    public <!NOTHING_TO_OVERRIDE!>override<!> fun deserialize(decoder: <!UNRESOLVED_REFERENCE!>Decoder<!>): ProtoUnknownFieldHolder = ProtoUnknownFieldHolder.Empty
}

// MODULE: main(lib)
// FILE: test.kt

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.ProtoUnknownFieldHolder

// OK: non-nullable with default value
@Serializable
data class ValidNonNullable(val unknown: <!SERIALIZER_NOT_FOUND!>ProtoUnknownFieldHolder<!> = ProtoUnknownFieldHolder.Empty)

// OK: nullable (no default value required)
@Serializable
data class ValidNullable(val unknown: <!SERIALIZER_NOT_FOUND!>ProtoUnknownFieldHolder?<!> = null)

// OK: nullable without default value
@Serializable
data class ValidNullableNoDefault(val unknown: <!SERIALIZER_NOT_FOUND!>ProtoUnknownFieldHolder?<!>)

// OK: no ProtoUnknownFieldHolder at all
@Serializable
data class NoAnnotation(val a: Int, val b: String)

// ERROR: more than one field with type ProtoUnknownFieldHolder
@Serializable
data class MultipleHolders(
    <!PROTO_UNKNOWN_FIELDS_MULTIPLE_HOLDERS!>val first: <!SERIALIZER_NOT_FOUND!>ProtoUnknownFieldHolder<!> = ProtoUnknownFieldHolder.Empty<!>,
    val second: <!SERIALIZER_NOT_FOUND!>ProtoUnknownFieldHolder<!> = ProtoUnknownFieldHolder.Empty,
)

// ERROR: non-nullable without default value
@Serializable
data class MissingDefault(<!PROTO_UNKNOWN_FIELDS_MISSING_DEFAULT!>val unknown: <!SERIALIZER_NOT_FOUND!>ProtoUnknownFieldHolder<!><!>)

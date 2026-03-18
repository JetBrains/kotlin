// FIR_IDENTICAL
// WITH_STDLIB
// SKIP_TXT

// MODULE: lib
// FILE: libtest.kt

package kotlinx.serialization.protobuf

import kotlinx.serialization.*

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class ProtoUnknownFields

public class ProtoUnknownFieldHolder private constructor() {
    companion object {
        val Empty = ProtoUnknownFieldHolder()
    }
}

// MODULE: main(lib)
// FILE: test.kt

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.ProtoUnknownFields
import kotlinx.serialization.protobuf.ProtoUnknownFieldHolder

// OK: non-nullable with default value
@Serializable
data class ValidNonNullable(@ProtoUnknownFields val unknown: <!SERIALIZER_NOT_FOUND!>ProtoUnknownFieldHolder<!> = ProtoUnknownFieldHolder.Empty)

// OK: nullable (no default value required)
@Serializable
data class ValidNullable(@ProtoUnknownFields val unknown: <!SERIALIZER_NOT_FOUND!>ProtoUnknownFieldHolder?<!> = null)

// OK: nullable without default value
@Serializable
data class ValidNullableNoDefault(@ProtoUnknownFields val unknown: <!SERIALIZER_NOT_FOUND!>ProtoUnknownFieldHolder?<!>)

// OK: no @ProtoUnknownFields at all
@Serializable
data class NoAnnotation(val a: Int, val b: String)

// ERROR: more than one field annotated with @ProtoUnknownFields
@Serializable
data class MultipleAnnotations(
    <!PROTO_UNKNOWN_FIELDS_MULTIPLE_ANNOTATIONS!>@ProtoUnknownFields<!> val first: <!SERIALIZER_NOT_FOUND!>ProtoUnknownFieldHolder<!> = ProtoUnknownFieldHolder.Empty,
    @ProtoUnknownFields val second: <!SERIALIZER_NOT_FOUND!>ProtoUnknownFieldHolder<!> = ProtoUnknownFieldHolder.Empty,
)

// ERROR: wrong type
@Serializable
data class WrongType(<!PROTO_UNKNOWN_FIELDS_WRONG_TYPE("WrongType; data; ByteArray")!>@ProtoUnknownFields<!> val data: ByteArray = byteArrayOf())

// ERROR: wrong type (String)
@Serializable
data class WrongTypeString(<!PROTO_UNKNOWN_FIELDS_WRONG_TYPE("WrongTypeString; extra; String")!>@ProtoUnknownFields<!> val extra: String = "")

// ERROR: non-nullable without default value
@Serializable
data class MissingDefault(<!PROTO_UNKNOWN_FIELDS_MISSING_DEFAULT!>@ProtoUnknownFields val unknown: <!SERIALIZER_NOT_FOUND!>ProtoUnknownFieldHolder<!><!>)

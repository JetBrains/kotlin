// FIR_IDENTICAL
// WITH_STDLIB
// SKIP_TXT

// MODULE: lib
// FILE: libtest.kt

package kotlinx.serialization.protobuf

import kotlinx.serialization.*

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class ProtoNumber(public val number: Int)

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
public annotation class ProtoOneOf

// MODULE: main(lib)
// FILE: test.kt

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoOneOf

@Serializable
data class SameNumbers(@ProtoNumber(1) val a: Int, @ProtoNumber(2) val b: Int, @ProtoNumber(3) val c: Int)

@Serializable
data class Replace(val a: Int, @ProtoNumber(4) val b: Int, @ProtoNumber(2) val c: Int)

@Serializable
data class Swap(val a: Int, @ProtoNumber(3) val b: Int, @ProtoNumber(2) val c: Int)

@Serializable
data class ConflictWithOrigin(val a: Int, <!PROTOBUF_PROTO_NUM_DUPLICATED("b; c")!>@ProtoNumber(3)<!> val b: Int, val c: Int)

@Serializable
data class ConflictWithCustom(<!PROTOBUF_PROTO_NUM_DUPLICATED("a; b")!>@ProtoNumber(10)<!> val a: Int, <!PROTOBUF_PROTO_NUM_DUPLICATED("b; a")!>@ProtoNumber(10)<!> val b: Int, val c: Int)

@Serializable
data class SkipCheckForOneOf(@ProtoNumber(2) val a: Int, @ProtoOneOf val oneOf: OneOf)

interface OneOf
// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// FILE: test.kt
import kotlinx.serialization.*

class NonSerializable

@Serializable
class Basic(val foo: <!SERIALIZER_NOT_FOUND("NonSerializable")!>NonSerializable<!>)

@Serializable
class Inside(val foo: List<<!SERIALIZER_NOT_FOUND("NonSerializable")!>NonSerializable<!>>)

@Serializable
class Inside2(val foo: List<List<<!SERIALIZER_NOT_FOUND("NonSerializable")!>NonSerializable<!>>>)

@Serializable
class WithImplicitType {
    val foo = <!SERIALIZER_NOT_FOUND!>NonSerializable()<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, primaryConstructor, propertyDeclaration */

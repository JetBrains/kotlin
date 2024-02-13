// WITH_STDLIB
// FIR_IDENTICAL
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
    <!SERIALIZER_NOT_FOUND("NonSerializable")!>val foo = NonSerializable()<!>
}

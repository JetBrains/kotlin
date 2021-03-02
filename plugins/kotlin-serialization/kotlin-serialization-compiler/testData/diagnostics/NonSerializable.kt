// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE
// WITH_RUNTIME
// FILE: test.kt
import kotlinx.serialization.*

class NonSerializable

@Serializable
class Basic(val foo: <!SERIALIZER_NOT_FOUND("NonSerializable")!>NonSerializable<!>)

@Serializable
class Inside(val foo: List<<!SERIALIZER_NOT_FOUND("NonSerializable")!>NonSerializable<!>>)

@Serializable
class WithImplicitType {
    <!SERIALIZER_NOT_FOUND("NonSerializable")!>val foo = NonSerializable()<!>
}
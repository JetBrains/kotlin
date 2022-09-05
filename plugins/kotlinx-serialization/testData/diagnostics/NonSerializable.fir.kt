// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE
// WITH_STDLIB
// FILE: test.kt
import kotlinx.serialization.*

class NonSerializable

@Serializable
class Basic(val foo: NonSerializable)

@Serializable
class Inside(val foo: List<NonSerializable>)

@Serializable
class WithImplicitType {
    val foo = NonSerializable()
}
// IGNORE_BACKEND_K1: JS_IR

// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// From #1264
@Serializable
sealed class TypedSealedClass<T>(val a: T) {
    @Serializable
    class Child(val y: Int) : TypedSealedClass<String>("10") {
        override fun toString(): String = "Child($a, $y)"
    }
}

// From #KT-43910
@Serializable
open class ValidatableValue<T : Any, V: Any>(
    var value: T? = null,
    var error: V? = null,
)

@Serializable
class Email<T: Any> : ValidatableValue<String, T>() { // Note this is a different T
    override fun toString(): String {
        return "Email($value, $error)"
    }
}

fun box(): String {
    val encodedChild = """{"a":"11","y":42}"""
    val decodedChild = Json.decodeFromString<TypedSealedClass.Child>(encodedChild)
    if (decodedChild.toString() != "Child(11, 42)") return "DecodedChild: $decodedChild"
    Json.encodeToString(decodedChild)?.let { if (it != encodedChild) return "EncodedChild: $it" }

    val email = Email<Int>().apply {
        value = "foo"
        error = 1
    }
    val encodedEmail = Json.encodeToString(email)
    if (encodedEmail != """{"value":"foo","error":1}""") return "EncodedEmail: $encodedEmail"
    val decodedEmail = Json.decodeFromString<Email<Int>>(encodedEmail)
    if (decodedEmail.toString() != "Email(foo, 1)") return "DecodedEmail: $decodedEmail"
    return "OK"
}

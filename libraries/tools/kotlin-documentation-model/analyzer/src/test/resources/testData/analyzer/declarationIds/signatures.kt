// MODULE: main
// FILE: sample/Signatures.kt
package sample

fun noArgs() {}
fun stringParam(value: String) {}
fun nullableString(value: String?) {}
fun String.stringReceiver() {}
fun Int.intReceiver() {}
context(value: String) fun contextReceiver() {}
suspend fun suspended() {}
fun varargParam(vararg value: String) {}
fun arrayOut(value: Array<out String>) {}
fun <T> generic(value: T) {}
fun <T : CharSequence> bounded(value: T) {}
fun functionType(block: (String) -> Unit) {}
fun suspendFunctionType(block: suspend (String) -> Unit) {}
fun extensionFunctionType(block: String.() -> Unit) {}
context(value: String) suspend fun <T : CharSequence> Int.complex(vararg values: T) {}
fun mapAndLambda(values: Map<String, List<Int>>, block: (String) -> Unit) {}
typealias Text = String
fun aliased(value: Text) {}
fun `quoted`() {}
fun `with spaces`() {}
fun `say hello!`() {}
fun `say+hello!`() {}
fun `say%20hello%21`() {}
class `A#B`
class `A%23B`
fun hashedClass(value: `A#B`) {}
fun escapedHashClass(value: `A%23B`) {}
class Outer<T, U> {
    fun member(value: T) {}
    fun member(value: U) {}
    fun<T> member(value: T) {}
}
val computed: Int
    get() = 0
var mutable: String
    get() = ""
    set(value) {}
context(value: String) var contextual: Int
    get() = 0
    set(value) {}
var <T> T.extensionValue: T
    get() = this
    set(value) {}
class WithAccessors {
    var counter: Int = 0
        get() = field
        set(value) {
            field = value
        }
}


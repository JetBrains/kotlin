// WITH_RUNTIME
// PROBLEM: none
class Optional<out T>(val value: T)

fun Any?.foo() = println("foo: $this")

fun main() {
    val b: Optional<Any?>? = Optional(null)
    b?.let<caret> { it.value.foo() }
}
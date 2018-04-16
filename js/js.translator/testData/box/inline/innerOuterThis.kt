// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1114
package foo

inline fun<T> with1(value: T, p: T.() -> Unit) = value.p()

class A(val expected: String) {
    val b = B()

    fun foo(): A {
        with1(b) {
            y = expected
        }
        return this
    }
}
class B() {
    var y = ""
}

fun box() = A("OK").foo().b.y
// WITH_RUNTIME
interface T

interface <caret>U: T {
    // INFO: {"checked": "true"}
    val x: Int
    // INFO: {"checked": "true"}
    fun foo(n: Int): Boolean
}
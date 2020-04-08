// PROBLEM: none
// WITH_RUNTIME
class SomeException : RuntimeException()
fun foo(): Int = 1

fun test(x: Boolean, y: Boolean) {
    if (x) {
        throw SomeException()
    } else if (y) {
        // empty
    } else<caret> {
        foo()
    }
}
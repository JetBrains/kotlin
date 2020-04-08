// WITH_RUNTIME
class SomeException : RuntimeException()
fun foo(): Int = 1
fun bar(): Int = 2

fun test(x: Boolean, y: Boolean) {
    if (x) {
        return
    } else if (y) {
        throw SomeException()
    } else<caret> {
        // comment1
        foo()
        // comment2
        bar()
    }
}
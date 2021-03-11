// PROBLEM: none
class FooException : RuntimeException()

fun test(i: Int) {
    val x = if (i == 1) {
        throw <caret>FooException()
    } else {
        0
    }
}
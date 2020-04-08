// PROBLEM: none
class FooException : RuntimeException()

fun createException() = FooException()

fun test(i: Int) {
    throw when (i) {
        1 -> FooException()
        else -> <caret>createException()
    }
}
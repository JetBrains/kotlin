// PROBLEM: Throwable instance 'FooException' is not thrown
// FIX: none
class FooException : Exception()

fun test() {
    val e = <caret>FooException()
}
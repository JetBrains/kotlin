// PROBLEM: none
class FooException : Exception()

fun test() {
    val e = <caret>FooException()
    throw e
}
// PROBLEM: none
class FooException : RuntimeException()

fun test() {
    throw <caret>FooException()
}
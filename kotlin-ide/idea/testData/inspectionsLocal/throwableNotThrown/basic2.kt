// PROBLEM: Result of 'createError' call is not thrown
// FIX: none
class FooException : RuntimeException()

fun createError() = FooException()

fun test() {
    createError<caret>()
}
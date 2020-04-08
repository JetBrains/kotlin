// WITH_RUNTIME
// IS_APPLICABLE: false

class FooException : Exception()

@Throws(exceptionClasses = [FooException::class])
fun test() {
    <caret>throw FooException()
}
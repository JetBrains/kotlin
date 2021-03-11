class FooException : Exception()

@Throws(FooException::class)
fun test() {
    <caret>throw java.io.IOException()
}

// RUNTIME_WITH_FULL_JDK
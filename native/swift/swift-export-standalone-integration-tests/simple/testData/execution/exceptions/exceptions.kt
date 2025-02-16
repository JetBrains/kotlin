// KIND: STANDALONE
// MODULE: Main
// FILE: exceptions.kt

// FIXME: error: this annotation is not applicable to target 'getter'. Applicable targets: function, constructor
//val variableWithThrowingGetter: String
//    @Throws(Exception::class)
//    get() { error("variableWithThrowingGetter") }

@Throws(Exception::class)
fun throwingFunctionThatThrows(value: String): Any {
    error("fatal – $value")
}

class NonConstructible {
    @Throws(Exception::class)
    constructor(value: String) {
        error("fatal – $value")
    }
}
// SKIP_ERRORS_BEFORE
// SKIP_ERRORS_AFTER
// IS_APPLICABLE: false

fun foo(s: String) {}

data class Example(private val str: String) {
    fun doWithExample(block : (Example) -> Unit) = block(Example("hello"))

}

fun Example.runExample() {
    doWithExample { example<caret> ->
        foo(example.str)
    }
}
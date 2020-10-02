// WITH_RUNTIME
// WITHOUT_CUSTOM_LINE_INDENT_PROVIDER
class Test {
    val test = run {
        <caret>foo()
    }

    fun foo(): Int {
        return 42
    }
}

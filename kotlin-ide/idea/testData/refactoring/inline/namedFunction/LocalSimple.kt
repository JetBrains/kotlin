fun bar(s: String) {}

fun foo() {
    fun local() {
        bar("Test")
    }

    <caret>local()
}
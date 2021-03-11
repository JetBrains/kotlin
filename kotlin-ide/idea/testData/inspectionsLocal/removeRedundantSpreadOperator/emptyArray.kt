fun foo(vararg x: String) {}

fun bar() {
    foo(*<caret>emptyArray<String>())
}

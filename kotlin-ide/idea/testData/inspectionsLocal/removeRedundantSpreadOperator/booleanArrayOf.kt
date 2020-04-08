fun foo(vararg x: Boolean) {}

fun bar() {
    foo(*<caret>booleanArrayOf(true, true))
}

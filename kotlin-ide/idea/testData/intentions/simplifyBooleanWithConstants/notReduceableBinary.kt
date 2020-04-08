fun test() {
    foo(<caret>false || !true) ?: return
}
fun foo(v: Boolean): Int = 1
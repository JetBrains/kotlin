fun test() {
    foo(false || !true) + foo(<caret>false || !true)
}
fun foo(v: Boolean): Int = 1
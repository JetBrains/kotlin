fun foo(f: () -> Unit) {}

fun test() {
    foo(/* c1 */ <caret>{ /* c2 */ } /* c3 */)
}
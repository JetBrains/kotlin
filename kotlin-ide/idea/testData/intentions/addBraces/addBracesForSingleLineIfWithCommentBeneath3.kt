fun foo() {}

fun test(b: Boolean) {
    <caret>if (b) /* aaa */ foo() // bbb
    // ccc
}
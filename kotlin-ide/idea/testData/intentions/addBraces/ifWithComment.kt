fun foo() {}

fun test(b: Boolean) {
    <caret>if (b) /* aaa */ /* bbb */ foo() // ccc
}

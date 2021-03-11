fun Int.<caret>foo(a: Int, bb: Int): Int = a + bb

fun test() {
    0.foo(1, bb = 2)
}
fun Int.<caret>foo(a: Int, b: Int): Int = a + b

fun test() {
    0.foo(1, b = 2)
}
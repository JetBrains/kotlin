class A(val n: Int) {
    val <caret>foo: Boolean = n > 1
}

fun test() {
    val t = A(1).foo
}
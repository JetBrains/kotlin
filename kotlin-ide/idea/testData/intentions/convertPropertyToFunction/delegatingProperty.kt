// WITH_RUNTIME
// IS_APPLICABLE: false

class A(val n: Int) {
    val <caret>foo: Boolean by lazy { n > 1 }
}

fun test() {
    val t = A(1).foo
}
// WITH_RUNTIME

annotation class X(val s: String)

class A(val n: Int) {
    internal @X("1") val <T : Number> T.<caret>foo: Boolean
        get() = toInt() - n > 1
}

fun test() {
    val t = with(A(1)) {
        2.5.foo
    }
}
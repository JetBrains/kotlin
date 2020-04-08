class A(var n: Int) {
    fun foo(m: Int) {
        n += m
    }
}

fun test() {
    val a = A(0)
    a.foo(1)
    a.foo(1)
}
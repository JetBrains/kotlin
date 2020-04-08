class A(val n: Int) {
    fun foo(k: Int): Boolean = k <= n
}

fun test() {
    A(2) foo 1
    A(2).foo(1)
    !A(2).foo(1)
}
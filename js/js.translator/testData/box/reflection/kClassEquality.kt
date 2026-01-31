class A {
    fun a() {}
}

fun box() {
    assertTrue(A::a == A::a)
}
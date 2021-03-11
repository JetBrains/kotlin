class A {
    fun <caret>a(a: Int) = b(a)
    fun b(a: Int) = Unit
}
fun aadada() {
    val test = A()
    test.a(42)
}

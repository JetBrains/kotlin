// IS_APPLICABLE: false
class A {
    fun foo() {}
    fun bar(lambda: () -> Unit = {}): Int = 1
}
fun baz(a: A): Int = 1

fun test() {
    val a = A()
    a.foo()
    baz(a) + <caret>a.bar {}
    a.foo()
}
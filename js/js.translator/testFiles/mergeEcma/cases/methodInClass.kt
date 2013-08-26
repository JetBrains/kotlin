package foo

class A {
    fun foo(): Int {
        return 1
    }

}

fun box(): String {
    val a = A()
    if (a.foo() != 1) return "a.foo() != 1, it: ${a.foo()}"
    return "OK"
}
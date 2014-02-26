package foo

class A {
    val a: Int = 1
        get() = $a + 1

    fun getA(): Int {
        return $a
    }
}

fun box(): String {
    val a = A()
    if (a.a != 2) return "A().a != 2, it: ${a.a}"
    if (a.getA() != 1) return "A().getA() != 1, it: ${a.getA()}"
    return "OK"
}
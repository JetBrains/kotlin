package foo

class A {
    val a: Int = 1
        get() = field + 1

    fun getA(): Int {
        return a
    }
}

fun box(): String {
    val a = A()
    if (a.a != 2) return "A().a != 2, it: ${a.a}"
    if (a.getA() != 2) return "A().getA() != 2, it: ${a.getA()}"
    return "OK"
}
class A {
    val x: Int
    init {
        x = 10
    }
}

class B(val x: String) {
    fun foo(): String = x
    val foo2 = x
}

fun box(): String {
    val z = Any()
    val x = A()
    if (x.x != 10) return "Fail"
    return B("OK").foo2
}
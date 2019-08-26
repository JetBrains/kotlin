
open class A {
    open fun foo(): Int = 1
}

class B : A() {
    override fun foo(): Int = 2
}

fun box(): String {
    val a: A = A()
    val res1 = a.foo()
    if (res1 != 1)
        return "Fail1: $res1"

    val b: A = B()
    val res2 = b.foo()
    if (res2 != 2)
         return "Fail2: $res2"

    return "OK"
}
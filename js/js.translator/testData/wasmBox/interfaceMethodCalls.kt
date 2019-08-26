
interface I {
    open fun foo(): Int
}

class C : I {
    final override fun foo(): Int = 2
}

class C2(val x: Int) : I {
    override fun foo(): Int = x
}

fun callFoo(x: I) = x.foo()

fun box(): String {
    val c: I = C()
    if (callFoo(c) != 2) {
        return "Fail"
    }

    if (callFoo(C2(10)) != 10)
        return "Fail 2"

    if (callFoo(C2(1000)) != 1000)
        return "Fail 3"

    return "OK"
}
// EXPECTED_REACHABLE_NODES: 1293
open class A {
    fun foo(): Char = 'X'
}

interface I {
    @JsName("foo")
    fun foo(): Any
}

class B : A(), I

fun typeOf(x: dynamic): String = js("typeof x")

fun box(): String {
    val b = B()
    val i: I = B()
    val a: A = B()

    val r1 = typeOf(b.asDynamic().foo())
    if (r1 != "object") return "fail1: $r1"

    val r2 = typeOf(i.asDynamic().foo())
    if (r2 != "object") return "fail2: $r2"

    val r3 = typeOf(a.asDynamic().foo())
    if (r3 != "object") return "fail3: $r3"

    val x4 = b.foo()
    val r4 = typeOf(x4)
    if (r4 != "number") return "fail4: $r4"

    val x5 = i.foo()
    val r5 = typeOf(x5)
    if (r5 != "object") return "fail5: $r5"

    val x6 = a.foo()
    val r6 = typeOf(x6)
    if (r6 != "number") return "fail6: $r6"

    return "OK"
}
// EXPECTED_REACHABLE_NODES: 506
external open class A

interface I

class B : A(), I

open class C : A()

class D : C(), I

fun box(): String {
    var a: Any = A()
    if (a is I) return "fail1"

    a = B()
    if (a !is I) return "fail2"

    a = D()
    if (a !is I) return "fail3"

    a = C()
    if (a is I) return "fail4"

    return "OK"
}
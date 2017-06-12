// EXPECTED_REACHABLE_NODES: 493
package foo

class A {
    var a = 3
    companion object {
        var a = -2
    }
}


fun box(): String {
    A.a = 2
    if (A.a != 2) return "A.a != 2, it: ${A.a}"

    val a = A
    a.a = 3
    if (a.a != 3) return "a = A; a.a = 3; a != 3, it: ${a.a}"

    if (A().a != 3) return "A().a != 3, it: ${A().a}"

    val x = A()
    x.a = 4
    if (x.a != 4) return "x = A(); x.a = 4; x.a != 4, it: ${x.a}"

    return "OK"
}

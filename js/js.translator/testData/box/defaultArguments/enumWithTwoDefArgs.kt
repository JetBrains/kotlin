// EXPECTED_REACHABLE_NODES: 522
package foo

enum class Foo(val a: Int = 1, val b: String = "a") {
    A(),
    B(2, "b"),
    C(b = "b"),
    D(a = 2)
}

fun box(): String {
    if (Foo.A.a != 1 || Foo.A.b != "a") return "fail1"
    if (Foo.B.a != 2 || Foo.B.b != "b") return "fail2"
    if (Foo.C.a != 1 || Foo.C.b != "b") return "fail3"
    if (Foo.D.a != 2 || Foo.D.b != "a") return "fail4"
    return "OK"
}

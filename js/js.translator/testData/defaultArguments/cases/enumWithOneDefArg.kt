package foo

enum class Foo(val a: Int = 1, val b: String) {
    B: Foo(2, "b")
    C: Foo(b = "b")
}

fun box(): String {
    if (Foo.B.a != 2 || Foo.B.b != "b") return "fail1"
    if (Foo.C.a != 1 || Foo.C.b != "b") return "fail2"
    return "OK"
}

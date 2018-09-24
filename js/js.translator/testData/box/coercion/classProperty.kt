// EXPECTED_REACHABLE_NODES: 1284
class A {
    var log = ""

    fun foo() {
        log += "foo()"
    }

    val bar: Any = foo()
}

fun box(): String {
    val a = A()
    if (a.bar != Unit) return "fail1: ${a.bar}"
    if (a.log != "foo()") return "fail2: ${a.log}"

    return "OK"
}
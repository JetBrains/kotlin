// EXPECTED_REACHABLE_NODES: 1283
var log = ""

fun foo() {
    log += "foo()"
}

val bar: Any = foo()

fun box(): String {
    if (bar != Unit) return "fail1: $bar"
    if (log != "foo()") return "fail2: $log"

    return "OK"
}
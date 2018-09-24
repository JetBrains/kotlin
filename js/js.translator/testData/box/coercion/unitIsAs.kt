// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1282
var log = ""

fun foo(): Unit {
    log += "foo();"
}

fun box(): String {
    if (foo() !is Any) return "fail1"
    if (foo() as Any != Unit) return "fail2"
    if (log != "foo();foo();") return "fail3: $log"

    return "OK"
}
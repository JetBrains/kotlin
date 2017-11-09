// EXPECTED_REACHABLE_NODES: 1027
var log = ""

fun box(): String {
    foo() ?: bar()
    if (foo() == null) bar()

    if (log != "foo;foo;") return "fail: $log"
    return "OK"
}

fun foo() {
    log += "foo;"
}

fun bar() {
    log += "bar;"
}

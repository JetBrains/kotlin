// EXPECTED_REACHABLE_NODES: 1281
// IGNORE_BACKEND: JS_IR
// CALL_MAIN

var ok: String = "fail"

fun main() {
    ok = "OK"
}

fun box() = ok
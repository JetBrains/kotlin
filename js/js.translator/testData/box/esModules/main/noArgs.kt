// EXPECTED_REACHABLE_NODES: 1281
// ES_MODULES
// CALL_MAIN

var ok: String = "fail"

fun main() {
    ok = "OK"
}

fun box() = ok
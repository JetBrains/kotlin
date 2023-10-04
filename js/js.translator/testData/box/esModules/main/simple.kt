// EXPECTED_REACHABLE_NODES: 1281
// ES_MODULES
// CALL_MAIN

var ok: String = "fail"

fun main(args: Array<String>) {
    if (0 != args.size) error("fail")

    ok = "OK"
}

fun box() = ok
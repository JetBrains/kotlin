// EXPECTED_REACHABLE_NODES: 1281
// ES_MODULES
// CALL_MAIN

var ok: String = "OK"

fun main(args: Array<Int>) {
    ok += "Fail Int"
}

fun main(args: Array<in String>) {
    ok += "Fail IN"
}

fun main(args: Array<String>): Unit? {
    ok += "Fail return Unit?"
    return Unit
}

fun Any.main(args: Array<String>) {
    ok += "Fail Any.main(...)"
}

fun box() = ok
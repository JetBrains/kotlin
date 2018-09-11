// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1283
var log = ""

fun foo() {
    log += "foo()"
}

fun test(x: Int) = when (x) {
    in 0..10 -> foo()
    else -> 55
}

fun box(): String {
    val a = test(20)
    if (a !is Int) return "fail1: $a"

    val b = test(5)
    if (b !is Unit) return "fail2: $b"
    if (log != "foo()") return "fail3: $log"

    return "OK"
}
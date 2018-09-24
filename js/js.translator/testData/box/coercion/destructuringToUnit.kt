// EXPECTED_REACHABLE_NODES: 1286
var log = ""

class A {
    operator fun component1() {
        log += "A.component1()"
    }

    operator fun component2() = 23
}

fun box(): String {
    val (x: Any, y) = A()

    if (x != Unit) return "fail1: $x"
    if (y != 23) return "fail2: $y"
    if (log != "A.component1()") return "fail3: $log"

    return "OK"
}
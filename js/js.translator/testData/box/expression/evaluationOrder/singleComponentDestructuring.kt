// EXPECTED_REACHABLE_NODES: 1283
// CHECK_VARS_COUNT: function=test count=2
var log = ""

fun test() {
    val (a, _) = idAndLog(Pair(idAndLog(1), idAndLog(2)))
    val (_, b) = idAndLog(Pair(idAndLog(3), idAndLog(4)))
    log += "result:$a,$b;"
}

fun <T> idAndLog(x: T): T {
    log += "$x;"
    return x
}

fun box(): String {
    test()
    if (log != "1;2;(1, 2);3;4;(3, 4);result:1,4;") return "fail: $log"
    return "OK"
}
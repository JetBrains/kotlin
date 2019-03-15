// EXPECTED_REACHABLE_NODES: 1291
var log = ""

fun foo(a: Boolean, b: () -> Boolean): Boolean = a or b()

fun bar(a: Boolean, b: () -> Boolean): Boolean = a and b()

fun box(): String {
    if (!foo(true) { log += "1"; false }) return "fail1"
    if (!foo(true) { log += "2"; true }) return "fail2"
    if (foo(false) { log += "3"; false }) return "fail3"
    if (!foo(false) { log += "4"; true }) return "fail4"

    if (bar(true) { log += "5"; false }) return "fail5"
    if (!bar(true) { log += "6"; true }) return "fail6"
    if (bar(false) { log += "7"; false }) return "fail7"
    if (bar(false) { log += "8"; true }) return "fail8"

    if (log != "12345678") return "fail log: $log"

    return "OK"
}
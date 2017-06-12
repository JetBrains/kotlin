// EXPECTED_REACHABLE_NODES: 490
var log = ""

fun printlnLog(message: Any) {
    log += "$message\n"
}

fun box(): String {
    printlnLog(true.and1(true))

    if (log != "true\n") return "fail: $log"
    return "OK"
}

fun Boolean.and1(other: Boolean): Boolean {
    if (other == true) {
        if (this == true) {
            return true ;
        }
        else {
            return false;
        }
    }
    else {
        return false;
    }
}

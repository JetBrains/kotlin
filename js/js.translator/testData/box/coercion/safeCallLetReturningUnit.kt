// EXPECTED_REACHABLE_NODES: 1112
var log = ""

fun test(param: Any?) {
    param?.let {
        log += "test($param);"
    } ?: run {
        log += "test-null;"
    }
}

fun box(): String {
    test(null)
    test(23)

    if (log != "test-null;test(23);") return "fail: $log"

    return "OK"
}

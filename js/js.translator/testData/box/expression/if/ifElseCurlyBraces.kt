// EXPECTED_REACHABLE_NODES: 1282
var log = ""

fun foo(s: String, a: Int, b: Int) {
    when (s) {
        "A" -> if (a > b) {
            log += "1"
        }
        else if (b > a) {
            log += "2"
        }
        "B" -> log += "3"
    }
}

fun box(): String {
    foo("A", 3, 2)
    foo("A", 2, 3)
    foo("B", 2, 3)

    if (log != "123") return "fail: $log"

    return "OK"
}
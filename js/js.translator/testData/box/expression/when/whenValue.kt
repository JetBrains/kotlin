// EXPECTED_REACHABLE_NODES: 487
package foo

fun box(): String {
    var a = 4
    when(a) {
        3 -> {
            a = 10;
        }
        4 -> {
            a = 20;
        }
        else -> {
            a = 30;
        }
    }
    if (a != 20) return "fail: $a"

    return "OK"
}
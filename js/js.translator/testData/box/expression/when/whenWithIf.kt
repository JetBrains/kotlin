// EXPECTED_REACHABLE_NODES: 487
package foo

fun box(): String {

    var c = when(3) {
        3 -> 1
        2 -> 100
        else -> 100
    } + when (2) {
        1 -> 100
        else -> 1
    } + when (0) {
        1 -> if (true) 100 else 100
        0 -> if (false) {
            100
        }
        else {
            1
        }
        else -> 100
    }

    if (c != 3) return "fail"

    return "OK"
}
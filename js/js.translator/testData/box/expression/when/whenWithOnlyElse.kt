// EXPECTED_REACHABLE_NODES: 1280
package foo

fun box(): String {
    var a = 0
    when (a) {
        else -> a = 2
    }
    if (a != 2) return "fail"

    return "OK"
}

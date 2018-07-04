// EXPECTED_REACHABLE_NODES: 1122
/*
Modified test case from KT-24777
 */
package foo

inline fun condition() = false

fun run(x: Boolean): String {
    var y = 0
    do {
        if (y > 0)
            return "NOT OK"
        y += 1
        do {
        } while (false)
        if (x) continue
    } while (condition())
    return "OK"
}

fun box(): String {
    return run(true)
}

// EXPECTED_REACHABLE_NODES: 490
package foo

var c = 2

fun loop(times: Int) {
    var left = times
    while (left > 0) {
        val u: (value: Int) -> Unit = {
            c++
        }
        u(left--)
    }
}

fun box(): Any? {
    loop(5)
    return if (c == 7) return "OK" else c
}
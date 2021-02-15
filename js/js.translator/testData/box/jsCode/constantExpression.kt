// EXPECTED_REACHABLE_NODES: 1282
package foo

const val x = '2'
const val y = '+'
const val z = '3'

fun box(): String {
    if (js(x.toString() + z) !== 23) return "Fail 1"
    if (js(x.toString() + y + z) !== 5) return "Fail 2"

    return "OK"
}
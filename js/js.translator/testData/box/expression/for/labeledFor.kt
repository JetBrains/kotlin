// EXPECTED_REACHABLE_NODES: 491
package foo

val a1 = arrayOfNulls<Int>(0)

fun box(): String {
    var bar = 33
    outer@ for (a in arrayOf(1, 2)) {
        for (b in arrayOf(1, 4)) {
            break@outer
        }
        bar = 42
    }
    if (bar != 33) return "fail: $bar"

    return "OK"
}
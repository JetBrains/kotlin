// EXPECTED_REACHABLE_NODES: 489
package foo

var c = 2

fun incC(i: Int) {
    c += c + i
}

fun box(): Any? {
    for (i in 0..2) {
        incC(i)
    }
    return if (c == 20) "OK" else c
}
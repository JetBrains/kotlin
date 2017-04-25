// EXPECTED_REACHABLE_NODES: 489
package foo

var d = 0

fun f(): Int {
    d = if (d < 0) -100 else 100
    return d
}

fun box(): String {
    d = d-- + f() + when(d) {
        -100 -> return "OK"
        1 -> 1
        else -> return "fail1"
    }
    return "fail2"
}

// EXPECTED_REACHABLE_NODES: 489
package foo

fun box(): String {
    var i = 0
    val c = sum(++i, if (i == 0) return "fail1" else i + 2)
    if (c != 4) {
        return "fail2: $c"
    }
    return "OK"
}


fun sum(a1: Int, a2: Int) = a1 + a2

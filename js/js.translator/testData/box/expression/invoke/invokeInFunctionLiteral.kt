// EXPECTED_REACHABLE_NODES: 489
package foo

fun box(): String {
    val v1 = { x: Int -> x}(2)

    val f = { x: Int -> x}
    val v2 = (f)(2)

    if (v1 != 2) return "fail1: $v1"
    if (v2 != 2) return "fail2: $v2"

    return "OK"
}

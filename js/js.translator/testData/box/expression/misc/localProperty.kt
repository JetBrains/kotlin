// EXPECTED_REACHABLE_NODES: 489
package foo

val y = 3

fun f(a: Int): Int {
    val x = 42
    val y = 50

    return y
}

fun box(): String {
    val r = f(y)
    return if (r == 50) "OK" else "Fail, r = $r"
}

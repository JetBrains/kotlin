// EXPECTED_REACHABLE_NODES: 493
package foo

class Point(val x: Int, val y: Int) {
    fun mul(): (scalar: Int) -> Point {
        return { scalar: Int -> Point(x * scalar, y * scalar) }
    }
}

val m = Point(2, 3).mul()

fun box(): String {
    val answer = m(5)
    return if (answer.x == 10 && answer.y == 15) "OK" else "FAIL"
}

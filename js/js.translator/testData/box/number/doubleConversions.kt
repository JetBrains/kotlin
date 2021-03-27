// EXPECTED_REACHABLE_NODES: 1368
package foo

fun box(): String {
    val c: Double = 3.6
    if (c.toDouble() != 3.6) {
        return "fail1"
    }
    if (c.toFloat() != 3.6.toFloat()) {
        return "fail2"
    }
    if (c.toInt() != 3) {
        return "fail4"
    }

    val cn: Double = -3.6
    if (cn.toDouble() != -3.6) {
        return "fail6"
    }
    if (cn.toFloat() != -3.6.toFloat()) {
        return "fail7"
    }
    if (cn.toInt() != -3) {
        return "fail9"
    }

    val f: Float = 3.6.toFloat()
    if (f.toDouble() != 3.6) {
        return "fail11"
    }
    if (f.toFloat() != 3.6.toFloat()) {
        return "fail12"
    }
    if (f.toInt() != 3) {
        return "fail14"
    }

    val fn: Float = -3.6.toFloat()
    if (fn.toDouble() != -3.6) {
        return "fail16"
    }
    if (fn.toFloat() != -3.6.toFloat()) {
        return "fail17"
    }
    if (fn.toInt() != -3) {
        return "fail19"
    }

    return "OK"
}
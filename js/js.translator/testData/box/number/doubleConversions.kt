// EXPECTED_REACHABLE_NODES: 489
package foo

fun box(): String {
    val c: Double = 3.6
    if (c.toDouble() != 3.6) {
        return "fail1"
    }
    if (c.toFloat() != 3.6.toFloat()) {
        return "fail2"
    }
    if (c.toByte() != 3.toByte()) {
        return "fail3"
    }
    if (c.toInt() != 3) {
        return "fail4"
    }
    if (c.toShort() != 3.toShort()) {
        return "fail5"
    }

    val cn: Double = -3.6
    if (cn.toDouble() != -3.6) {
        return "fail6"
    }
    if (cn.toFloat() != -3.6.toFloat()) {
        return "fail7"
    }
    if (cn.toByte() != (-3).toByte()) {
        return "fail8"
    }
    if (cn.toInt() != -3) {
        return "fail9"
    }
    if (cn.toShort() != (-3).toShort()) {
        return "fail10"
    }

    val f: Float = 3.6.toFloat()
    if (f.toDouble() != 3.6) {
        return "fail11"
    }
    if (f.toFloat() != 3.6.toFloat()) {
        return "fail12"
    }
    if (f.toByte() != 3.toByte()) {
        return "fail13"
    }
    if (f.toInt() != 3) {
        return "fail14"
    }
    if (f.toShort() != 3.toShort()) {
        return "fail15"
    }

    val fn: Float = -3.6.toFloat()
    if (fn.toDouble() != -3.6) {
        return "fail16"
    }
    if (fn.toFloat() != -3.6.toFloat()) {
        return "fail17"
    }
    if (fn.toByte() != (-3).toByte()) {
        return "fail18"
    }
    if (fn.toInt() != -3) {
        return "fail19"
    }
    if (fn.toShort() != (-3).toShort()) {
        return "fail20"
    }

    return "OK"
}
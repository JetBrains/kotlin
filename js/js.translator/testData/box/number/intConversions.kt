// EXPECTED_REACHABLE_NODES: 489
package foo

fun box(): String {
    val c: Int = 3
    if (c.toDouble() != 3.0) {
        return "fail1"
    }
    if (c.toFloat() != 3.toFloat()) {
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
    val c2: Int = -5
    if (c2.toShort() != (-5).toShort()) {
        return "fail6"
    }
    if (c2.toFloat() != -5.toFloat()) {
        return "fail7"
    }
    return "OK"
}
package foo

fun box(): Boolean {
    val c: Int = 3
    if (c.toDouble() != 3.0) {
        return false
    }
    if (c.toFloat() != 3.toFloat()) {
        return false
    }
    if (c.toByte() != 3.toByte()) {
        return false
    }
    if (c.toInt() != 3) {
        return false
    }
    if (c.toShort() != 3.toShort()) {
        return false
    }
    val c2: Int = -5
    if (c2.toShort() != (-5).toShort()) {
        return false
    }
    if (c2.toFloat() != -5.toFloat()) {
        return false
    }
    return true
}
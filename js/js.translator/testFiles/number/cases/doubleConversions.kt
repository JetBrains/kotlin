package foo

fun box(): Boolean {
    val c: Double = 3.6
    if (c.toDouble() != 3.6) {
        return false
    }
    if (c.toFloat() != 3.6.toFloat()) {
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

    val f: Float = 3.6.toFloat()
    if (f.toDouble() != 3.6) {
        return false
    }
    if (f.toFloat() != 3.6.toFloat()) {
        return false
    }
    if (f.toByte() != 3.toByte()) {
        return false
    }
    if (f.toInt() != 3) {
        return false
    }
    if (f.toShort() != 3.toShort()) {
        return false
    }
    return true
}
package foo

fun testIntegerConversions(c: Number): Boolean {
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
    return true
}

fun testFloatingPointConversions(c: Number): Boolean {
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
    return true
}

fun box(): Boolean = testIntegerConversions(3) && testFloatingPointConversions(3.6) && testFloatingPointConversions(3.6.toFloat()) &&
testIntegerConversions(3.toByte()) && testIntegerConversions(3.toShort())
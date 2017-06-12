// EXPECTED_REACHABLE_NODES: 491
package foo

fun testShortConversions(c: Short): Boolean {
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

fun testByteConversions(c: Byte): Boolean {
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

fun box(): String {
    if (!testShortConversions(3)) return "fail: testShortConversions"
    if (!testByteConversions(3)) return "fail: testByteConversions"
    return "OK"
}
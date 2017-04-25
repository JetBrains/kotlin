// EXPECTED_REACHABLE_NODES: 495
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

fun box(): String {
    if (!testIntegerConversions(3)) return "fail: testIntegerConversions1"
    if (!testFloatingPointConversions(3.6)) return "fail: testFloatingPointConversions1"
    if (!testFloatingPointConversions(3.6.toFloat())) return "fail: testFloaintPointConversions2"
    if (!testIntegerConversions(3.toByte())) return "fail: testIntegerConversions2"
    if (!testIntegerConversions(3.toShort())) return "fail: testIntegerConversions3"
    return "OK"
}
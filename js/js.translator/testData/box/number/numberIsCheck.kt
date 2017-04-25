// EXPECTED_REACHABLE_NODES: 495
package foo

// For now, check is Byte(is Short, is Int is Float, is Double) translates to typeof ... == "number"

fun testNum(numX: Number) {
    assertEquals(true, numX is Number, "numX is Number")
    assertEquals(true, numX is Int, "numX is Int")

    assertEquals(true, numX is Short, "numX is Short")
    assertEquals(true, numX is Byte, "numX is Byte")
    assertEquals(false, numX is Long, "numX is Long")

    assertEquals(true, numX is Double, "numX is Double")
    assertEquals(true, numX is Float, "numX is Float")
}

fun testAny(anyX: Any) {
    assertEquals(true, anyX is Number, "anyX is Number")
    assertEquals(true, anyX is Int, "anyX is Int")

    assertEquals(true, anyX is Short, "anyX is Short")
    assertEquals(true, anyX is Byte, "anyX is Byte")
    assertEquals(false, anyX is Long, "anyX is Long")

    assertEquals(true, anyX is Double, "anyX is Double")
    assertEquals(true, anyX is Float, "anyX is Float")
}

fun box(): String {
    val intX = 100

    assertEquals(true, intX is Number, "intX is Number")
    assertEquals(true, intX is Int, "intX is Int")

    var anyX: Any = "A"
    assertEquals(false, anyX is Number, "anyX is Number")

    testNum(100)
    testNum(100.toShort())
    testNum(100.toByte())
    testNum(100.0)
    testNum(100.0f)

    anyX = 100L
    assertEquals(true, anyX is Number, "anyX is Number")
    assertEquals(true, anyX is Long, "anyX is Long")
    assertEquals(false, anyX is Int, "anyX is Int")


    return "OK"
}
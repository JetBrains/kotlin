// EXPECTED_REACHABLE_NODES: 494
package foo

// CHECK_NOT_CALLED: isInstance

fun box(): String {
    assertEquals(true, isInstance<Short>(0.toShort()))
    assertEquals(true, isInstance<Byte>(0.toByte()))
    assertEquals(true, isInstance<Int>(0))
    assertEquals(true, isInstance<Long>(0.toLong()))
    assertEquals(true, isInstance<Double>(0.toDouble()))
    assertEquals(true, isInstance<Float>(0.toFloat()))

    assertEquals(true, isInstance<Number>(0))
    assertEquals(true, isInstance<Number>(0.toLong()))
    assertEquals(true, isInstance<Number>(0.0))
    assertEquals(false, isInstance<Number>("0"))

    assertEquals(true, isInstance<Int?>(0), "isInstance<Int?>(0)")
    assertEquals(true, isInstance<Int?>(null), "isInstance<Int?>(null)")
    assertEquals(false, isInstance<Int?>(true), "isInstance<Int?>(true)")

    return "OK"
}
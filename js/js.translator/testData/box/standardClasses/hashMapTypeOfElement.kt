// EXPECTED_REACHABLE_NODES: 826
package foo


fun box(): String {

    val x = true
    val y = false

    val mapWithIntKeys = HashMap<Int, Int>()
    mapWithIntKeys[1] = 1
    assertEquals("number", jsTypeOf (mapWithIntKeys.keys.iterator().next()), "mapWithIntKeys")

    val mapWithShortKeys = HashMap<Short, Int>()
    mapWithShortKeys[1.toShort()] = 1
    assertEquals("number", jsTypeOf (mapWithShortKeys.keys.iterator().next()), "mapWithShortKeys")

    val mapWithByteKeys = HashMap<Byte, Int>()
    mapWithByteKeys[1.toByte()] = 1
    assertEquals("number", jsTypeOf (mapWithByteKeys.keys.iterator().next()), "mapWithByteKeys")

    val mapWithDoubleKeys = HashMap<Double, Int>()
    mapWithDoubleKeys[1.0] = 1
    assertEquals("number", jsTypeOf (mapWithDoubleKeys.keys.iterator().next()), "mapWithDoubleKeys")

    mapWithDoubleKeys.clear()
    var dNaN = 0.0 / 0.0
    mapWithDoubleKeys[dNaN] = 100
    assertEquals(100, mapWithDoubleKeys[dNaN])
    assertEquals("number", jsTypeOf (mapWithDoubleKeys.keys.iterator().next()), "dNaN")

    mapWithDoubleKeys.clear()
    var dPositiveInfinity = +1.0 / 0.0
    mapWithDoubleKeys[dPositiveInfinity] = 100
    assertEquals(100, mapWithDoubleKeys[dPositiveInfinity])
    assertEquals("number", jsTypeOf (mapWithDoubleKeys.keys.iterator().next()), "dPositiveInfinity")

    mapWithDoubleKeys.clear()
    var dNegativeInfinity = -1.0 / 0.0
    mapWithDoubleKeys[dNegativeInfinity] = 100
    assertEquals(100, mapWithDoubleKeys[dNegativeInfinity])
    assertEquals("number", jsTypeOf (mapWithDoubleKeys.keys.iterator().next()), "dNegativeInfinity")

    val mapWithFloatKeys = HashMap<Float, Int>()
    mapWithFloatKeys[1.0f] = 1
    assertEquals("number", jsTypeOf (mapWithFloatKeys.keys.iterator().next()), "mapWithFloatKeys")

    mapWithFloatKeys.clear()
    var fNaN: Float = 0.0f / 0.0f
    mapWithFloatKeys[fNaN] = 100
    assertEquals(100, mapWithFloatKeys[fNaN])
    assertEquals("number", jsTypeOf (mapWithFloatKeys.keys.iterator().next()), "fNaN")

    mapWithFloatKeys.clear()
    var fPositiveInfinity = +1.0f / 0.0f
    mapWithFloatKeys[fPositiveInfinity] = 100
    assertEquals(100, mapWithFloatKeys[fPositiveInfinity])
    assertEquals("number", jsTypeOf (mapWithFloatKeys.keys.iterator().next()), "fPositiveInfinity")

    mapWithFloatKeys.clear()
    var NegativeInfinity = -1.0f / 0.0f
    mapWithFloatKeys[NegativeInfinity] = 100
    assertEquals(100, mapWithFloatKeys[NegativeInfinity])
    assertEquals("number", jsTypeOf (mapWithFloatKeys.keys.iterator().next()), "fNegativeInfinity")

    val mapWithCharKeys = HashMap<Char, Int>()
    mapWithCharKeys['A'] = 1
    assertEquals("object", jsTypeOf (mapWithCharKeys.keys.iterator().next()), "mapWithCharKeys")

    val mapWithLongKeys = HashMap<Long, Int>()
    mapWithLongKeys[1L] = 1
    assertEquals("object", jsTypeOf (mapWithLongKeys.keys.iterator().next()), "mapWithLongKeys")

    val mapWithBooleanKeys = HashMap<Boolean, Int>()
    mapWithBooleanKeys[true] = 1
    assertEquals("boolean", jsTypeOf (mapWithBooleanKeys.keys.iterator().next()), "mapWithBooleanKeys")

    val mapWithStringKeys = HashMap<String, Int>()
    mapWithStringKeys["key"] = 1
    assertEquals("string", jsTypeOf (mapWithStringKeys.keys.iterator().next()), "mapWithStringKeys")

    return "OK"
}


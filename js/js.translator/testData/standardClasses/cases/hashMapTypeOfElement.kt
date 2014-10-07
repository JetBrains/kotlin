package foo

import java.util.HashMap

native fun typeof(a: Any): String = noImpl

fun box(): String {

    val x = true
    val y = false

    val mapWithIntKeys = HashMap<Int, Int>()
    mapWithIntKeys[1] = 1
    assertEquals("number", typeof (mapWithIntKeys.keySet().iterator().next()), "mapWithIntKeys")

    val mapWithShortKeys = HashMap<Short, Int>()
    mapWithShortKeys[1: Short] = 1
    assertEquals("number", typeof (mapWithShortKeys.keySet().iterator().next()), "mapWithShortKeys")

    val mapWithByteKeys = HashMap<Byte, Int>()
    mapWithByteKeys[1: Byte] = 1
    assertEquals("number", typeof (mapWithByteKeys.keySet().iterator().next()), "mapWithByteKeys")

    val mapWithDoubleKeys = HashMap<Double, Int>()
    mapWithDoubleKeys[1.0] = 1
    assertEquals("number", typeof (mapWithDoubleKeys.keySet().iterator().next()), "mapWithDoubleKeys")

    mapWithDoubleKeys.clear()
    var dNaN = 0.0 / 0.0
    mapWithDoubleKeys[dNaN] = 100
    assertEquals(100, mapWithDoubleKeys[dNaN])
    assertEquals("number", typeof (mapWithDoubleKeys.keySet().iterator().next()), "dNaN")

    mapWithDoubleKeys.clear()
    var dPositiveInfinity = +1.0 / 0.0
    mapWithDoubleKeys[dPositiveInfinity] = 100
    assertEquals(100, mapWithDoubleKeys[dPositiveInfinity])
    assertEquals("number", typeof (mapWithDoubleKeys.keySet().iterator().next()), "dPositiveInfinity")

    mapWithDoubleKeys.clear()
    var dNegativeInfinity = -1.0 / 0.0
    mapWithDoubleKeys[dNegativeInfinity] = 100
    assertEquals(100, mapWithDoubleKeys[dNegativeInfinity])
    assertEquals("number", typeof (mapWithDoubleKeys.keySet().iterator().next()), "dNegativeInfinity")

    val mapWithFloatKeys = HashMap<Float, Int>()
    mapWithFloatKeys[1.0f] = 1
    assertEquals("number", typeof (mapWithFloatKeys.keySet().iterator().next()), "mapWithFloatKeys")

    mapWithFloatKeys.clear()
    var fNaN: Float = 0.0f / 0.0f
    mapWithFloatKeys[dNaN] = 100
    assertEquals(100, mapWithFloatKeys[dNaN])
    assertEquals("number", typeof (mapWithFloatKeys.keySet().iterator().next()), "fNaN")

    mapWithFloatKeys.clear()
    var fPositiveInfinity = +1.0f / 0.0f
    mapWithFloatKeys[fPositiveInfinity] = 100
    assertEquals(100, mapWithFloatKeys[fPositiveInfinity])
    assertEquals("number", typeof (mapWithFloatKeys.keySet().iterator().next()), "fPositiveInfinity")

    mapWithFloatKeys.clear()
    var NegativeInfinity = -1.0f / 0.0f
    mapWithFloatKeys[NegativeInfinity] = 100
    assertEquals(100, mapWithFloatKeys[NegativeInfinity])
    assertEquals("number", typeof (mapWithFloatKeys.keySet().iterator().next()), "fNegativeInfinity")

    val mapWithCharKeys = HashMap<Char, Int>()
    mapWithCharKeys['A'] = 1
    assertEquals("number", typeof (mapWithCharKeys.keySet().iterator().next()), "mapWithCharKeys")

    val mapWithLongKeys = HashMap<Long, Int>()
    mapWithLongKeys[1L] = 1
    assertEquals("number", typeof (mapWithLongKeys.keySet().iterator().next()), "mapWithLongKeys")

    val mapWithBooleanKeys = HashMap<Boolean, Int>()
    mapWithBooleanKeys[true] = 1
    assertEquals("boolean", typeof (mapWithBooleanKeys.keySet().iterator().next()), "mapWithBooleanKeys")

    val mapWithStringKeys = HashMap<String, Int>()
    mapWithStringKeys["key"] = 1
    assertEquals("string", typeof (mapWithStringKeys.keySet().iterator().next()), "mapWithStringKeys")

    return "OK"
}


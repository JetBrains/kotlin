// EXPECTED_REACHABLE_NODES: 841
package foo


fun box(): String {

    val x = true
    val y = false

    val intSet = HashSet<Int>()
    intSet.add(1)
    assertEquals("number", jsTypeOf (intSet.iterator().next()), "intSet")

    val shortSet = HashSet<Short>()
    shortSet.add(1.toShort())
    assertEquals("number", jsTypeOf (shortSet.iterator().next()), "shortSet")

    val byteSet = HashSet<Byte>()
    byteSet.add(1.toByte())
    assertEquals("number", jsTypeOf (byteSet.iterator().next()), "byteSet")

    val doubleSet = HashSet<Double>()
    doubleSet.add(1.0)
    assertEquals("number", jsTypeOf (doubleSet.iterator().next()), "doubleSet")

    doubleSet.clear()
    doubleSet.add(0.0 / 0.0)
    assertEquals("number", jsTypeOf (doubleSet.iterator().next()), "dNaN")

    doubleSet.clear()
    doubleSet.add(1.0 / 0.0)
    assertEquals("number", jsTypeOf (doubleSet.iterator().next()), "dPositiveInfinity")

    doubleSet.clear()
    doubleSet.add(-1.0 / 0.0)
    assertEquals("number", jsTypeOf (doubleSet.iterator().next()), "dNegativeInfinity")

    val floatSet = HashSet<Float>()
    floatSet.add(1.0f)
    assertEquals("number", jsTypeOf (floatSet.iterator().next()), "floatSet")

    floatSet.clear()
    floatSet.add(0.0f / 0.0f)
    assertEquals("number", jsTypeOf (floatSet.iterator().next()), "fNaN")

    floatSet.clear()
    floatSet.add(+1.0f / 0.0f)
    assertEquals("number", jsTypeOf (floatSet.iterator().next()), "fPositiveInfinity")

    floatSet.clear()
    floatSet.add(-1.0f / 0.0f)
    assertEquals("number", jsTypeOf (floatSet.iterator().next()), "fNegativeInfinity")

    val charSet = HashSet<Char>()
    charSet.add('A')
    assertEquals("object", jsTypeOf (charSet.iterator().next()), "charSet")

    val longSet = HashSet<Long>()
    longSet.add(1L)
    assertEquals("object", jsTypeOf (longSet.iterator().next()), "longSet")

    val booleanSet = HashSet<Boolean>()
    booleanSet.add(true)
    assertEquals("boolean", jsTypeOf (booleanSet.iterator().next()), "booleanSet")

    val stringSet = HashSet<String>()
    stringSet.add("text")
    assertEquals("string", jsTypeOf (stringSet.iterator().next()), "stringSet")

    return "OK"
}


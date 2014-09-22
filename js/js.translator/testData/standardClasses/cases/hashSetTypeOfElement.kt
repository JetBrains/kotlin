package foo

import java.util.HashSet

native fun typeof(a: Any): String = noImpl

fun box(): String {

    val x = true
    val y = false

    val intSet = HashSet<Int>()
    intSet.add(1)
    assertEquals("number", typeof (intSet.iterator().next()), "intSet")

    val shortSet = HashSet<Short>()
    shortSet.add(1: Short)
    assertEquals("number", typeof (shortSet.iterator().next()), "shortSet")

    val byteSet = HashSet<Byte>()
    byteSet.add(1: Byte)
    assertEquals("number", typeof (byteSet.iterator().next()), "byteSet")

    val doubleSet = HashSet<Double>()
    doubleSet.add(1.0)
    assertEquals("number", typeof (doubleSet.iterator().next()), "doubleSet")

    doubleSet.clear()
    doubleSet.add(0.0 / 0.0)
    assertEquals("number", typeof (doubleSet.iterator().next()), "dNaN")

    doubleSet.clear()
    doubleSet.add(1.0 / 0.0)
    assertEquals("number", typeof (doubleSet.iterator().next()), "dPositiveInfinity")

    doubleSet.clear()
    doubleSet.add(-1.0 / 0.0)
    assertEquals("number", typeof (doubleSet.iterator().next()), "dNegativeInfinity")

    val floatSet = HashSet<Float>()
    floatSet.add(1.0f)
    assertEquals("number", typeof (floatSet.iterator().next()), "floatSet")

    floatSet.clear()
    floatSet.add(0.0f / 0.0f)
    assertEquals("number", typeof (floatSet.iterator().next()), "fNaN")

    floatSet.clear()
    floatSet.add(+1.0f / 0.0f)
    assertEquals("number", typeof (floatSet.iterator().next()), "fPositiveInfinity")

    floatSet.clear()
    floatSet.add(-1.0f / 0.0f)
    assertEquals("number", typeof (floatSet.iterator().next()), "fNegativeInfinity")

    val charSet = HashSet<Char>()
    charSet.add('A')
    assertEquals("number", typeof (charSet.iterator().next()), "charSet")

    val longSet = HashSet<Long>()
    longSet.add(1L)
    assertEquals("number", typeof (longSet.iterator().next()), "longSet")

    val booleanSet = HashSet<Boolean>()
    booleanSet.add(true)
    assertEquals("boolean", typeof (booleanSet.iterator().next()), "booleanSet")

    val stringSet = HashSet<String>()
    stringSet.add("text")
    assertEquals("string", typeof (stringSet.iterator().next()), "stringSet")

    return "OK"
}


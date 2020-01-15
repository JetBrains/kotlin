// EXPECTED_REACHABLE_NODES: 1403
import kotlin.reflect.KClass

fun box(): String {
    check(js("Object"), "Any", Any::class)
    check(js("String"), "String", String::class)
    check(js("Boolean"), "Boolean", Boolean::class)
    check(js("Error"), "Throwable", Throwable::class)
    check(js("Array"), "Array", Array::class)
    check(js("Function"), "Function0", Function0::class)
    check(js("Function"), "Function1", Function1::class)

    check(js("Number"), "Byte", Byte::class)
    check(js("Number"), "Short", Short::class)
    check(js("Number"), "Int", Int::class)
    check(js("Number"), "Float", Float::class)
    check(js("Number"), "Double", Double::class)
    check(js("Number"), "Number", Number::class)

    check(js("Array"), "BooleanArray", BooleanArray::class)
    check(js("Uint16Array"), "CharArray", CharArray::class)
    check(js("Int8Array"), "ByteArray", ByteArray::class)
    check(js("Int16Array"), "ShortArray", ShortArray::class)
    check(js("Int32Array"), "IntArray", IntArray::class)
    check(js("Array"), "LongArray", LongArray::class)
    check(js("Float32Array"), "FloatArray", FloatArray::class)
    check(js("Float64Array"), "DoubleArray", DoubleArray::class)

    check(js("Object"), "Any", Any())
    check(js("String"), "String", "*")
    check(js("Boolean"), "Boolean", true)
    check(js("Error"), "Throwable", Throwable())
    check(js("Array"), "Array", arrayOf(1, 2, 3))
    check(js("Function"), "Function0", { -> 23 })
    check(js("Function"), "Function1", { x: Int -> x })

    check(js("Array"), "BooleanArray", booleanArrayOf())
    check(js("Uint16Array"), "CharArray", charArrayOf())
    check(js("Int8Array"), "ByteArray", byteArrayOf())
    check(js("Int16Array"), "ShortArray", shortArrayOf())
    check(js("Int32Array"), "IntArray", intArrayOf())
    check(js("Array"), "LongArray", longArrayOf())
    check(js("Float32Array"), "FloatArray", floatArrayOf())
    check(js("Float64Array"), "DoubleArray", doubleArrayOf())

    check(js("Number"), "Int", 23.toByte())
    check(js("Number"), "Int", 23.toShort())
    check(js("Number"), "Int", 23)
    check(js("Number"), "Int", 23.0)
    check(js("Number"), "Double", 23.1F)
    check(js("Number"), "Double", 23.2)

    check(js("Number"), "Int", Int::class)
    check(js("Number"), "Byte", Byte::class)
    check(js("Number"), "Double", Double::class)

    assertEquals("Long", Long::class.simpleName)
    assertEquals("Long", 23L::class.simpleName)
    if (testUtils.isLegacyBackend()) {
        assertEquals("BoxedChar", Char::class.simpleName)
        assertEquals("BoxedChar", '@'::class.simpleName)
    } else {
        assertEquals("Char", Char::class.simpleName)
        assertEquals("Char", '@'::class.simpleName)
    }
    assertEquals("RuntimeException", RuntimeException::class.simpleName)
    assertEquals("RuntimeException", RuntimeException()::class.simpleName)
    assertEquals("KClass", KClass::class.simpleName)
    assertEquals("KClass", Any::class::class.simpleName)
    assertEquals("KClass", Map::class::class.simpleName)

    try {
        assertEquals("Nothing", Nothing::class.simpleName)
        Nothing::class.js
        fail("Exception expected when trying to get JS class for Nothing type")
    }
    catch (e: UnsupportedOperationException) {
        // It's OK
    }

    return "OK"
}

private fun check(nativeClass: dynamic, simpleName: String, c: KClass<*>) {
    assertEquals(simpleName, c.simpleName, "Simple name of class has unexpected value")
    assertEquals(nativeClass, c.js, "Kotlin class does not correspond native class ${nativeClass.name}")
}

private fun check(nativeClass: dynamic, simpleName: String, value: Any) {
    check(nativeClass, simpleName, value::class)
    assertTrue(value::class.isInstance(value), "isInstance should return true for $simpleName")
}
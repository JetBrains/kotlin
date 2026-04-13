import kotlin.reflect.KClass

fun compileLongAsBigInt(): Boolean {
    val long = 1L
    return js("typeof long === 'bigint'").unsafeCast<Boolean>()
}

fun box(): String {
    check(js("Object"), "Any", Any::class, Any::class.js)
    check(js("String"), "String", String::class, String::class.js)
    check(js("Boolean"), "Boolean", Boolean::class, Boolean::class.js)
    check(js("Error"), "Throwable", Throwable::class, Throwable::class.js)
    check(js("Array"), "Array", Array::class, Array::class.js)
    check(js("Function"), "Function0", Function0::class, Function0::class.js)
    check(js("Function"), "Function1", Function1::class, Function1::class.js)

    check(js("Number"), "Byte", Byte::class, Byte::class.js)
    check(js("Number"), "Short", Short::class, Short::class.js)
    check(js("Number"), "Int", Int::class, Int::class.js)
    check(js("Number"), "Float", Float::class, Float::class.js)
    check(js("Number"), "Double", Double::class, Double::class.js)
    check(js("Number"), "Number", Number::class, Number::class.js)

    check(js("Array"), "BooleanArray", BooleanArray::class, BooleanArray::class.js)
    check(js("Uint16Array"), "CharArray", CharArray::class, CharArray::class.js)
    check(js("Int8Array"), "ByteArray", ByteArray::class, ByteArray::class.js)
    check(js("Int16Array"), "ShortArray", ShortArray::class, ShortArray::class.js)
    check(js("Int32Array"), "IntArray", IntArray::class, IntArray::class.js)

    if (compileLongAsBigInt()) {
        check(js("BigInt64Array"), "LongArray", LongArray::class, LongArray::class.js)
    } else {
        check(js("Array"), "LongArray", LongArray::class, LongArray::class.js)
    }

    check(js("Float32Array"), "FloatArray", FloatArray::class, FloatArray::class.js)
    check(js("Float64Array"), "DoubleArray", DoubleArray::class, DoubleArray::class.js)

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

    if (compileLongAsBigInt()) {
        check(js("BigInt64Array"), "LongArray", longArrayOf())
    } else {
        check(js("Array"), "LongArray", longArrayOf())
    }

    check(js("Float32Array"), "FloatArray", floatArrayOf())
    check(js("Float64Array"), "DoubleArray", doubleArrayOf())

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

    if (compileLongAsBigInt()) {
        check(js("BigInt64Array"), "LongArray", longArrayOf())
    } else {
        check(js("Array"), "LongArray", longArrayOf())
    }

    check(js("Float32Array"), "FloatArray", floatArrayOf())
    check(js("Float64Array"), "DoubleArray", doubleArrayOf())

    check(js("Number"), "Int", 23.toByte())
    check(js("Number"), "Int", 23.toShort())
    check(js("Number"), "Int", 23)
    check(js("Number"), "Int", 23.0)
    check(js("Number"), "Double", 23.1F)
    check(js("Number"), "Double", 23.2)

    // KT-84474
    if (compileLongAsBigInt()) {
        check(js("BigInt"), "Long", 23L)
    }
    check(js("Number"), "Int", Int::class, Int::class.js)
    check(js("Number"), "Byte", Byte::class, Int::class.js)
    check(js("Number"), "Double", Double::class, Int::class.js)

    // KT-84474
    if (compileLongAsBigInt()) {
        check(js("BigInt"), "Long", Long::class, Long::class.js)
    }

    // KT-84474
    if (js("typeof BigInt !== 'undefined'")) {
        val expecteClassName = when {
            compileLongAsBigInt() -> "Long"
            else -> "BigInt"
        }

        val maxLong = Long.MAX_VALUE.toString()

        check(js("BigInt"), expecteClassName, js("BigInt(23)").unsafeCast<Any>())
        check(js("BigInt"), "BigInt", (js("BigInt(maxLong)") * js("BigInt(2)")).unsafeCast<Any>())
    }

    assertEquals("Long", Long::class.simpleName)
    assertEquals("Long", 23L::class.simpleName)
    assertEquals("Char", Char::class.simpleName)
    assertEquals("Char", '@'::class.simpleName)
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

private fun check(nativeClass: dynamic, simpleName: String, c: KClass<*>, jsClass: JsClass<*>) {
    assertEquals(simpleName, c.simpleName, "Simple name of class has unexpected value")
    assertEquals(nativeClass, c.js, "Kotlin class does not correspond native class ${nativeClass?.name}")
    assertEquals(c.js, jsClass, "Native class (gotten by getKClass().js) does not correspond to native class gotten by optimization of ::class.js: ${nativeClass?.name}")
}

private fun check(nativeClass: dynamic, simpleName: String, value: Any) {
    check(nativeClass, simpleName, value::class, value::class.js)
    assertTrue(value::class.isInstance(value), "isInstance should return true for $simpleName")
}
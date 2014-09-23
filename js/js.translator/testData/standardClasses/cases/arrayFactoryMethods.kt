package foo

class Fail(val message: String) : Exception(message)
native fun eval(arg: String): Any? = noImpl

fun test(testName: String, actual: Any, expectedAsString: String) {
    val expected = eval("[$expectedAsString]")
    assertEquals(expected, actual)
}

fun box(): String {
    try {
        test("array", array(0, 1, 2, 3, 4), "0, 1, 2, 3, 4")
        test("booleanArray", booleanArray(true, false, false, true, true), "true, false, false, true, true")
        test("charArray'", charArray('0', '1', '2', '3', '4'), "'0', '1', '2', '3', '4'")
        test("byteArray", byteArray(0, 1, 2, 3, 4), "0, 1, 2, 3, 4")
        test("shortArray", shortArray(0, 1, 2, 3, 4), "0, 1, 2, 3, 4")
        test("intArray,", intArray(0, 1, 2, 3, 4), "0, 1, 2, 3, 4")
        test("longArray", longArray(0, 1, 2, 3, 4), "Kotlin.Long.fromInt(0), Kotlin.Long.fromInt(1), Kotlin.Long.fromInt(2), Kotlin.Long.fromInt(3), Kotlin.Long.fromInt(4)")
        test("floatArray", floatArray(0.0f, 1.0f, 2.0f, 3.0f, 4.0f), "0.0, 1.0, 2.0, 3.0, 4.0")
        test("doubleArray", doubleArray(0.0, 1.1, 2.2, 3.3, 4.4), "0.0, 1.1, 2.2, 3.3, 4.4")
    }
    catch (e: Fail) {
        return e.message
    }

    return "OK"
}

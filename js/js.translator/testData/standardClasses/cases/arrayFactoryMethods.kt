package foo

class Fail(message: String) : Exception(message)

fun test(testName: String, actual: Any, expectedAsString: String) {
    val expected = eval("[$expectedAsString]")
    assertEquals(expected, actual)
}

fun box(): String {
    try {
        test("arrayOf", arrayOf(0, 1, 2, 3, 4), "0, 1, 2, 3, 4")
        test("booleanArrayOf", booleanArrayOf(true, false, false, true, true), "true, false, false, true, true")
        test("charArray'", charArrayOf('0', '1', '2', '3', '4'), "'0', '1', '2', '3', '4'")
        test("byteArrayOf", byteArrayOf(0, 1, 2, 3, 4), "0, 1, 2, 3, 4")
        test("shortArrayOf", shortArrayOf(0, 1, 2, 3, 4), "0, 1, 2, 3, 4")
        test("intArray,", intArrayOf(0, 1, 2, 3, 4), "0, 1, 2, 3, 4")
        test("longArrayOf", longArrayOf(0, 1, 2, 3, 4), "Kotlin.Long.fromInt(0), Kotlin.Long.fromInt(1), Kotlin.Long.fromInt(2), Kotlin.Long.fromInt(3), Kotlin.Long.fromInt(4)")
        test("floatArrayOf", floatArrayOf(0.0f, 1.0f, 2.0f, 3.0f, 4.0f), "0.0, 1.0, 2.0, 3.0, 4.0")
        test("doubleArrayOf", doubleArrayOf(0.0, 1.1, 2.2, 3.3, 4.4), "0.0, 1.1, 2.2, 3.3, 4.4")
    }
    catch (e: Fail) {
        return e.message!!
    }

    return "OK"
}

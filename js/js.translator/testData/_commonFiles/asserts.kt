package kotlin

fun fail(message: String? = null) = throw Exception(message)

fun assertEquals<T>(expected: T, actual: T, message: String? = null) {
    if (expected != actual) {
        val msg = if (message == null) "" else (" message = '" + message + "',")
        fail("Unexpected value:$msg expected = '$expected', actual = '$actual'")
    }
}

fun assertNotEquals<T>(illegal: T, actual: T, message: String? = null) {
    if (illegal == actual) {
        val msg = if (message == null) "" else (" message = '" + message + "',")
        fail("Illegal value:$msg illegal = '$illegal', actual = '$actual'")
    }
}

fun assertTrue(actual: Boolean, message: String? = null) = assertEquals(true, actual, message)

fun assertFalse(actual: Boolean, message: String? = null) = assertEquals(false, actual, message)

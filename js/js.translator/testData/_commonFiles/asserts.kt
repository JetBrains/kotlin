package kotlin
// This file should be excluded from tests using StdLib, as these methods conflict with corresponding methods from kotlin.test
// see StdLibTestBase.removeAdHocAssertions

fun fail(message: String? = null): Nothing = js("throw new Error(message)")

fun <T> assertEquals(expected: T, actual: T, message: String? = null) {
    if (expected != actual) {
        val msg = if (message == null) "" else (" message = '" + message + "',")
        fail("Unexpected value:$msg expected = '$expected', actual = '$actual'")
    }
}

fun <T> assertNotEquals(illegal: T, actual: T, message: String? = null) {
    if (illegal == actual) {
        val msg = if (message == null) "" else (" message = '" + message + "',")
        fail("Illegal value:$msg illegal = '$illegal', actual = '$actual'")
    }
}

fun assertTrue(actual: Boolean, message: String? = null) = assertEquals(true, actual, message)

fun assertFalse(actual: Boolean, message: String? = null) = assertEquals(false, actual, message)

fun testTrue(f: () -> Boolean) {
    assertTrue(f(), f.toString())
}

fun testFalse(f: () -> Boolean) {
    assertFalse(f(), f.toString())
}

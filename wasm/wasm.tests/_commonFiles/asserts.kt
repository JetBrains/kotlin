package kotlin
// This file should be excluded from tests using StdLib, as these methods conflict with corresponding methods from kotlin.test
// see StdLibTestBase.removeAdHocAssertions

fun <T> assertEquals(expected: T, actual: T, message: String? = null) {
    if (expected != actual) {
        val msg = if (message == null) "" else ", message = '$message'"
        fail("Unexpected value: expected = '$expected', actual = '$actual'$msg")
    }
}

fun <T> assertNotEquals(illegal: T, actual: T, message: String? = null) {
    if (illegal == actual) {
        val msg = if (message == null) "" else ", message = '$message'"
        fail("Illegal value: illegal = '$illegal', actual = '$actual'$msg")
    }
}

fun <T> assertSame(expected: T, actual: T, message: String? = null) {
    if (expected !== actual) {
        val msg = if (message == null) "" else ", message = '$message'"
        fail("Expected same instances: expected = '$expected', actual = '$actual'$msg")
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

fun assertFails(block: () -> Unit): Throwable {
    try {
        block()
    } catch (t: Throwable) {
        return t
    }
    fail("Expected an exception to be thrown, but was completed successfully.")
}
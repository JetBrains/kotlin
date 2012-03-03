/**
 * A number of helper methods for writing Kool unit tests
 */
package kotlin.test

import org.junit.Assert
import junit.framework.TestCase

/** Asserts that the given block returns true */
inline fun assertTrue(message: String, block: ()-> Boolean) {
    val actual = block()
    Assert.assertTrue(message, actual)
}

/** Asserts that the given block returns true */
inline fun assertTrue(block: ()-> Boolean) = assertTrue(block.toString(), block)

/** Asserts that the given block returns false */
inline fun assertNot(message: String, block: ()-> Boolean) {
    assertTrue(message){ !block() }
}

/** Asserts that the given block returns true */
inline fun assertNot(block: ()-> Boolean) = assertNot(block.toString(), block)

/** Asserts that the expression is true with an optional message */
inline fun assertTrue(actual: Boolean, message: String = "") {
    return assertEquals(true, actual, message)
}

/** Asserts that the expression is false with an optional message */
inline fun assertFalse(actual: Boolean, message: String = "") {
    return assertEquals(false, actual, message)
}

/** Asserts that the expected value is equal to the actual value, with an optional message */
inline fun assertEquals(expected: Any?, actual: Any?, message: String = "") {
    Assert.assertEquals(message, expected, actual)
}

/** Asserts that the expression is not null, with an optional message */
inline fun assertNotNull(actual: Any?, message: String = "") {
    Assert.assertNotNull(message, actual)
}

/** Asserts that the expression is null, with an optional message */
inline fun assertNull(actual: Any?, message: String = "") {
    Assert.assertNull(message, actual)
}

/** Marks a test as having failed if this point in the execution path is reached, with an optional message */
inline fun fail(message: String = "") {
    Assert.fail(message)
}

/** Asserts that given function block returns the given expected value */
inline fun <T> expect(expected: T, block: ()-> T) {
    expect(expected, block.toString(), block)
}

/** Asserts that given function block returns the given expected value and use the given message if it fails */
inline fun <T> expect(expected: T, message: String, block: ()-> T) {
    val actual = block()
    assertEquals(expected, actual, message)
}

/** Asserts that given function block fails by throwing an exception */
fun fails(block: ()-> Unit): Exception? {
    try {
        block()
        Assert.fail("Expected an exception to be thrown")
        return null
    } catch (e: Exception) {
        println("Caught excepted exception: $e")
        return e
    }
}

/** Asserts that a block fails with a specific exception being thrown */
fun <T: Exception> failsWith(block: ()-> Unit) {
    try {
        block()
        Assert.fail("Expected an exception to be thrown")
    } catch (e: T) {
        println("Caught excepted exception: $e")
        // OK
    }
}

/**
 * Comments out a block of test code until it is implemented while keeping a link to the code
 * to implement in your unit test output
 */
inline fun todo(block: ()-> Any) {
    println("TODO at " + (Exception() as java.lang.Throwable).getStackTrace()?.get(1) + " for " + block)
}


/**
 * Useful base class for test cases using the old JUnit 3 naming convention of functions
 * starting with "test*" as being a test case
 */
abstract class TestSupport() : TestCase() {
}

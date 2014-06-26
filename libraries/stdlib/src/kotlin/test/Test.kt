/**
 * A number of helper methods for writing Kool unit tests
 */
package kotlin.test

// TODO should not need this - its here for the JS stuff
import java.lang.IllegalStateException

/** Asserts that the given block returns true */
public inline fun assertTrue(message: String, block: ()-> Boolean) {
    val actual = block()
    asserter.assertTrue(message, actual)
}

/** Asserts that the given block returns true */
public inline fun assertTrue(block: ()-> Boolean) : Unit = assertTrue("expected true", block)

/** Asserts that the given block returns false */
public inline fun assertNot(message: String, block: ()-> Boolean) {
    assertTrue(message){ !block() }
}

/** Asserts that the given block returns false */
public fun assertNot(block: ()-> Boolean) : Unit = assertNot("expected false", block)

/** Asserts that the expression is true with an optional message */
public fun assertTrue(actual: Boolean, message: String = "") {
    return assertEquals(true, actual, message)
}

/** Asserts that the expression is false with an optional message */
public fun assertFalse(actual: Boolean, message: String = "") {
    return assertEquals(false, actual, message)
}

/** Asserts that the expected value is equal to the actual value, with an optional message */
public fun assertEquals(expected: Any?, actual: Any?, message: String = "") {
    asserter.assertEquals(message, expected, actual)
}

/** Asserts that the expression is not null, with an optional message */
public fun <T> assertNotNull(actual: T?, message: String = ""): T {
    asserter.assertNotNull(message, actual)
    return actual!!
}

//TODO merge with next via default (currently inline functions do not support default values)
/** Asserts that the expression is not null, with an optional message and a function block to process the not-null value */
public inline fun <T, R> assertNotNull(actual: T?, block: (T) -> R) {
    assertNotNull(actual, "", block)
}

/** Asserts that the expression is not null, with an optional message and a function block to process the not-null value */
public inline fun <T, R> assertNotNull(actual: T?, message: String, block: (T) -> R) {
    asserter.assertNotNull(message, actual)
    if (actual != null) {
        block(actual)
    }
}

/** Asserts that the expression is null, with an optional message */
public fun assertNull(actual: Any?, message: String = "") {
    asserter.assertNull(message, actual)
}

/** Marks a test as having failed if this point in the execution path is reached, with an optional message */
public fun fail(message: String = "") {
    asserter.fail(message)
}

/** Asserts that given function block returns the given expected value */
public inline fun <T> expect(expected: T, block: ()-> T) {
    expect(expected, "expected " + expected, block)
}

/** Asserts that given function block returns the given expected value and use the given message if it fails */
public inline fun <T> expect(expected: T, message: String, block: ()-> T) {
    val actual = block()
    assertEquals(expected, actual, message)
}

/** Asserts that given function block fails by throwing an exception */
public fun fails(block: ()-> Unit): Throwable? {
    try {
        block()
        asserter.fail("Expected an exception to be thrown")
        return null
    } catch (e: Throwable) {
        //println("Caught expected exception: $e")
        return e
    }
}

/**
 * A plugin for performing assertions which can reuse JUnit or TestNG
 */
public trait Asserter {
    public fun assertTrue(message: String, actual: Boolean): Unit

    public fun assertEquals(message: String, expected: Any?, actual: Any?): Unit

    public fun assertNotNull(message: String, actual: Any?): Unit

    public fun assertNull(message: String, actual: Any?): Unit

    public fun fail(message: String): Unit
}

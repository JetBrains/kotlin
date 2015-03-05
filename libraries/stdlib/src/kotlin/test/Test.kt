/**
 * A number of helper methods for writing unit tests
 */
package kotlin.test

/** Asserts that the given block returns true */
public inline fun assertTrue(message: String, block: () -> Boolean) {
    val actual = block()
    asserter.assertTrue(message, actual)
}

/** Asserts that the given block returns true */
public inline fun assertTrue(block: () -> Boolean): Unit = assertTrue("expected true", block)

/** Asserts that the given block returns false */
public inline fun assertNot(message: String, block: () -> Boolean) {
    assertTrue(message) { !block() }
}

/** Asserts that the given block returns false */
public fun assertNot(block: () -> Boolean): Unit = assertNot("expected false", block)

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

/** Asserts that the actual value is not equal to the illegal value, with an optional message */
public fun assertNotEquals(illegal: Any?, actual: Any?, message: String = "") {
    asserter.assertNotEquals(message, illegal, actual)
}

/** Asserts that the expression is not null, with an optional message */
public fun <T : Any> assertNotNull(actual: T?, message: String = ""): T {
    asserter.assertNotNull(message, actual)
    return actual!!
}

/** Asserts that the expression is not null, with an optional message and a function block to process the not-null value */
public inline fun <T : Any, R> assertNotNull(actual: T?, message: String = "", block: (T) -> R) {
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
public inline fun <T> expect(expected: T, block: () -> T) {
    expect(expected, "expected " + expected, block)
}

/** Asserts that given function block returns the given expected value and use the given message if it fails */
public inline fun <T> expect(expected: T, message: String, block: () -> T) {
    val actual = block()
    assertEquals(expected, actual, message)
}

/** Asserts that given function block fails by throwing an exception */
public fun fails(block: () -> Unit): Throwable? {
    var thrown: Throwable? = null
    try {
        block()
    } catch (e: Throwable) {
        thrown = e
    }
    if (thrown == null)
        asserter.fail("Expected an exception to be thrown")
    return thrown
}

/**
 * Abstracts the logic for performing assertions. Specific implementations of [Asserter] can use JUnit
 * or TestNG assertion facilities.
 */
public trait Asserter {
    /**
     * Asserts that the specified value is true.
     *
     * @param message the message to report if the assertion fails.
     */
    public fun assertTrue(message: String, actual: Boolean): Unit

    /**
     * Asserts that the specified values are equal.
     *
     * @param message the message to report if the assertion fails.
     */
    public fun assertEquals(message: String, expected: Any?, actual: Any?): Unit

    /**
     * Asserts that the specified values are not equal.
     *
     * @param message the message to report if the assertion fails.
     */
    public fun assertNotEquals(message: String, illegal: Any?, actual: Any?): Unit

    /**
     * Asserts that the specified value is not null.
     *
     * @param message the message to report if the assertion fails.
     */
    public fun assertNotNull(message: String, actual: Any?): Unit

    /**
     * Asserts that the specified value is null.
     *
     * @param message the message to report if the assertion fails.
     */
    public fun assertNull(message: String, actual: Any?): Unit

    /**
     * Fails the current test with the specified message.
     *
     * @param message the message to report.
     */
    public fun fail(message: String): Unit
}

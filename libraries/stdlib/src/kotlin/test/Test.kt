@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("TestAssertionsKt")
/**
 * A number of helper methods for writing unit tests.
 */
package kotlin.test


/** Asserts that the given [block] returns `false`. */
@Deprecated("Use assertFalse instead.", ReplaceWith("assertFalse(message, block)"))
public fun assertNot(message: String, block: () -> Boolean): Unit = assertFalse(message, block)

/** Asserts that the given [block] returns `false`. */
@Deprecated("Use assertFalse instead.", ReplaceWith("assertFalse(null, block)"))
public fun assertNot(block: () -> Boolean): Unit = assertFalse(block = block)

/** Asserts that the given [block] returns `true`. */
public fun assertTrue(message: String? = null, block: () -> Boolean): Unit = assertTrue(block(), message)

/** Asserts that the expression is `true` with an optional [message]. */
public fun assertTrue(actual: Boolean, message: String? = null) {
    return asserter.assertTrue(message ?: "Expected value to be true.", actual)
}

/** Asserts that the given [block] returns `false`. */
public fun assertFalse(message: String? = null, block: () -> Boolean): Unit = assertFalse(block(), message)

/** Asserts that the expression is `false` with an optional [message]. */
public fun assertFalse(actual: Boolean, message: String? = null) {
    return asserter.assertTrue(message ?: "Expected value to be false.", !actual)
}

/** Asserts that the [expected] value is equal to the [actual] value, with an optional [message]. */
public fun assertEquals(expected: Any?, actual: Any?, message: String? = null) {
    asserter.assertEquals(message, expected, actual)
}

/** Asserts that the [actual] value is not equal to the illegal value, with an optional [message]. */
public fun assertNotEquals(illegal: Any?, actual: Any?, message: String? = null) {
    asserter.assertNotEquals(message, illegal, actual)
}

/** Asserts that the [actual] value is not `null`, with an optional [message]. */
public fun <T : Any> assertNotNull(actual: T?, message: String? = null): T {
    asserter.assertNotNull(message, actual)
    return actual!!
}

/** Asserts that the [actual] value is not `null`, with an optional [message] and a function [block] to process the not-null value. */
public fun <T : Any, R> assertNotNull(actual: T?, message: String? = null, block: (T) -> R) {
    asserter.assertNotNull(message, actual)
    if (actual != null) {
        block(actual)
    }
}

/** Asserts that the [actual] value is `null`, with an optional [message]. */
public fun assertNull(actual: Any?, message: String? = null) {
    asserter.assertNull(message, actual)
}

/** Marks a test as having failed if this point in the execution path is reached, with an optional [message]. */
public fun fail(message: String? = null) {
    asserter.fail(message)
}

/** Asserts that given function [block] returns the given [expected] value. */
public fun <T> expect(expected: T, block: () -> T) {
    assertEquals(expected, block())
}

/** Asserts that given function [block] returns the given [expected] value and use the given [message] if it fails. */
public fun <T> expect(expected: T, message: String?, block: () -> T) {
    assertEquals(expected, block(), message)
}

@Deprecated("Use assertFails instead.", ReplaceWith("assertFails(block)"))
public fun fails(block: () -> Unit): Throwable? = assertFails(block)

/** Asserts that given function [block] fails by throwing an exception. */
public fun assertFails(block: () -> Unit): Throwable? {
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
public interface Asserter {
    /**
     * Fails the current test with the specified message.
     *
     * @param message the message to report.
     */
    public fun fail(message: String?): Unit

    /**
     * Asserts that the specified value is `true`.
     *
     * @param lazyMessage the function to return a message to report if the assertion fails.
     */
    public fun assertTrue(lazyMessage: () -> String?, actual: Boolean): Unit {
        if (!actual) {
            fail(lazyMessage())
        }
    }

    /**
     * Asserts that the specified value is `true`.
     *
     * @param message the message to report if the assertion fails.
     */
    public fun assertTrue(message: String?, actual: Boolean): Unit {
        assertTrue({message}, actual)
    }

    /**
     * Asserts that the specified values are equal.
     *
     * @param message the message to report if the assertion fails.
     */
    public fun assertEquals(message: String?, expected: Any?, actual: Any?): Unit {
        assertTrue({ (message?.let { "$it. " } ?: "") + "Expected <$expected>, actual <$actual>." }, actual == expected)
    }

    /**
     * Asserts that the specified values are not equal.
     *
     * @param message the message to report if the assertion fails.
     */
    public fun assertNotEquals(message: String?, illegal: Any?, actual: Any?): Unit {
        assertTrue({ (message?.let { "$it. " } ?: "") + "Illegal value: <$actual>." }, actual != illegal)
    }

    /**
     * Asserts that the specified value is `null`.
     *
     * @param message the message to report if the assertion fails.
     */
    public fun assertNull(message: String?, actual: Any?): Unit {
        assertTrue({ (message?.let { "$it. " } ?: "") + "Expected value to be null, but was: <$actual>." }, actual == null)
    }

    /**
     * Asserts that the specified value is not `null`.
     *
     * @param message the message to report if the assertion fails.
     */
    public fun assertNotNull(message: String?, actual: Any?): Unit {
        assertTrue({ (message?.let { "$it. " } ?: "") + "Expected value to be not null." }, actual != null)
    }

}

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("AssertionsKt")

/**
 * A number of helper methods for writing unit tests.
 */
package kotlin.test

import kotlin.internal.*


/** Asserts that the given [block] returns `false`. */
@Deprecated("Use assertFalse instead.", ReplaceWith("assertFalse(message, block)"), kotlin.DeprecationLevel.ERROR)
fun assertNot(message: String, block: () -> Boolean): Unit = assertFalse(message, block)

/** Asserts that the given [block] returns `false`. */
@Deprecated("Use assertFalse instead.", ReplaceWith("assertFalse(null, block)"), kotlin.DeprecationLevel.ERROR)
fun assertNot(block: () -> Boolean): Unit = assertFalse(block = block)

/** Asserts that the given [block] returns `true`. */
fun assertTrue(message: String? = null, block: () -> Boolean): Unit = assertTrue(block(), message)

/** Asserts that the expression is `true` with an optional [message]. */
fun assertTrue(actual: Boolean, message: String? = null) {
    return asserter.assertTrue(message ?: "Expected value to be true.", actual)
}

/** Asserts that the given [block] returns `false`. */
fun assertFalse(message: String? = null, block: () -> Boolean): Unit = assertFalse(block(), message)

/** Asserts that the expression is `false` with an optional [message]. */
fun assertFalse(actual: Boolean, message: String? = null) {
    return asserter.assertTrue(message ?: "Expected value to be false.", !actual)
}

/** Asserts that the [expected] value is equal to the [actual] value, with an optional [message]. */
fun <@OnlyInputTypes T> assertEquals(expected: T, actual: T, message: String? = null) {
    asserter.assertEquals(message, expected, actual)
}

/** Asserts that the [actual] value is not equal to the illegal value, with an optional [message]. */
fun <@OnlyInputTypes T> assertNotEquals(illegal: T, actual: T, message: String? = null) {
    asserter.assertNotEquals(message, illegal, actual)
}

/** Asserts that the [actual] value is not `null`, with an optional [message]. */
fun <T : Any> assertNotNull(actual: T?, message: String? = null): T {
    asserter.assertNotNull(message, actual)
    return actual!!
}

/** Asserts that the [actual] value is not `null`, with an optional [message] and a function [block] to process the not-null value. */
fun <T : Any, R> assertNotNull(actual: T?, message: String? = null, block: (T) -> R) {
    asserter.assertNotNull(message, actual)
    if (actual != null) {
        block(actual)
    }
}

/** Asserts that the [actual] value is `null`, with an optional [message]. */
fun assertNull(actual: Any?, message: String? = null) {
    asserter.assertNull(message, actual)
}

/** Marks a test as having failed if this point in the execution path is reached, with an optional [message]. */
fun fail(message: String? = null) {
    asserter.fail(message)
}

/** Asserts that given function [block] returns the given [expected] value. */
fun <@OnlyInputTypes T> expect(expected: T, block: () -> T) {
    assertEquals(expected, block())
}

/** Asserts that given function [block] returns the given [expected] value and use the given [message] if it fails. */
fun <@OnlyInputTypes T> expect(expected: T, message: String?, block: () -> T) {
    assertEquals(expected, block(), message)
}

@Deprecated("Use assertFails instead.", ReplaceWith("assertFails(block)"), kotlin.DeprecationLevel.ERROR)
fun fails(block: () -> Unit): Throwable? = assertFails(block)

/** Asserts that given function [block] fails by throwing an exception. */
fun assertFails(block: () -> Unit): Throwable? {
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
interface Asserter {
    /**
     * Fails the current test with the specified message.
     *
     * @param message the message to report.
     */
    fun fail(message: String?): Unit

    /**
     * Asserts that the specified value is `true`.
     *
     * @param lazyMessage the function to return a message to report if the assertion fails.
     */
    fun assertTrue(lazyMessage: () -> String?, actual: Boolean): Unit {
        if (!actual) {
            fail(lazyMessage())
        }
    }

    /**
     * Asserts that the specified value is `true`.
     *
     * @param message the message to report if the assertion fails.
     */
    fun assertTrue(message: String?, actual: Boolean): Unit {
        assertTrue({ message }, actual)
    }

    /**
     * Asserts that the specified values are equal.
     *
     * @param message the message to report if the assertion fails.
     */
    fun assertEquals(message: String?, expected: Any?, actual: Any?): Unit {
        assertTrue({ (message?.let { "$it. " } ?: "") + "Expected <$expected>, actual <$actual>." }, actual == expected)
    }

    /**
     * Asserts that the specified values are not equal.
     *
     * @param message the message to report if the assertion fails.
     */
    fun assertNotEquals(message: String?, illegal: Any?, actual: Any?): Unit {
        assertTrue({ (message?.let { "$it. " } ?: "") + "Illegal value: <$actual>." }, actual != illegal)
    }

    /**
     * Asserts that the specified value is `null`.
     *
     * @param message the message to report if the assertion fails.
     */
    fun assertNull(message: String?, actual: Any?): Unit {
        assertTrue({ (message?.let { "$it. " } ?: "") + "Expected value to be null, but was: <$actual>." }, actual == null)
    }

    /**
     * Asserts that the specified value is not `null`.
     *
     * @param message the message to report if the assertion fails.
     */
    fun assertNotNull(message: String?, actual: Any?): Unit {
        assertTrue({ (message?.let { "$it. " } ?: "") + "Expected value to be not null." }, actual != null)
    }

}

/**
 * Checks applicability and provides Asserter instance
 */
interface AsserterContributor {
    /**
     * Provides [Asserter] instance or `null` depends on the current context.
     *
     * @return asserter instance or null if it is not applicable now
     */
    fun contribute(): Asserter?
}


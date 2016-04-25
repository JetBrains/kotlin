package kotlin.test

/**
 * Comments out a block of test code until it is implemented while keeping a link to the code
 * to implement in your unit test output
 */
public fun todo(block: () -> Any) {
    // println("TODO at " + (Exception() as java.lang.Throwable).getStackTrace()?.get(1) + " for " + block)
    println("TODO at " + block)
}

/** Asserts that a [block] fails with a specific exception of type [T] being thrown.
 *  Since inline method doesn't allow to trace where it was invoked, it is required to pass a [message] to distinguish this method call from others.
 */
@kotlin.internal.InlineOnly
inline fun <reified T : Throwable> assertFailsWith(message: String? = null, noinline block: () -> Unit): T {
    val exception = assertFails(block) // TODO: message (in 1.1)
    assertTrue(exception is T, (message?.let { "$it. " } ?: "") + "An exception thrown is not of the expected type: $exception")
    return exception as T
}

/**
 * Provides the JS implementation of asserter using [QUnit](http://QUnitjs.com/)
 */
public var asserter: Asserter = QUnitAsserter()

public class QUnitAsserter() : Asserter {

    public override fun assertTrue(lazyMessage: () -> String?, actual: Boolean) {
        assertTrue(actual, lazyMessage())
    }

    public override fun assertTrue(message: String?, actual: Boolean) {
        QUnit.ok(actual, message)
        if (!actual) failWithMessage(message)
    }

    public override fun fail(message: String?): Nothing {
        QUnit.ok(false, message)
        failWithMessage(message)
    }

    private fun failWithMessage(message: String?): Nothing {
        if (message == null)
            throw AssertionError()
        else
            throw AssertionError(message)
    }
}
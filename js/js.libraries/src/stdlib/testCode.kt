package kotlin.test

/**
 * Comments out a block of test code until it is implemented while keeping a link to the code
 * to implement in your unit test output
 */
public fun todo(block: () -> Any) {
    // println("TODO at " + (Exception() as java.lang.Throwable).getStackTrace()?.get(1) + " for " + block)
    println("TODO at " + block)
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
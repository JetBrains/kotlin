package kotlin.test

/**
 * Default [Asserter] implementation to avoid dependency on JUnit or TestNG.
 */
class DefaultAsserter() : Asserter {

    override fun fail(message: String?): Nothing {
        if (message == null)
            throw AssertionError()
        else
            throw AssertionError(message)
    }
}
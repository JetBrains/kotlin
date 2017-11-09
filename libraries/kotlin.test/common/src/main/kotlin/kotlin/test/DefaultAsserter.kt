package kotlin.test

/**
 * Default [Asserter] implementation to avoid dependency on JUnit or TestNG.
 */
// TODO: make object in 1.2
class DefaultAsserter : Asserter {
    override fun fail(message: String?): Nothing {
        if (message == null)
            throw AssertionError()
        else
            throw AssertionError(message)
    }
}
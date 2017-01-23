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

header fun AssertionError(message: String): Throwable
header fun AssertionError(): Throwable

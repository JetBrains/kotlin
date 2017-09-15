package kotlin.test

/**
 * Default [Asserter] implementation for Kotlin/Native.
 */
object DefaultAsserter : Asserter {
    override fun fail(message: String?): Nothing {
        if (message == null)
            throw AssertionError()
        else
            throw AssertionError(message)
    }
}
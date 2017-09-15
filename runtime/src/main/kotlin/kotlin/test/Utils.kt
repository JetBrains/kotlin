package kotlin.test

@PublishedApi
internal fun messagePrefix(message: String?) = if (message == null) "" else "$message. "

internal fun lookupAsserter(): Asserter = DefaultAsserter

@PublishedApi // required to get stable name as it's called from box tests
internal fun overrideAsserter(value: Asserter?): Asserter? = _asserter.also { _asserter = value }
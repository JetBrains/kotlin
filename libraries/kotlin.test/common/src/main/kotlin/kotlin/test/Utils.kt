package kotlin.test

internal fun messagePrefix(message: String?) = if (message == null) "" else "$message. "
internal expect fun lookupAsserter(): Asserter

@PublishedApi // required to get stable name as it's called from box tests
internal fun overrideAsserter(value: Asserter?): Asserter? {
    // TODO: Replace with return _asserter.also { _asserter = value } after KT-17540 is fixed
    val previous = _asserter
    _asserter = value
    return previous
}
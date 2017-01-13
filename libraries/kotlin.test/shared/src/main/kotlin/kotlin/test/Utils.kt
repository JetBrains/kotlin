package kotlin.test

internal fun messagePrefix(message: String?) = if (message == null) "" else "$message. "
internal header fun lookupAsserter(): Asserter
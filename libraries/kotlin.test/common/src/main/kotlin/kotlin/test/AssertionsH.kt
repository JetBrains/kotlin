package kotlin.test

import kotlin.reflect.KClass

/**
 * Comments out a block of test code until it is implemented while keeping a link to the code
 * to implement in your unit test output
 */
expect fun todo(block: () -> Unit)

/** Asserts that a [block] fails with a specific exception of type [exceptionClass] being thrown. */
expect fun <T : Throwable> assertFailsWith(exceptionClass: KClass<T>, message: String?, block: () -> Unit): T

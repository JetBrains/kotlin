@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("AssertionsKt")
package kotlin.test

import kotlin.reflect.*

/** Asserts that a [block] fails with a specific exception being thrown. */
private fun <T : Throwable> assertFailsWithImpl(exceptionClass: Class<T>, message: String?, block: () -> Unit): T {
    try {
        block()
    } catch (e: Throwable) {
        if (exceptionClass.isInstance(e)) {
            @Suppress("UNCHECKED_CAST")
            return e as T
        }
        asserter.fail((message?.let { "$it. " } ?: "") + "Expected an exception of type $exceptionClass to be thrown, but was $e")
    }
    val msg = message?.let { "$it. " } ?: ""
    asserter.fail(msg + "Expected an exception of type $exceptionClass to be thrown, but was completed successfully.")
}

/** Asserts that a [block] fails with a specific exception of type [exceptionClass] being thrown. */
fun <T : Throwable> assertFailsWith(exceptionClass: KClass<T>, block: () -> Unit): T = assertFailsWith(exceptionClass, null, block)

/** Asserts that a [block] fails with a specific exception of type [exceptionClass] being thrown. */
fun <T : Throwable> assertFailsWith(exceptionClass: KClass<T>, message: String?, block: () -> Unit): T = assertFailsWithImpl(exceptionClass.java, message, block)

/** Asserts that a [block] fails with a specific exception of type [T] being thrown.
 *  Since inline method doesn't allow to trace where it was invoked, it is required to pass a [message] to distinguish this method call from others.
 */
@kotlin.internal.InlineOnly
inline fun <reified T : Throwable> assertFailsWith(message: String? = null, noinline block: () -> Unit): T = assertFailsWith(T::class, message, block)


/**
 * Comments out a [block] of test code until it is implemented while keeping a link to the code
 * to implement in your unit test output
 */
inline fun todo(@Suppress("UNUSED_PARAMETER") block: () -> Unit) {
    System.out.println("TODO at " + currentStackTrace()[1])
}

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN", "NOTHING_TO_INLINE")
inline fun currentStackTrace() = (java.lang.Exception() as java.lang.Throwable).stackTrace

/**
 * The active implementation of [Asserter]. An implementation of [Asserter] can be provided
 * using the [Java service loader](http://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html) mechanism.
 */
val asserter: Asserter
    get() = lookup()


@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("TestAssertionsKt")
package kotlin.test

import kotlin.jvm.internal.*
import kotlin.reflect.*

@Deprecated("Use assertFailsWith instead.", ReplaceWith("assertFailsWith(exceptionClass, block)"), kotlin.DeprecationLevel.ERROR)
fun <T : Throwable> failsWith(exceptionClass: Class<T>, block: () -> Any): T = assertFailsWith(exceptionClass, { block() })

/** Asserts that a [block] fails with a specific exception being thrown. */
fun <T : Throwable> assertFailsWith(exceptionClass: Class<T>, block: () -> Unit): T = assertFailsWith(exceptionClass, null, block)

/** Asserts that a [block] fails with a specific exception being thrown. */
fun <T : Throwable> assertFailsWith(exceptionClass: Class<T>, message: String?, block: () -> Unit): T {
    try {
        block()
    } catch (e: Throwable) {
        if (exceptionClass.isInstance(e)) {
            @Suppress("UNCHECKED_CAST")
            return e as T
        }
        asserter.fail((message?.let { "$it. " } ?: "") + "Expected an exception of type $exceptionClass to be thrown, but was $e")
        throw e
    }
    val msg = message?.let { "$it. " } ?: ""
    asserter.fail(msg + "Expected an exception of type $exceptionClass to be thrown, but was completed successfully.")
    throw IllegalStateException(msg + "Should have failed.")
}

/** Asserts that a [block] fails with a specific exception of type [exceptionClass] being thrown. */
@Suppress("DEPRECATION")
fun <T : Throwable> assertFailsWith(exceptionClass: KClass<T>, message: String? = null, block: () -> Unit): T = assertFailsWith(exceptionClass.javaClass, message, block)

/** Asserts that a [block] fails with a specific exception of type [T] being thrown.
 *  Since inline method doesn't allow to trace where it was invoked, it is required to pass a [message] to distinguish this method call from others.
 */
@Suppress("DEPRECATION")
inline fun <reified T : Throwable> assertFailsWith(message: String, noinline block: () -> Unit): T = assertFailsWith(T::class.javaClass, message, block)


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


/**
 * Returns a Java [Class] instance corresponding to the given [KClass] instance.
 */
@Suppress("UNCHECKED_CAST")
@Intrinsic("kotlin.KClass.java.property")
@Deprecated("This is for internal use only", ReplaceWith("this.java"), kotlin.DeprecationLevel.WARNING)
val <T : Any> KClass<T>.javaClass: Class<T>
    get() = (this as ClassBasedDeclarationContainer).jClass as Class<T>

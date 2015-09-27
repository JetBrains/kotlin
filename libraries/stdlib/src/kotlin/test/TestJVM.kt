@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("TestAssertionsKt")
package kotlin.test

import java.util.ServiceLoader
import kotlin.reflect.KClass

@Deprecated("Use assertFailsWith instead.", ReplaceWith("assertFailsWith(exceptionClass, block)"))
public fun <T: Throwable> failsWith(exceptionClass: Class<T>, block: ()-> Any): T = assertFailsWith(exceptionClass, { block() })

/** Asserts that a [block] fails with a specific exception being thrown. */
public fun <T: Throwable> assertFailsWith(exceptionClass: Class<T>, block: () -> Unit): T = assertFailsWith(exceptionClass, null, block)

/** Asserts that a [block] fails with a specific exception being thrown. */
public fun <T: Throwable> assertFailsWith(exceptionClass: Class<T>, message: String?, block: () -> Unit): T {
    try {
        block()
    }
    catch (e: Throwable) {
        if (exceptionClass.isInstance(e)) {
            return e as T
        }
        asserter.fail((message?.let { "$it. "} ?: "") + "Expected an exception of type $exceptionClass to be thrown, but was $e")
        throw e
    }
    val msg = message?.let { "$it. " } ?: ""
    asserter.fail(msg + "Expected an exception of type $exceptionClass to be thrown, but was completed successfully.")
    throw IllegalStateException(msg + "Should have failed.")
}

/** Asserts that a [block] fails with a specific exception of type [exceptionClass] being thrown. */
public fun <T: Throwable> assertFailsWith(exceptionClass: KClass<T>, message: String? = null, block: () -> Unit): T = assertFailsWith(exceptionClass.java, message, block)

/** Asserts that a [block] fails with a specific exception of type [T] being thrown.
 *  Since inline method doesn't allow to trace where it was invoked, it is required to pass a [message] to distinguish this method call from others.
 */
public inline fun <reified T: Throwable> assertFailsWith(message: String, noinline block: () -> Unit): T = assertFailsWith(T::class.java, message, block)


/**
 * Comments out a [block] of test code until it is implemented while keeping a link to the code
 * to implement in your unit test output
 */
public inline fun todo(block: ()-> Any) {
    println("TODO at " + (Exception() as java.lang.Throwable).getStackTrace()?.get(1))
}

private var _asserter: Asserter? = null

/**
 * The active implementation of [Asserter]. An implementation of [Asserter] can be provided
 * using the [Java service loader](http://docs.oracle.com/javase/7/docs/api/java/util/ServiceLoader.html) mechanism.
 */
public var asserter: Asserter
    get() {
        if (_asserter == null) {
            val klass = Asserter::class.java
            val loader = ServiceLoader.load(klass)
            _asserter = loader.firstOrNull { it != null } ?: DefaultAsserter()
            //debug("using asserter $_asserter")
        }
        return _asserter!!
    }

    set(value) {
        _asserter = value
    }


/**
 * Default [Asserter] implementation to avoid dependency on JUnit or TestNG.
 */
private class DefaultAsserter() : Asserter {

    public override fun fail(message : String?) {
        if (message == null)
            throw AssertionError()
        else
            throw AssertionError(message)
    }
}
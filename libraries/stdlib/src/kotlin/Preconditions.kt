package kotlin

object Assertions {
    // TODO make private once KT-1528 is fixed.
    val _ENABLED = (javaClass<java.lang.System>()).desiredAssertionStatus()
}

/**
* Throws an [[AssertionError]] with specified *message* if the *value* is false
* and runtime assertions have been enabled on the JVM using the *-ea* JVM option.
*/
public inline fun assert(value: Boolean, message: Any = "Assertion failed") {
    if (Assertions._ENABLED) {
        if (!value) {
            throw AssertionError(message)
        }
    }
}

/**
 * Throws an [[AssertionError]] with specified *message* if the *value* is false
 * and runtime assertions have been enabled on the JVM using the *-ea* JVM option.
 */
public inline fun assert(value: Boolean, lazyMessage: () -> String) {
    if (Assertions._ENABLED) {
        if (!value) {
            val message = lazyMessage()
            throw AssertionError(message)
        }
    }
}

/**
 * Throws an [[IllegalArgumentException]] with specified *message* if the *value* is false.
 *
 * @includeFunctionBody ../../test/PreconditionsTest.kt failingRequireWithMessage
 */
public inline fun require(value: Boolean, message: Any = "Failed requirement"): Unit {
    if (!value) {
        throw IllegalArgumentException(message.toString())
    }
}

/**
 * Throws an [[IllegalArgumentException]] with specified *message* if the *value* is false.
 *
 * @includeFunctionBody ../../test/PreconditionsTest.kt failingRequireWithLazyMessage
 */
public inline fun require(value: Boolean, lazyMessage: () -> String): Unit {
    if (!value) {
        val message = lazyMessage()
        throw IllegalArgumentException(message.toString())
    }
}

/**
 * Throws an [[IllegalArgumentException]] with the given *message* if the *value* is null otherwise
 * the not null value is returned.
 *
 *  @includeFunctionBody ../../test/PreconditionsTest.kt requireNotNull
 */
public inline fun <T> requireNotNull(value: T?, message: Any = "Required value was null"): T {
    if (value == null) {
        throw IllegalArgumentException(message.toString())
    } else {
        return value
    }
}

/**
 * Throws an [[IllegalStateException]] with specified *message* if the *value* is false.
 *
 * @includeFunctionBody ../../test/PreconditionsTest.kt failingCheckWithMessage
 */
public inline fun check(value: Boolean, message: Any = "Check failed"): Unit {
    if (!value) {
        throw IllegalStateException(message.toString())
    }
}

/**
 * Throws an [[IllegalStateException]] with specified *message* if the *value* is false.
 *
 * @includeFunctionBody ../../test/PreconditionsTest.kt failingCheckWithMessage
 */
public inline fun check(value: Boolean, lazyMessage: () -> String): Unit {
    if (!value) {
        val message = lazyMessage()
        throw IllegalStateException(message.toString())
    }
}

/**
 * Throws an [[IllegalStateException]] with the given *message* if the *value* is null otherwise
 * the not null value is returned.
 *
 *  @includeFunctionBody ../../test/PreconditionsTest.kt checkNotNull
 */
public inline fun <T> checkNotNull(value: T?, message: String = "Required value was null"): T {
    if (value == null) {
        throw IllegalStateException(message)
    } else {
        return value
    }
}




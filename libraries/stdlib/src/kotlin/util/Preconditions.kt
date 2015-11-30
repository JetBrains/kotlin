@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("PreconditionsKt")
package kotlin

// TODO should not need this - its here for the JS stuff
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException

/**
 * Throws an [IllegalArgumentException] if the [value] is false.
 */
public fun require(value: Boolean): Unit = require(value) { "Failed requirement" }

/**
 * Throws an [IllegalArgumentException] with an optional [message] if the [value] is false.
 *
 * @sample test.collections.PreconditionsTest.failingRequireWithMessage
 */
@Deprecated("Use require with lazy message instead.", ReplaceWith("require(value) { message }"), DeprecationLevel.ERROR)
public fun require(value: Boolean, message: Any = "Failed requirement"): Unit {
    if (!value) {
        throw IllegalArgumentException(message.toString())
    }
}

/**
 * Throws an [IllegalArgumentException] with the result of calling [lazyMessage] if the [value] is false.
 *
 * @sample test.collections.PreconditionsTest.failingRequireWithLazyMessage
 */
public inline fun require(value: Boolean, lazyMessage: () -> Any): Unit {
    if (!value) {
        val message = lazyMessage()
        throw IllegalArgumentException(message.toString())
    }
}

/**
 * Throws an [IllegalArgumentException] if the [value] is null. Otherwise returns the not null value.
 */
public fun <T:Any> requireNotNull(value: T?): T = requireNotNull(value) { "Required value was null" }

/**
 * Throws an [IllegalArgumentException] with the given [message] if the [value] is null. Otherwise
 * returns the not null value.
 *
 * @sample test.collections.PreconditionsTest.requireNotNull
 */
@Deprecated("Use requireNotNull with lazy message instead.", ReplaceWith("requireNotNull(value) { message }"), DeprecationLevel.ERROR)
public fun <T:Any> requireNotNull(value: T?, message: Any = "Required value was null"): T {
    if (value == null) {
        throw IllegalArgumentException(message.toString())
    } else {
        return value
    }
}

/**
 * Throws an [IllegalArgumentException] with the result of calling [lazyMessage] if the [value] is null. Otherwise
 * returns the not null value.
 *
 * @sample test.collections.PreconditionsTest.requireNotNullWithLazyMessage
 */
public inline fun <T:Any> requireNotNull(value: T?, lazyMessage: () -> Any): T {
    if (value == null) {
        val message = lazyMessage()
        throw IllegalArgumentException(message.toString())
    } else {
        return value
    }
}

/**
 * Throws an [IllegalStateException] if the [value] is false.
 */
public fun check(value: Boolean): Unit = check(value) { "Check failed" }

/**
 * Throws an [IllegalStateException] with an optional [message] if the [value] is false.
 *
 * @sample test.collections.PreconditionsTest.failingCheckWithMessage
 */
@Deprecated("Use check with lazy message instead.", ReplaceWith("check(value) { message }"), DeprecationLevel.ERROR)
public fun check(value: Boolean, message: Any = "Check failed"): Unit {
    if (!value) {
        throw IllegalStateException(message.toString())
    }
}

/**
 * Throws an [IllegalStateException] with the result of calling [lazyMessage] if the [value] is false.
 *
 * @sample test.collections.PreconditionsTest.failingCheckWithLazyMessage
 */
public inline fun check(value: Boolean, lazyMessage: () -> Any): Unit {
    if (!value) {
        val message = lazyMessage()
        throw IllegalStateException(message.toString())
    }
}

/**
 * Throws an [IllegalStateException] if the [value] is null. Otherwise
 * returns the not null value.
 */
public fun <T:Any> checkNotNull(value: T?): T = checkNotNull(value) { "Required value was null" }

/**
 * Throws an [IllegalStateException] with the given [message] if the [value] is null. Otherwise
 * returns the not null value.
 *
 * @sample test.collections.PreconditionsTest.checkNotNull
 */
@Deprecated("Use checkNotNull with lazy message instead.", ReplaceWith("checkNotNull(value) { message }"), DeprecationLevel.ERROR)
public fun <T:Any> checkNotNull(value: T?, message: Any = "Required value was null"): T {
    if (value == null) {
        throw IllegalStateException(message.toString())
    } else {
        return value
    }
}

/**
 * Throws an [IllegalStateException] with the result of calling [lazyMessage]  if the [value] is null. Otherwise
 * returns the not null value.
 */
public inline fun <T:Any> checkNotNull(value: T?, lazyMessage: () -> Any): T {
    if (value == null) {
        val message = lazyMessage()
        throw IllegalStateException(message.toString())
    } else {
        return value
    }
}


/**
 * Throws an [IllegalStateException] with the given [message].
 *
 * @sample test.collections.PreconditionsTest.error
 */
public fun error(message: Any): Nothing = throw IllegalStateException(message.toString())

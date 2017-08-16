@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("PreconditionsKt")
package kotlin

/**
 * Throws an [IllegalArgumentException] if the [value] is false.
 *
 * @sample samples.misc.Preconditions.failRequireWithLazyMessage
 */
@kotlin.internal.InlineOnly
@kotlin.internal.Returns
public inline fun require(@kotlin.internal.Equals(kotlin.internal.ConstantValue.TRUE) value: Boolean): Unit = require(value) { "Failed requirement." }

/**
 * Throws an [IllegalArgumentException] with the result of calling [lazyMessage] if the [value] is false.
 *
 * @sample samples.misc.Preconditions.failRequireWithLazyMessage
 */
@kotlin.internal.InlineOnly
@kotlin.internal.Returns
public inline fun require(@kotlin.internal.Equals(kotlin.internal.ConstantValue.TRUE) value: Boolean, lazyMessage: () -> Any): Unit {
    if (!value) {
        val message = lazyMessage()
        throw IllegalArgumentException(message.toString())
    }
}

/**
 * Throws an [IllegalArgumentException] if the [value] is null. Otherwise returns the not null value.
 */
@kotlin.internal.InlineOnly
@kotlin.internal.Returns
public inline fun <T:Any> requireNotNull(@kotlin.internal.Equals(kotlin.internal.ConstantValue.NOT_NULL) value: T?): T = requireNotNull(value) { "Required value was null." }

/**
 * Throws an [IllegalArgumentException] with the result of calling [lazyMessage] if the [value] is null. Otherwise
 * returns the not null value.
 *
 * @sample samples.misc.Preconditions.failRequireWithLazyMessage
 */
@kotlin.internal.InlineOnly
@kotlin.internal.Returns
public inline fun <T:Any> requireNotNull(@kotlin.internal.Equals(kotlin.internal.ConstantValue.NOT_NULL) value: T?, lazyMessage: () -> Any): T {
    if (value == null) {
        val message = lazyMessage()
        throw IllegalArgumentException(message.toString())
    } else {
        return value
    }
}

/**
 * Throws an [IllegalStateException] if the [value] is false.
 *
 * @sample samples.misc.Preconditions.failCheckWithLazyMessage
 */
@kotlin.internal.InlineOnly
@kotlin.internal.Returns
public inline fun check(@kotlin.internal.Equals(kotlin.internal.ConstantValue.TRUE) value: Boolean): Unit = check(value) { "Check failed." }

/**
 * Throws an [IllegalStateException] with the result of calling [lazyMessage] if the [value] is false.
 *
 * @sample samples.misc.Preconditions.failCheckWithLazyMessage
 */
@kotlin.internal.InlineOnly
@kotlin.internal.Returns
public inline fun check(@kotlin.internal.Equals(kotlin.internal.ConstantValue.TRUE) value: Boolean, lazyMessage: () -> Any): Unit {
    if (!value) {
        val message = lazyMessage()
        throw IllegalStateException(message.toString())
    }
}

/**
 * Throws an [IllegalStateException] if the [value] is null. Otherwise
 * returns the not null value.
 *
 * @sample samples.misc.Preconditions.failCheckWithLazyMessage
 */
@kotlin.internal.InlineOnly
@kotlin.internal.Returns
public inline fun <T:Any> checkNotNull(@kotlin.internal.Equals(kotlin.internal.ConstantValue.NOT_NULL) value: T?): T = checkNotNull(value) { "Required value was null." }

/**
 * Throws an [IllegalStateException] with the result of calling [lazyMessage]  if the [value] is null. Otherwise
 * returns the not null value.
 *
 * @sample samples.misc.Preconditions.failCheckWithLazyMessage
 */
@kotlin.internal.InlineOnly
@kotlin.internal.Returns
public inline fun <T:Any> checkNotNull(@kotlin.internal.Equals(kotlin.internal.ConstantValue.NOT_NULL) value: T?, lazyMessage: () -> Any): T {
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
 * @sample samples.misc.Preconditions.failWithError
 */
@kotlin.internal.InlineOnly
public inline fun error(message: Any): Nothing = throw IllegalStateException(message.toString())

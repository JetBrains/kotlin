@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StandardKt")
package kotlin

/**
 * An exception is thrown to indicate that a method body remains to be implemented.
 */
public class NotImplementedError(message: String = "An operation is not implemented.") : Error(message)

/**
 * Always throws [NotImplementedError] stating that operation is not implemented.
 */

@kotlin.internal.InlineOnly
public inline fun TODO(): Nothing = throw NotImplementedError()

/**
 * Always throws [NotImplementedError] stating that operation is not implemented.
 *
 * @param reason a string explaining why the implementation is missing.
 */
@kotlin.internal.InlineOnly
public inline fun TODO(reason: String): Nothing = throw NotImplementedError("An operation is not implemented: $reason")



/**
 * Calls the specified function [block] and returns its result.
 */
@kotlin.internal.InlineOnly
public inline fun <R> run(@kotlin.internal.CalledInPlace(kotlin.internal.InvocationCount.EXACTLY_ONCE) block: () -> R): R = block()

/**
 * Calls the specified function [block] with `this` value as its receiver and returns its result.
 */
@kotlin.internal.InlineOnly
public inline fun <T, R> T.run(@kotlin.internal.CalledInPlace(kotlin.internal.InvocationCount.EXACTLY_ONCE) block: T.() -> R): R = block()

/**
 * Calls the specified function [block] with the given [receiver] as its receiver and returns its result.
 */
@kotlin.internal.InlineOnly
public inline fun <T, R> with(receiver: T, @kotlin.internal.CalledInPlace(kotlin.internal.InvocationCount.EXACTLY_ONCE) block: T.() -> R): R = receiver.block()

/**
 * Calls the specified function [block] with `this` value as its receiver and returns `this` value.
 */
@kotlin.internal.InlineOnly
public inline fun <T> T.apply(@kotlin.internal.CalledInPlace(kotlin.internal.InvocationCount.EXACTLY_ONCE) block: T.() -> Unit): T { block(); return this }

/**
 * Calls the specified function [block] with `this` value as its argument and returns `this` value.
 */
@kotlin.internal.InlineOnly
@SinceKotlin("1.1")
public inline fun <T> T.also(@kotlin.internal.CalledInPlace(kotlin.internal.InvocationCount.EXACTLY_ONCE) block: (T) -> Unit): T { block(this); return this }

/**
 * Calls the specified function [block] with `this` value as its argument and returns its result.
 */
@kotlin.internal.InlineOnly
public inline fun <T, R> T.let(@kotlin.internal.CalledInPlace(kotlin.internal.InvocationCount.EXACTLY_ONCE) block: (T) -> R): R = block(this)

/**
 * Returns `this` value if it satisfies the given [predicate] or `null`, if it doesn't.
 */
@kotlin.internal.InlineOnly
@SinceKotlin("1.1")
public inline fun <T> T.takeIf(@kotlin.internal.CalledInPlace(kotlin.internal.InvocationCount.EXACTLY_ONCE) predicate: (T) -> Boolean): T? = if (predicate(this)) this else null

/**
 * Returns `this` value if it _does not_ satisfy the given [predicate] or `null`, if it does.
 */
@kotlin.internal.InlineOnly
@SinceKotlin("1.1")
public inline fun <T> T.takeUnless(@kotlin.internal.CalledInPlace(kotlin.internal.InvocationCount.EXACTLY_ONCE) predicate: (T) -> Boolean): T? = if (!predicate(this)) this else null

/**
 * Executes the given function [action] specified number of [times].
 *
 * A zero-based index of current iteration is passed as a parameter to [action].
 */
@kotlin.internal.InlineOnly
public inline fun repeat(times: Int, @kotlin.internal.CalledInPlace action: (Int) -> Unit) {
    for (index in 0..times - 1) {
        action(index)
    }
}
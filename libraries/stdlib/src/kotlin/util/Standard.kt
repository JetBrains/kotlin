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
public fun TODO(): Nothing = throw NotImplementedError()

/**
 * Always throws [NotImplementedError] stating that operation is not implemented.
 *
 * @param reason a string explaining why the implementation is missing.
 */
public fun TODO(reason: String): Nothing = throw NotImplementedError("An operation is not implemented: $reason")



/**
 * Calls the specified function [f] and returns its result.
 */
public inline fun <R> run(f: () -> R): R = f()

/**
 * Calls the specified function [f] with `this` value as its receiver and returns its result.
 */
public inline fun <T, R> T.run(f: T.() -> R): R = f()

/**
 * Calls the specified function [f] with the given [receiver] as its receiver and returns its result.
 */
public inline fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()

/**
 * Calls the specified function [f] with `this` value as its receiver and returns `this` value.
 */
public inline fun <T> T.apply(f: T.() -> Unit): T { f(); return this }

/**
 * Calls the specified function [f] with `this` value as its argument and returns its result.
 */
public inline fun <T, R> T.let(f: (T) -> R): R = f(this)

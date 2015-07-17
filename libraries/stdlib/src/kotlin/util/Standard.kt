package kotlin

/**
 * Creates a tuple of type [Pair] from this and [that].
 *
 * This can be useful for creating [Map] literals with less noise, for example:
 * @sample test.collections.MapTest.createUsingTo
 */
public fun <A, B> A.to(that: B): Pair<A, B> = Pair(this, that)

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

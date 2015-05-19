package kotlin

/**
 * Creates a tuple of type [Pair] from this and [that].
 *
 * This can be useful for creating [Map] literals with less noise, for example:
 * @sample test.collections.MapTest.createUsingTo
 */
public fun <A, B> A.to(that: B): Pair<A, B> = Pair(this, that)

/**
 * Calls the specified function.
 */
public inline fun <T> run(f: () -> T): T = f()

/**
 * Execute f with given receiver
 */
public inline fun <T, R> with(receiver: T, f: T.() -> R): R = receiver.f()

/**
 * Converts receiver to body parameter
 */
public inline fun <T, R> T.let(f: (T) -> R): R = f(this)

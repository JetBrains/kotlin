package kotlin.coroutines.experimental


/**
 * Creates coroutine with receiver type [R] and result type [T].
 * This function creates a new, fresh instance of suspendable computation every time it is invoked.
 * To start executing the created coroutine, invoke `resume(Unit)` on the returned [Continuation] instance.
 * The [completion] continuation is invoked when coroutine completes with result of exception.
 * An optional [dispatcher] may be specified to customise dispatch of continuations between suspension points inside the coroutine.
 */
@SinceKotlin("1.1")
public header fun <R, T> (suspend R.() -> T).createCoroutine(
        receiver: R,
        completion: Continuation<T>
): Continuation<Unit>

/**
 * Creates coroutine without receiver and with result type [T].
 * This function creates a new, fresh instance of suspendable computation every time it is invoked.
 * To start executing the created coroutine, invoke `resume(Unit)` on the returned [Continuation] instance.
 * The [completion] continuation is invoked when coroutine completes with result of exception.
 * An optional [dispatcher] may be specified to customise dispatch of continuations between suspension points inside the coroutine.
 */
@SinceKotlin("1.1")
public header fun <T> (suspend () -> T).createCoroutine(
        completion: Continuation<T>
): Continuation<Unit>

@PublishedApi
internal header class SafeContinuation<in T> : Continuation<T> {
    internal constructor(delegate: Continuation<T>, initialResult: Any?)

    @PublishedApi
    internal constructor(delegate: Continuation<T>)

    @PublishedApi
    internal fun getResult(): Any?

    override val context: CoroutineContext
    override fun resume(value: T): Unit
    override fun resumeWithException(exception: Throwable): Unit
}

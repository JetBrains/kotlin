package kotlin.coroutines.experimental

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

@SinceKotlin("1.1")
internal header fun <T> (suspend () -> T).createCoroutineInternal(
        completion: Continuation<T>
): Continuation<Unit>

@SinceKotlin("1.1")
internal header fun <R, T> (suspend R.() -> T).createCoroutineInternal(
        receiver: R,
        completion: Continuation<T>
): Continuation<Unit>

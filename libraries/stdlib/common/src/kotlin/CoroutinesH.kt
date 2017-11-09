package kotlin.coroutines.experimental

@PublishedApi
internal expect class SafeContinuation<in T> : Continuation<T> {
    internal constructor(delegate: Continuation<T>, initialResult: Any?)

    @PublishedApi
    internal constructor(delegate: Continuation<T>)

    @PublishedApi
    internal fun getResult(): Any?

    override val context: CoroutineContext
    override fun resume(value: T): Unit
    override fun resumeWithException(exception: Throwable): Unit
}

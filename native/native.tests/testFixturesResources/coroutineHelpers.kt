// This file is compiled into each stepping test.

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

object EmptyContinuation: Continuation<Any?> {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<Any?>) {
        result.getOrThrow()
    }
}

import kotlin.coroutines.*

// Shared coroutine helpers. Added once at step 0 and never modified, so they stay compiled while the
// per-step `test.kt` is swapped in and out.
val emptyCont = object : Continuation<Any?> {
    override val context = EmptyCoroutineContext
    override fun resumeWith(result: Result<Any?>) { result.getOrThrow() }
}

fun builder(c: suspend () -> Boolean): Boolean {
    var res = false
    c.startCoroutine(object : Continuation<Boolean> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Boolean>) { res = result.getOrThrow() }
    })
    return res
}

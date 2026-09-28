package sample

import kotlin.coroutines.*

suspend fun id(x: Any): Any = x

// A suspend lambda with a non-tail suspend call: WasmSuspendLambdaMergingLowering replaces its
// coroutine class with a shared class (`SuspendLambda_*`), which is merged at link time with the
// structurally identical one from the `main` module. This file is never modified, so from step 1 on
// its fragment comes from the incremental cache.
fun fromLib(x: Any): suspend () -> Any = { id(x); x }

fun runCoroutine(c: suspend () -> Any): Any? {
    var result: Any? = null
    c.startCoroutine(Continuation(EmptyCoroutineContext) { result = it.getOrThrow() })
    return result
}

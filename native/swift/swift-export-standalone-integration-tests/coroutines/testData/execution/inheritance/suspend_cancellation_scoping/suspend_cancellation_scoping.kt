// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Main
// FILE: suspend_cancellation_scoping.kt

import kotlinx.coroutines.*

// The Kotlin `guarded()` implementation suspends for as long as the Swift overrides do, so passing a plain
// `AsyncGuarded` through any driver below is an exact control for passing a Swift subclass
open class AsyncGuarded {
    open suspend fun guarded(): String {
        delay(3_000)
        return "kotlin"
    }
}

// The reverse-bridge call runs inside `withContext(NonCancellable)`, so cancelling the surrounding job must not
// signal the Swift override
suspend fun callGuardedNonCancellable(g: AsyncGuarded): String = coroutineScope {
    var captured = "not-completed"
    val job = launch {
        withContext(NonCancellable) {
            captured = g.guarded()
        }
    }
    delay(50)
    job.cancel()
    job.join()
    captured
}

// A child coroutine fails with an exception — so the scope is failed — and the override is awaited
// directly in the scope body
suspend fun callFailingChildWithOverrideInScopeBody(g: AsyncGuarded): String = coroutineScope {
    launch {
        delay(50)
        throw IllegalStateException("boom")
    }
    g.guarded()
}

// The scope's own job is cancelled
suspend fun callCancelledScopeJobWithOverrideInScopeBody(g: AsyncGuarded): String = coroutineScope {
    val self = coroutineContext[Job]!!
    launch {
        delay(50)
        self.cancel()
    }
    g.guarded()
}

// A child coroutine fails with an exception, and the override is awaited inside a child coroutine
suspend fun callFailingChildWithOverrideInChild(g: AsyncGuarded): String = coroutineScope {
    launch { g.guarded() }
    launch {
        delay(50)
        throw IllegalStateException("boom")
    }
    "done"
}

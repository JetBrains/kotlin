// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Main
// FILE: suspend_identity_and_constructors.kt

import kotlinx.coroutines.*

// Two suspend interfaces implemented by one class, so a Swift subclass overriding both members forces two
// distinct itable slots to be patched on it.
interface AsyncLeft {
    suspend fun left(): String
}

interface AsyncRight {
    suspend fun right(): String
}

open class AsyncMultiBase : AsyncLeft, AsyncRight {
    override suspend fun left(): String = "Kotlin left"
    override suspend fun right(): String = "Kotlin right"
}

// Each call site is typed to one interface only, so each goes through that interface's own slot.
suspend fun callAsyncLeft(value: AsyncLeft): String = value.left()
suspend fun callAsyncRight(value: AsyncRight): String = value.right()

// Constructor ancestry combined with Kotlin retention and cancellation.
open class ConstructedAsyncBase(val origin: String) {
    constructor(code: Int) : this("code:$code")

    open suspend fun work(): String = "Kotlin work:$origin"
}

class ConstructedAsyncStorage {
    private var stored: ConstructedAsyncBase? = null

    fun store(value: ConstructedAsyncBase) {
        stored = value
    }

    fun retrieve(): ConstructedAsyncBase? = stored
}

suspend fun callConstructedWorkWithTimeout(value: ConstructedAsyncBase, timeoutMs: Long): String =
    withTimeoutOrNull(timeoutMs) { value.work() } ?: "timed_out"

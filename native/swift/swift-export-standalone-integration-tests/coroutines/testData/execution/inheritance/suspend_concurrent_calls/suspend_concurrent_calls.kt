// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Main
// FILE: suspend_concurrent_calls.kt

import kotlinx.coroutines.*

open class AsyncWorker(val id: String) {
    open suspend fun work(): String = "kotlin-work:$id"
}

suspend fun callWork(w: AsyncWorker): String = w.work()

suspend fun callBothConcurrently(a: AsyncWorker, b: AsyncWorker): String =
    withContext(Dispatchers.Default) {
        val first = async { a.work() }
        val second = async { b.work() }
        first.await() + "|" + second.await()
    }

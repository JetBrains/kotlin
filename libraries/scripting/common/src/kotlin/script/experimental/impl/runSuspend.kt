/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.impl

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

// Copied with modifications form kotlin.coroutines.jvm.internal.runSuspend/RunSuspend
// to use as an equivalent of runBlocking without dependency on the kotlinx.coroutines

@Deprecated("For internal use only, use kotlinx.coroutines instead", level = DeprecationLevel.ERROR)
fun <T> internalScriptingRunSuspend(block: suspend () -> T) : T {
    val run = InternalScriptingRunSuspend<T>()
    block.startCoroutine(run)
    return run.await()
}

private class InternalScriptingRunSuspend<T> : Continuation<T> {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    @Suppress("RESULT_CLASS_IN_RETURN_TYPE")
    var result: Result<T>? = null

    override fun resumeWith(result: Result<T>) = synchronized(this) {
        this.result = result
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") (this as Object).notifyAll()
    }

    fun await(): T = synchronized(this) {
        while (true) {
            when (this.result) {
                null -> @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") (this as Object).wait()
                else -> break
            }
        }
        return result!!.getOrThrow()
    }
}


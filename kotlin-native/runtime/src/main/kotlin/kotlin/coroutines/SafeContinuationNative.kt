/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.coroutines

import kotlin.*
import kotlin.native.concurrent.*
import kotlin.coroutines.intrinsics.CoroutineSingletons.*
import kotlin.coroutines.intrinsics.*
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED

@PublishedApi
@SinceKotlin("1.3")
@OptIn(FreezingIsDeprecated::class)
internal actual class SafeContinuation<in T>
internal actual constructor(
        private val delegate: Continuation<T>,
        initialResult: Any?
) : Continuation<T> {
    @PublishedApi
    internal actual constructor(delegate: Continuation<T>) : this(delegate, UNDECIDED)

    public actual override val context: CoroutineContext
        get() = delegate.context

    @Suppress("DEPRECATION")
    private var resultRef = FreezableAtomicReference<Any?>(initialResult)

    public actual override fun resumeWith(result: Result<T>) {
        while (true) {
            val cur = resultRef.value
            when {
                cur === UNDECIDED -> if (resultRef.compareAndSet(UNDECIDED, result.value)) return
                cur === COROUTINE_SUSPENDED -> if (resultRef.compareAndSet(COROUTINE_SUSPENDED, RESUMED)) {
                    delegate.resumeWith(result)
                    return
                }
                else -> throw IllegalStateException("Already resumed")
            }
        }
    }

    @PublishedApi
    internal actual fun getOrThrow(): Any? {
        var result = resultRef.value
        if (result === UNDECIDED) {
            if (resultRef.compareAndSet(UNDECIDED, COROUTINE_SUSPENDED)) return COROUTINE_SUSPENDED
            result = resultRef.value
        }
        return when {
            result === RESUMED -> COROUTINE_SUSPENDED // already called continuation, indicate COROUTINE_SUSPENDED upstream
            result is Result.Failure -> throw result.exception
            else -> result // either COROUTINE_SUSPENDED or data
        }
    }
}

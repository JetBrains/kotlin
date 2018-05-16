/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.coroutines.experimental

import kotlin.coroutines.experimental.intrinsics.*

// Single-threaded continuation.
@PublishedApi
internal final actual class SafeContinuation<in T> internal actual constructor(
        private val delegate: Continuation<T>, initialResult: Any?) : Continuation<T> {

    @PublishedApi
    internal actual constructor(delegate: Continuation<T>) : this(delegate, UNDECIDED)

    actual override val context: CoroutineContext
        get() = delegate.context

    private var result: Any? = initialResult

    companion object {
        private val UNDECIDED: Any? = Any()
        private val RESUMED: Any? = Any()
    }

    private class Fail(val exception: Throwable)

    actual override fun resume(value: T) {
        when {
            result === UNDECIDED -> result = value
            result === COROUTINE_SUSPENDED -> {
                result = RESUMED
                delegate.resume(value)
            }
            else -> throw IllegalStateException("Already resumed")
        }
    }

    actual override fun resumeWithException(exception: Throwable) {
        when  {
            result === UNDECIDED -> result = Fail(exception)
            result === COROUTINE_SUSPENDED -> {
                result = RESUMED
                delegate.resumeWithException(exception)
            }
            else -> throw IllegalStateException("Already resumed")
        }
    }

    @PublishedApi
    internal actual fun getResult(): Any? {
        if (this.result === UNDECIDED) this.result = COROUTINE_SUSPENDED
        val result = this.result
        when {
            result === RESUMED -> return COROUTINE_SUSPENDED // already called continuation, indicate COROUTINE_SUSPENDED upstream
            result is Fail -> throw result.exception
            else -> return result // either COROUTINE_SUSPENDED or data
        }
    }
}
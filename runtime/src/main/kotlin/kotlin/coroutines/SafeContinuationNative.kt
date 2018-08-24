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

package kotlin.coroutines

import kotlin.*
import kotlin.coroutines.intrinsics.CoroutineSingletons.*

@PublishedApi
@SinceKotlin("1.3")
internal actual class SafeContinuation<in T>
internal actual constructor(
        private val delegate: Continuation<T>,
        initialResult: Any?
) : Continuation<T> {
    @PublishedApi
    internal actual constructor(delegate: Continuation<T>) : this(delegate, UNDECIDED)

    public actual override val context: CoroutineContext
        get() = delegate.context

    private var result: Any? = initialResult

    public actual override fun resumeWith(result: SuccessOrFailure<T>) {
        val cur = this.result
        when {
            cur === UNDECIDED -> this.result = result.value
            cur === COROUTINE_SUSPENDED -> {
                this.result = RESUMED
                delegate.resumeWith(result)
            }
            else -> throw IllegalStateException("Already resumed")
        }
    }

    @PublishedApi
    internal actual fun getOrThrow(): Any? {
        val result = this.result
        if (result === UNDECIDED) {
            this.result = COROUTINE_SUSPENDED
            return COROUTINE_SUSPENDED
        }
        return when {
            result === RESUMED -> COROUTINE_SUSPENDED // already called continuation, indicate COROUTINE_SUSPENDED upstream
            result is SuccessOrFailure.Failure -> throw result.exception
            else -> result // either COROUTINE_SUSPENDED or data
        }
    }
}
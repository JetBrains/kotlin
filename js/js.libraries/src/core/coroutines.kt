/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

@JsName("CoroutineImpl")
internal abstract class CoroutineImpl(private val resultContinuation: Continuation<Any?>) : Continuation<Any?> {
    protected var state = 0
    protected var exceptionState = 0
    protected var result: Any? = null
    protected var exception: Throwable? = null
    protected var finallyPath: Array<Int>? = null

    public override val context: CoroutineContext = resultContinuation.context

    val facade: Continuation<Any?> = context[ContinuationInterceptor]?.interceptContinuation(this) ?: this

    override fun resume(value: Any?) {
        result = value
        doResumeWrapper()
    }

    override fun resumeWithException(exception: Throwable) {
        state = exceptionState
        this.exception = exception
        doResumeWrapper()
    }

    protected fun doResumeWrapper() {
        processBareContinuationResume(resultContinuation) { doResume() }
    }

    protected abstract fun doResume(): Any?
}

private val UNDECIDED: Any? = Any()
private val RESUMED: Any? = Any()
private class Fail(val exception: Throwable)

@PublishedApi
internal class SafeContinuation<in T>
internal constructor(
        private val delegate: Continuation<T>,
        initialResult: Any?
) : Continuation<T> {

    @PublishedApi
    internal constructor(delegate: Continuation<T>) : this(delegate, UNDECIDED)

    public override val context: CoroutineContext
        get() = delegate.context

    private var result: Any? = initialResult

    override fun resume(value: T) {
        when {
            result === UNDECIDED -> {
                result = value
            }
            result === COROUTINE_SUSPENDED -> {
                result = RESUMED
                delegate.resume(value)
            }
            else -> {
                throw IllegalStateException("Already resumed")
            }
        }
    }

    override fun resumeWithException(exception: Throwable) {
        when {
            result === UNDECIDED -> {
                result = Fail(exception)
            }
            result === COROUTINE_SUSPENDED -> {
                result = RESUMED
                delegate.resumeWithException(exception)
            }
            else -> {
                throw IllegalStateException("Already resumed")
            }
        }
    }

    @PublishedApi
    internal fun getResult(): Any? {
        if (result === UNDECIDED) {
            result = COROUTINE_SUSPENDED
        }
        val result = this.result
        return when {
            result === RESUMED -> {
                COROUTINE_SUSPENDED // already called continuation, indicate SUSPENDED upstream
            }
            result is Fail -> {
                throw result.exception
            }
            else -> {
                result // either SUSPENDED or data
            }
        }
    }
}

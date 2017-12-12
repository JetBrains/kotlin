/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
@file:kotlin.jvm.JvmVersion
package kotlin.coroutines.experimental

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import kotlin.*
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED

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

    @Volatile
    private var result: Any? = initialResult

    companion object {
        private val UNDECIDED: Any? = Any()
        private val RESUMED: Any? = Any()

        @Suppress("UNCHECKED_CAST")
        @JvmStatic
        private val RESULT = AtomicReferenceFieldUpdater.newUpdater<SafeContinuation<*>, Any?>(
                SafeContinuation::class.java, Any::class.java as Class<Any?>, "result")
    }

    private class Fail(val exception: Throwable)

    override fun resume(value: T) {
        while (true) { // lock-free loop
            val result = this.result // atomic read
            when {
                result === UNDECIDED -> if (RESULT.compareAndSet(this, UNDECIDED, value)) return
                result === COROUTINE_SUSPENDED -> if (RESULT.compareAndSet(this, COROUTINE_SUSPENDED, RESUMED)) {
                    delegate.resume(value)
                    return
                }
                else -> throw IllegalStateException("Already resumed")
            }
        }
    }

    override fun resumeWithException(exception: Throwable) {
        while (true) { // lock-free loop
            val result = this.result // atomic read
            when  {
                result === UNDECIDED -> if (RESULT.compareAndSet(this, UNDECIDED, Fail(exception))) return
                result === COROUTINE_SUSPENDED -> if (RESULT.compareAndSet(this, COROUTINE_SUSPENDED, RESUMED)) {
                    delegate.resumeWithException(exception)
                    return
                }
                else -> throw IllegalStateException("Already resumed")
            }
        }
    }

    @PublishedApi
    internal fun getResult(): Any? {
        var result = this.result // atomic read
        if (result === UNDECIDED) {
            if (RESULT.compareAndSet(this, UNDECIDED, COROUTINE_SUSPENDED)) return COROUTINE_SUSPENDED
            result = this.result // reread volatile var
        }
        when {
            result === RESUMED -> return COROUTINE_SUSPENDED // already called continuation, indicate COROUTINE_SUSPENDED upstream
            result is Fail -> throw result.exception
            else -> return result // either COROUTINE_SUSPENDED or data
        }
    }
}

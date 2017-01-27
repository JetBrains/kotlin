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

@file:JvmVersion
package kotlin.coroutines.experimental.jvm.internal

import java.lang.IllegalStateException
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.ContinuationInterceptor
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.experimental.jvm.internal.interceptContinuationIfNeeded
import kotlin.jvm.internal.Lambda

abstract class CoroutineImpl(
        arity: Int,
        @JvmField
        protected var completion: Continuation<Any?>?
) : Lambda(arity), Continuation<Any?> {

    // label == -1 when coroutine cannot be started (it is just a factory object) or has already finished execution
    // label == 0 in initial part of the coroutine
    @JvmField
    protected var label: Int = if (completion != null) 0 else -1

    private val _context: CoroutineContext? = completion?.context

    override val context: CoroutineContext
        get() = _context!!

    private var _facade: Continuation<Any?>? = null

    val facade: Continuation<Any?> get() {
        if (_facade == null) _facade = _context!!.interceptContinuationIfNeeded(this)
        return _facade!!
    }

    override fun resume(value: Any?) {
        try {
            val result = doResume(value, null)
            if (result != COROUTINE_SUSPENDED)
                completion!!.resume(result)
        } catch (e: Throwable) {
            completion!!.resumeWithException(e)
        }
    }

    override fun resumeWithException(exception: Throwable) {
        try {
            val result = doResume(null, exception)
            if (result != COROUTINE_SUSPENDED)
                completion!!.resume(result)
        } catch (e: Throwable) {
            completion!!.resumeWithException(e)
        }
    }

    protected abstract fun doResume(data: Any?, exception: Throwable?): Any?

    open fun create(completion: Continuation<*>): Continuation<Unit> {
        throw IllegalStateException("create(Continuation) has not been overridden")
    }

    open fun create(value: Any?, completion: Continuation<*>): Continuation<Unit> {
        throw IllegalStateException("create(Any?;Continuation) has not been overridden")
    }
}

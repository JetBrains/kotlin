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
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.processBareContinuationResume
import kotlin.jvm.internal.Lambda

/**
 * @suppress
 */
abstract class CoroutineImpl(
        arity: Int,
        @JvmField
        protected var completion: Continuation<Any?>?
) : Lambda(arity), Continuation<Any?> {

    // label == -1 when coroutine cannot be started (it is just a factory object) or has already finished execution
    // label == 0 in initial part of the coroutine
    @JvmField
    var label: Int = if (completion != null) 0 else -1

    private val _context: CoroutineContext? = completion?.context

    override val context: CoroutineContext
        get() = _context!!

    private var _facade: Continuation<Any?>? = null

    val facade: Continuation<Any?> get() {
        if (_facade == null) _facade = interceptContinuationIfNeeded(_context!!, this)
        return _facade!!
    }

    override fun resume(value: Any?) {
        processBareContinuationResume(completion!!) {
            doResume(value, null)
        }
    }

    override fun resumeWithException(exception: Throwable) {
        processBareContinuationResume(completion!!) {
            doResume(null, exception)
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

abstract class CoroutineImplForNamedFunction(
       completion: Continuation<Any?>?
) : CoroutineImpl(0, completion), Continuation<Any?> {
    private companion object {
        private const val LAST_BIT_MASK = 1 shl 31
    }

    @JvmField
    var data: Any? = null

    @JvmField
    var exception: Throwable? = null

    fun checkAndFlushLastBit(): Boolean {
        if (label and LAST_BIT_MASK != 0) {
            label -= LAST_BIT_MASK
            return true
        }

        return false
    }

    override fun doResume(data: Any?, exception: Throwable?): Any? {
        this.data = data
        this.exception = exception
        this.label = this.label or LAST_BIT_MASK
        return doResume()
    }

    protected abstract fun doResume(): Any?
}

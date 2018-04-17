/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

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
) : Lambda<Any?>(arity), Continuation<Any?> {

    // label == -1 when coroutine cannot be started (it is just a factory object) or has already finished execution
    // label == 0 in initial part of the coroutine
    @JvmField
    protected var label: Int = if (completion != null) 0 else -1

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

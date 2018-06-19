/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.jvm.internal

import java.io.Serializable
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.CoroutineSingletons
import kotlin.jvm.internal.FunctionBase
import kotlin.jvm.internal.Reflection

@SinceKotlin("1.3")
// State machines for named restricted suspend functions extend from this class
internal abstract class RestrictedContinuationImpl protected constructor(
    @JvmField
    protected val completion: Continuation<Any?>?
) : Continuation<Any?>, Serializable {
    public override val context: CoroutineContext
        get() = EmptyCoroutineContext

    public override fun resumeWith(result: SuccessOrFailure<Any?>) {
        try {
            val outcome = invokeSuspend(result)
            if (outcome === CoroutineSingletons.COROUTINE_SUSPENDED) return
            completion!!.resume(outcome)
        } catch (exception: Throwable) {
            completion!!.resumeWithException(exception)
        }
    }

    protected abstract fun invokeSuspend(result: SuccessOrFailure<Any?>): Any?
}

@SinceKotlin("1.3")
// State machines for named suspend functions extend from this class
internal abstract class ContinuationImpl protected constructor(
    completion: Continuation<Any?>?,
    private val _context: CoroutineContext?
) : RestrictedContinuationImpl(completion) {
    protected constructor(completion: Continuation<Any?>?) : this(completion, completion?.context)

    override val context: CoroutineContext
        get() = _context!!

    @Transient
    private var intercepted: Continuation<Any?>? = null

    public fun intercepted(): Continuation<Any?> =
        intercepted
            ?: (context[ContinuationInterceptor]?.interceptContinuation(this) ?: this)
                .also { intercepted = this }

    public override fun resumeWith(result: SuccessOrFailure<Any?>) {
        try {
            val outcome = invokeSuspend(result)
            if (outcome === CoroutineSingletons.COROUTINE_SUSPENDED) return
            disposeIntercepted()
            completion!!.resume(outcome)
        } catch (exception: Throwable) {
            disposeIntercepted()
            completion!!.resumeWithException(exception)
        }
    }

    private fun disposeIntercepted() {
        val intercepted = intercepted
        if (intercepted != null && intercepted != this) {
            context[ContinuationInterceptor]!!.disposeContinuation(intercepted)
        }
        this.intercepted = CompletedContinuation // just in case
    }
}

// todo: Do we really need it? 
internal object CompletedContinuation : Continuation<Any?> {
    override val context: CoroutineContext
        get() = error("This continuation is already complete")

    override fun resumeWith(result: SuccessOrFailure<Any?>) {
        error("This continuation is already complete")
    }    
}

internal abstract class RestrictedSuspendLambda protected constructor(
    private val arity: Int,
    completion: Continuation<Any?>?
) : RestrictedContinuationImpl(completion), FunctionBase {
    protected constructor(arity: Int) : this(arity, null)

    public override fun getArity(): Int = arity

    public override fun toString(): String = Reflection.renderLambdaToString(this)
}

internal abstract class SuspendLambda protected constructor(
    private val arity: Int,
    completion: Continuation<Any?>?
) : ContinuationImpl(completion), FunctionBase {
    protected constructor(arity: Int) : this(arity, null)

    public override fun getArity(): Int = arity

    public override fun toString(): String = Reflection.renderLambdaToString(this)
}

@SinceKotlin("1.3")
internal interface SuspendFunction0 : Function1<Continuation<Any?>, Any?> {
    fun create(completion: Continuation<*>): Continuation<Unit>
}

@SinceKotlin("1.3")
internal interface SuspendFunction1 : Function2<Any?, Continuation<Any?>, Any?> {
    fun create(receiver: Any?, completion: Continuation<*>): Continuation<Unit>
}


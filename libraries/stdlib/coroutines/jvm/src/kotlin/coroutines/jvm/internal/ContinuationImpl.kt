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
    init {
        @Suppress("LeakingThis")
        validateContext()
    }

    protected open fun validateContext() {
        completion?.let {
            require(it.context === EmptyCoroutineContext) { "Coroutines with restricted suspension must have EmptyCoroutineContext" }
        }
    }

    public override val context: CoroutineContext
        get() = EmptyCoroutineContext

    public override fun resumeWith(result: SuccessOrFailure<Any?>) {
        val completion = completion!! // fail fast when trying to resume continuation without completion
        try {
            val outcome = invokeSuspend(result)
            if (outcome === CoroutineSingletons.COROUTINE_SUSPENDED) return
            completion.resume(outcome)
        } catch (exception: Throwable) {
            completion.resumeWithException(exception)
        }
    }

    protected abstract fun invokeSuspend(result: SuccessOrFailure<Any?>): Any?

    public open fun create(completion: Continuation<*>): Continuation<Unit> {
        throw UnsupportedOperationException("create(Continuation) has not been overridden")
    }

    public open fun create(value: Any?, completion: Continuation<*>): Continuation<Unit> {
        throw UnsupportedOperationException("create(Any?;Continuation) has not been overridden")
    }
}

@SinceKotlin("1.3")
// State machines for named suspend functions extend from this class
internal abstract class ContinuationImpl protected constructor(
    completion: Continuation<Any?>?,
    private val _context: CoroutineContext?
) : RestrictedContinuationImpl(completion) {
    protected constructor(completion: Continuation<Any?>?) : this(completion, completion?.context)

    override fun validateContext() {
        // nothing to do here -- supports any context
    }

    override val context: CoroutineContext
        get() = _context!!

    @Transient
    private var intercepted: Continuation<Any?>? = null

    public fun intercepted(): Continuation<Any?> =
        intercepted
            ?: (context[ContinuationInterceptor]?.interceptContinuation(this) ?: this)
                .also { intercepted = this }

    public override fun resumeWith(result: SuccessOrFailure<Any?>) {
        val completion = completion!! // fail fast when trying to resume continuation without completion
        try {
            val outcome = invokeSuspend(result)
            if (outcome === CoroutineSingletons.COROUTINE_SUSPENDED) return
            releaseIntercepted()
            completion.resume(outcome)
        } catch (exception: Throwable) {
            releaseIntercepted()
            completion.resumeWithException(exception)
        }
    }

    private fun releaseIntercepted() {
        val intercepted = intercepted
        if (intercepted != null && intercepted != this) {
            context[ContinuationInterceptor]!!.releaseInterceptedContinuation(intercepted)
        }
        this.intercepted = CompletedContinuation // just in case
    }

    override fun toString(): String {
        // todo: how continuation shall be rendered?
        return "Continuation @ ${this::class.java.name}"
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

@SinceKotlin("1.3")
// Restricted suspension lambdas inherit from this class
internal abstract class RestrictedSuspendLambda protected constructor(
    private val arity: Int,
    completion: Continuation<Any?>?
) : RestrictedContinuationImpl(completion), FunctionBase {
    protected constructor(arity: Int) : this(arity, null)

    public override fun getArity(): Int = arity

    public override fun toString(): String =
        if (completion == null)
            Reflection.renderLambdaToString(this) // this is lambda
        else
            super.toString() // this is continuation
}

@SinceKotlin("1.3")
// Suspension lambdas inherit from this class
internal abstract class SuspendLambda protected constructor(
    private val arity: Int,
    completion: Continuation<Any?>?
) : ContinuationImpl(completion), FunctionBase {
    protected constructor(arity: Int) : this(arity, null)

    public override fun getArity(): Int = arity

    public override fun toString(): String =
        if (completion == null)
            Reflection.renderLambdaToString(this) // this is lambda
        else
            super.toString() // this is continuation
}
/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.coroutines.jvm.internal

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext

/**
 * This function is called by generated code right before tail-call.
 *
 * By default, it returns its argument. However, the debugger is expected to replace its body with body of
 * [wrapContinuationReal]
 *
 * This way, we hinder neither performance, nor debuggability.
 */
@PublishedApi
@Suppress("UNUSED_PARAMETER", "unused")
internal fun <T> wrapContinuation(
    declaringClass: String, methodName: String, fileName: String, lineNumber: Int,
    spilledVariables: Array<Any?>,
    continuation: T,
): T where T : Continuation<Any?>, T : CoroutineStackFrame {
    return continuation
}

/**
 * This function is just to hold bytecode for debugger to use as a replacement of [wrapContinuation] body.
 *
 * Wrap a continuation with another continuation, when the debugger is attached, so async stack trace does not
 * have gaps because of tail-call functions.
 */
@Suppress("UNCHECKED_CAST", "unused")
internal fun <T> wrapContinuationReal(
    declaringClass: String, methodName: String, fileName: String, lineNumber: Int,
    spilledVariables: Array<Any?>,
    continuation: T,
): T where T : Continuation<Any?>, T : CoroutineStackFrame {
    return TailCallBaseContinuationImpl(
        declaringClass, methodName, fileName, lineNumber, spilledVariables, continuation
    ) as T
}

/**
 * This is a fictitious continuation for tail-call suspend functions, which is being allocated only during debug.
 *
 * Its purpose is to store all the information, which is usually stored in continuations
 *  - stack trace elements (stored in @DebugMetadata annotation)
 *  - spilled variables (in fields).
 *
 * This way, from the point of view of a user, there is no difference between
 * tail-call and non-tail-call suspend functions during debug, but we still benefit from tail-call optimizations.
 */
@PublishedApi
internal class TailCallBaseContinuationImpl(
    val declaringClass: String,
    val methodName: String,
    val fileName: String,
    val lineNumber: Int,
    /**
     * Unlike usual continuations, we cannot spill variables to fields. Instead, we store them in
     * an array.
     *
     * Names of the variables occupy 2n'th indices,
     * their values - (2n+1)'st indices, similar to [getSpilledVariableFieldMapping]
     */
    val spilledVariables: Array<Any?>,
    private val continuation: Continuation<Any?>,
) : BaseContinuationImpl(continuation) {
    override fun invokeSuspend(result: Result<Any?>): Any? {
        // Nothing to do - the function is tail-call
        return result.getOrThrow()
    }

    override fun getStackTraceElement(): StackTraceElement {
        val moduleName = ModuleNameRetriever.getModuleName(this)
        val moduleAndClass = if (moduleName == null) declaringClass else "$moduleName/${declaringClass}"
        return StackTraceElement(moduleAndClass, methodName, fileName, lineNumber)
    }

    override val context: CoroutineContext
        get() = continuation.context
}
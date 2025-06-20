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
    continuation: T,
): T where T : Continuation<Any?>, T : CoroutineStackFrame {
    return object : Continuation<Any?>, CoroutineStackFrame {
        override val context: CoroutineContext
            get() = continuation.context

        override fun resumeWith(result: Result<Any?>) {
            continuation.resumeWith(result)
        }

        override val callerFrame: CoroutineStackFrame?
            get() = continuation

        override fun getStackTraceElement(): StackTraceElement? {
            val moduleName = ModuleNameRetriever.getModuleName(this)
            val moduleAndClass = if (moduleName == null) declaringClass else "$moduleName/${declaringClass}"
            return StackTraceElement(moduleAndClass, methodName, fileName, lineNumber)
        }

        override fun toString(): String =
            "Continuation at ${getStackTraceElement() ?: this::class.java.name}"
    } as T
}
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.coroutines.native.internal.*
import kotlin.native.concurrent.*

@ExportForCppRuntime
private fun Kotlin_ObjCExport_createContinuationArgumentImpl(
        completionHolder: Any,
        exceptionTypes: NativePtr
): Continuation<Any?> = createContinuationArgumentFromCallback(EmptyCompletion) { result ->
    result.fold(
            onSuccess = { value ->
                runCompletionSuccess(completionHolder, value)
            },
            onFailure = { exception ->
                runCompletionFailure(completionHolder, exception, exceptionTypes)
            }
    )
}

private object EmptyCompletion : Continuation<Any?> {
    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<Any?>) {
        val exception = result.exceptionOrNull() ?: return
        TerminateWithUnhandledException(exception)
        // Throwing the exception from [resumeWith] is not generally expected.
        // Also terminating is consistent with other pieces of ObjCExport machinery.
    }
}

@PublishedApi
@ExportForCppRuntime("Kotlin_ObjCExport_resumeContinuationSuccess") // Also makes it a data flow root.
internal fun resumeContinuation(continuation: Continuation<Any?>, value: Any?) {
    continuation.resume(value)
}

@PublishedApi
@ExportForCppRuntime("Kotlin_ObjCExport_resumeContinuationFailure") // Also makes it a data flow root.
internal fun resumeContinuationWithException(continuation: Continuation<Any?>, exception: Throwable) {
    continuation.resumeWithException(exception)
}

@PublishedApi
@ExportForCompiler // Mark as data flow root.
internal fun getCoroutineSuspended(): Any = COROUTINE_SUSPENDED

@PublishedApi
@ExportForCompiler // Mark as data flow root.
internal fun interceptedContinuation(continuation: Continuation<Any?>): Continuation<Any?> = continuation.intercepted()

@FilterExceptions
@SymbolName("Kotlin_ObjCExport_runCompletionSuccess")
private external fun runCompletionSuccess(completionHolder: Any, result: Any?)

@FilterExceptions
@SymbolName("Kotlin_ObjCExport_runCompletionFailure")
private external fun runCompletionFailure(completionHolder: Any, exception: Throwable, exceptionTypes: NativePtr)
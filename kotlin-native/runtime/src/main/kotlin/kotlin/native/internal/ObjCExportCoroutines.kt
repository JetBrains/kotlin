/*
 * Copyright 2010-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
@file:OptIn(ExperimentalForeignApi::class)
package kotlin.native.internal

import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.internal.UsedFromCompilerGeneratedCode
import kotlin.native.internal.escapeAnalysis.Escapes

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

    @OptIn(ExperimentalNativeApi::class)
    override fun resumeWith(result: Result<Any?>) {
        val exception = result.exceptionOrNull() ?: return
        processUnhandledException(exception)
        terminateWithUnhandledException(exception)
        // Terminate even if unhandled exception hook has finished successfully, because
        // throwing the exception from [resumeWith] is not generally expected.
        // Also terminating is consistent with other pieces of ObjCExport machinery.
    }
}

@PublishedApi
@ExportForCppRuntime("Kotlin_ObjCExport_resumeContinuationSuccess") // Also makes it a data flow root.
@UsedFromCompilerGeneratedCode
internal fun resumeContinuation(continuation: Continuation<Any?>, value: Any?) {
    continuation.resume(value)
}

@PublishedApi
@ExportForCppRuntime("Kotlin_ObjCExport_resumeContinuationFailure") // Also makes it a data flow root.
@UsedFromCompilerGeneratedCode
internal fun resumeContinuationWithException(continuation: Continuation<Any?>, exception: Throwable) {
    continuation.resumeWithException(exception)
}

@PublishedApi
@ExportForCompiler // Mark as data flow root.
@UsedFromCompilerGeneratedCode
internal fun getCoroutineSuspended(): Any = COROUTINE_SUSPENDED

@PublishedApi
@ExportForCompiler // Mark as data flow root.
@UsedFromCompilerGeneratedCode
internal fun interceptedContinuation(continuation: Continuation<Any?>): Continuation<Any?> = continuation.intercepted()

@FilterExceptions
@GCUnsafeCall("Kotlin_ObjCExport_runCompletionSuccess")
@Escapes(0b010) // result escapes into a stable ref.
private external fun runCompletionSuccess(completionHolder: Any, result: Any?)

@FilterExceptions
@GCUnsafeCall("Kotlin_ObjCExport_runCompletionFailure")
@Escapes(0b0010) // exception escapes into a stable ref.
private external fun runCompletionFailure(completionHolder: Any, exception: Throwable, exceptionTypes: NativePtr)

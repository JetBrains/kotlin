/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror

import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.coroutine.util.isSubTypeOrSame
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

class StandaloneCoroutine private constructor(context: DefaultExecutionContext) :
    BaseMirror<ObjectReference, MirrorOfStandaloneCoroutine>("kotlinx.coroutines.StandaloneCoroutine", context) {
    private val state by FieldMirrorDelegate("_state", ChildContinuation(context)) // childContinuation
    private val contextFieldRef by FieldMirrorDelegate("context", CoroutineContext(context))

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfStandaloneCoroutine {
        val childContinuation = state.mirror(value, context)
        val coroutineContext = contextFieldRef.mirror(value, context)
        return MirrorOfStandaloneCoroutine(value, childContinuation, coroutineContext)
    }

    companion object {
        val log by logger

        fun instance(context: DefaultExecutionContext): StandaloneCoroutine? {
            return try {
                StandaloneCoroutine(context)
            } catch (e: IllegalStateException) {
                log.debug("Attempt to access DebugProbesImpl but none found.", e)
                null
            }
        }
    }
}

class ChildContinuation(context: DefaultExecutionContext) :
    BaseMirror<ObjectReference, MirrorOfChildContinuation>("kotlinx.coroutines.ChildContinuation", context) {
    private val childFieldRef by FieldMirrorDelegate("child", CancellableContinuationImpl(context)) // cancellableContinuationImpl

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfChildContinuation? {
        return MirrorOfChildContinuation(value, childFieldRef.mirror(value, context))
    }
}

class CancellableContinuationImpl(context: DefaultExecutionContext) :
    BaseMirror<ObjectReference, MirrorOfCancellableContinuationImpl>("kotlinx.coroutines.CancellableContinuationImpl", context) {
    private val decisionFieldRef by FieldDelegate<IntegerValue>("_decision")
    private val delegateFieldRef by FieldMirrorDelegate("delegate", DispatchedContinuation(context)) // DispatchedContinuation
    private val resumeModeFieldRef by FieldDelegate<IntegerValue>("resumeMode")
    private val submissionTimeFieldRef by FieldDelegate<LongValue>("submissionTime")
    private val contextFieldRef by FieldMirrorDelegate("context", CoroutineContext(context))

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfCancellableContinuationImpl? {
        val decision = decisionFieldRef.value(value)?.intValue()
        val dispatchedContinuation = delegateFieldRef.mirror(value, context)
        val submissionTime = submissionTimeFieldRef.value(value)?.longValue()
        val resumeMode = resumeModeFieldRef.value(value)?.intValue()
        val contextMirror = contextFieldRef.mirror(value, context)
        return MirrorOfCancellableContinuationImpl(value, decision, dispatchedContinuation, resumeMode, submissionTime, contextMirror)
    }
}

class DispatchedContinuation(context: DefaultExecutionContext) :
    BaseMirror<ObjectReference, MirrorOfDispatchedContinuation>("kotlinx.coroutines.DispatchedContinuation", context) {
    private val continuation by FieldDelegate<ObjectReference>("continuation")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfDispatchedContinuation? {
        return MirrorOfDispatchedContinuation(value, continuation.value(value))
    }
}

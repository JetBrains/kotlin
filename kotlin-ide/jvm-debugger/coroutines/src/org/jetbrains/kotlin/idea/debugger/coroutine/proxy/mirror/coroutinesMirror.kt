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
    BaseMirror<MirrorOfStandaloneCoroutine>("kotlinx.coroutines.StandaloneCoroutine", context) {
    private val coroutineContextMirror = CoroutineContext(context)
    private val childContinuationMirror = ChildContinuation(context)
    private val stateFieldRef = makeField("_state") // childContinuation
    private val contextFieldRef = makeField("context")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfStandaloneCoroutine {
        val state = objectValue(value, stateFieldRef)
        val childContinuation = childContinuationMirror.mirror(state, context)
        val cc = objectValue(value, contextFieldRef)
        val coroutineContext = coroutineContextMirror.mirror(cc, context)
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
    BaseMirror<MirrorOfChildContinuation>("kotlinx.coroutines.ChildContinuation", context) {
    private val childContinuationMirror = CancellableContinuationImpl(context)
    private val childFieldRef = makeField("child") // cancellableContinuationImpl

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfChildContinuation? {
        val child = objectValue(value, childFieldRef)
        return MirrorOfChildContinuation(value, childContinuationMirror.mirror(child, context))
    }
}

class CancellableContinuationImpl(context: DefaultExecutionContext) :
    BaseMirror<MirrorOfCancellableContinuationImpl>("kotlinx.coroutines.CancellableContinuationImpl", context) {
    private val coroutineContextMirror = CoroutineContext(context)
    private val dispatchedContinuationMirror = DispatchedContinuation(context)
    private val decisionFieldRef = makeField("_decision")
    private val delegateFieldRef = makeField("delegate") // DispatchedContinuation
    private val resumeModeFieldRef = makeField("resumeMode")
    private val submissionTimeFieldRef = makeField("submissionTime")
    private val contextFieldRef = makeField("context")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfCancellableContinuationImpl? {
        val decision = intValue(value, decisionFieldRef)
        val dispatchedContinuation = dispatchedContinuationMirror.mirror(objectValue(value, delegateFieldRef), context)
        val submissionTime = longValue(value, submissionTimeFieldRef)
        val resumeMode = intValue(value, resumeModeFieldRef)
        val coroutineContext = objectValue(value, contextFieldRef)
        val contextMirror = coroutineContextMirror.mirror(coroutineContext, context)
        return MirrorOfCancellableContinuationImpl(value, decision, dispatchedContinuation, resumeMode, submissionTime, contextMirror)
    }
}

class DispatchedContinuation(context: DefaultExecutionContext) :
    BaseMirror<MirrorOfDispatchedContinuation>("kotlinx.coroutines.DispatchedContinuation", context) {
    private val decisionFieldRef = makeField("continuation")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfDispatchedContinuation? {
        val continuation = objectValue(value, decisionFieldRef)
        return MirrorOfDispatchedContinuation(value, continuation)
    }
}

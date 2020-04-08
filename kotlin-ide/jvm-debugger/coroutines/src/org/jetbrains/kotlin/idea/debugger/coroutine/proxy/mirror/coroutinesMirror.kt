/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror

import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.isSubTypeOrSame
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext


abstract class BaseMirror<T>(val name: String, context: DefaultExecutionContext) {
    val log by logger
    protected val cls = context.findClass(name) ?: throw IllegalStateException("Can't find class ${name} in remote jvm.")

    fun makeField(fieldName: String): Field =
        cls.fieldByName(fieldName) // childContinuation

    fun makeMethod(methodName: String): Method =
        cls.methodsByName(methodName).single()

    fun isCompatible(value: ObjectReference) =
        value.referenceType().isSubTypeOrSame(name)

    fun mirror(value: ObjectReference, context: DefaultExecutionContext): T? {
        if (!isCompatible(value)) {
            log.warn("Value ${value.referenceType()} is not compatible with $name.")
            return null
        } else
            return fetchMirror(value, context)
    }

    fun staticObjectValue(fieldName: String): ObjectReference {
        val keyFieldRef = makeField(fieldName)
        return cls.getValue(keyFieldRef) as ObjectReference
    }

    fun stringValue(value: ObjectReference, field: Field) =
        (value.getValue(field) as StringReference).value()

    fun stringValue(value: ObjectReference, method: Method, context: DefaultExecutionContext) =
        (context.invokeMethod(value, method, emptyList()) as StringReference).value()

    fun objectValue(value: ObjectReference, method: Method, context: DefaultExecutionContext, vararg values: Value) =
        context.invokeMethodAsObject(value, method, *values)

    fun longValue(value: ObjectReference, method: Method, context: DefaultExecutionContext, vararg values: Value) =
        (context.invokeMethodAsObject(value, method, *values) as LongValue).longValue()

    fun objectValue(value: ObjectReference, field: Field) =
        value.getValue(field) as ObjectReference

    fun intValue(value: ObjectReference, field: Field) =
        (value.getValue(field) as IntegerValue).intValue()

    fun longValue(value: ObjectReference, field: Field) =
        (value.getValue(field) as LongValue).longValue()

    protected abstract fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): T?
}

class StandaloneCoroutine(context: DefaultExecutionContext) :
    BaseMirror<MirrorOfStandaloneCoroutine>("kotlinx.coroutines.StandaloneCoroutine", context) {
    private val coroutineContextMirror = CoroutineContext(context)
    private val childContinuationMirror = ChildContinuation(context)
    private val stateFieldRef: Field = makeField("_state") // childContinuation
    private val contextFieldRef: Field = makeField("context")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfStandaloneCoroutine {
        val state = objectValue(value, stateFieldRef)
        val childcontinuation = childContinuationMirror.mirror(state, context)
        val cc = objectValue(value, contextFieldRef)
        val coroutineContext = coroutineContextMirror.mirror(cc, context)
        return MirrorOfStandaloneCoroutine(value, childcontinuation, coroutineContext)
    }

}

data class MirrorOfStandaloneCoroutine(
    val that: ObjectReference,
    val state: MirrorOfChildContinuation?,
    val context: MirrorOfCoroutineContext?
)

class ChildContinuation(context: DefaultExecutionContext) :
    BaseMirror<MirrorOfChildContinuation>("kotlinx.coroutines.ChildContinuation", context) {
    private val childContinuationMirror = CancellableContinuationImpl(context)
    private val childFieldRef: Field = makeField("child") // cancellableContinuationImpl

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfChildContinuation? {
        val child = objectValue(value, childFieldRef)
        return MirrorOfChildContinuation(value, childContinuationMirror.mirror(child, context))
    }
}

data class MirrorOfChildContinuation(
    val that: ObjectReference,
    val child: MirrorOfCancellableContinuationImpl?
)

class CancellableContinuationImpl(context: DefaultExecutionContext) :
    BaseMirror<MirrorOfCancellableContinuationImpl>("kotlinx.coroutines.CancellableContinuationImpl", context) {
    private val coroutineContextMirror = CoroutineContext(context)
    private val dispatchedContinuationtMirror = DispatchedContinuation(context)
    private val decisionFieldRef: Field = makeField("_decision")
    private val delegateFieldRef: Field = makeField("delegate") // DispatchedContinuation
    private val resumeModeFieldRef: Field = makeField("resumeMode")
    private val submissionTimeFieldRef: Field = makeField("submissionTime")
    private val contextFieldRef: Field = makeField("context")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfCancellableContinuationImpl? {
        val decision = intValue(value, decisionFieldRef)
        val dispatchedContinuation = dispatchedContinuationtMirror.mirror(objectValue(value, delegateFieldRef), context)
        val submissionTime = longValue(value, submissionTimeFieldRef)
        val resumeMode = intValue(value, resumeModeFieldRef)
        val coroutineContext = objectValue(value, contextFieldRef)
        val contextMirror = coroutineContextMirror.mirror(coroutineContext, context)
        return MirrorOfCancellableContinuationImpl(value, decision, dispatchedContinuation, resumeMode, submissionTime, contextMirror)
    }
}

data class MirrorOfCancellableContinuationImpl(
    val that: ObjectReference,
    val decision: Int,
    val delegate: MirrorOfDispatchedContinuation?,
    val resumeMode: Int,
    val submissionTyme: Long,
    val jobContext: MirrorOfCoroutineContext?
)

class DispatchedContinuation(context: DefaultExecutionContext) :
    BaseMirror<MirrorOfDispatchedContinuation>("kotlinx.coroutines.DispatchedContinuation", context) {
    private val decisionFieldRef: Field = makeField("continuation")

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfDispatchedContinuation? {
        val continuation = objectValue(value, decisionFieldRef)
        return MirrorOfDispatchedContinuation(value, continuation)
    }
}

data class MirrorOfDispatchedContinuation(
    val that: ObjectReference,
    val continuation: ObjectReference?,
)

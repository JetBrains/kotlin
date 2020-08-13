/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror

import com.sun.jdi.ArrayReference
import com.sun.jdi.ObjectReference
import com.sun.jdi.StringReference
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

class DebugMetadata private constructor(context: DefaultExecutionContext) :
    BaseMirror<MirrorOfDebugProbesImpl>("kotlin.coroutines.jvm.internal.DebugMetadataKt", context) {
    private val getStackTraceElementMethod = makeMethod("getStackTraceElement")
    private val getSpilledVariableFieldMappingMethod =
        makeMethod("getSpilledVariableFieldMapping", "(Lkotlin/coroutines/jvm/internal/BaseContinuationImpl;)[Ljava/lang/String;")
    val baseContinuationImpl = BaseContinuationImpl(context, this)

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfDebugProbesImpl? =
        throw IllegalStateException("Not meant to be mirrored.")

    fun fetchContinuationStack(continuation: ObjectReference, context: DefaultExecutionContext): MirrorOfContinuationStack {
        val coroutineStack = mutableListOf<MirrorOfStackFrame>()
        var loopContinuation: ObjectReference? = continuation
        while (loopContinuation != null) {
            val continuationMirror = baseContinuationImpl.mirror(loopContinuation, context) ?: break
            coroutineStack.add(MirrorOfStackFrame(loopContinuation, continuationMirror))
            loopContinuation = continuationMirror.nextContinuation
        }
        return MirrorOfContinuationStack(continuation, coroutineStack)
    }

    fun getStackTraceElement(value: ObjectReference, context: DefaultExecutionContext) =
        staticMethodValue(getStackTraceElementMethod, context, value)

    fun getSpilledVariableFieldMapping(value: ObjectReference, context: DefaultExecutionContext) =
        staticMethodValue(getSpilledVariableFieldMappingMethod, context, value) as? ArrayReference

    companion object {
        val log by logger

        fun instance(context: DefaultExecutionContext): DebugMetadata? {
            try {
                return DebugMetadata(context)
            } catch (e: IllegalStateException) {
                log.debug("Attempt to access DebugMetadata but none found.", e)
            }
            return null
        }
    }
}

class BaseContinuationImpl(context: DefaultExecutionContext, private val debugMetadata: DebugMetadata) :
    BaseMirror<MirrorOfBaseContinuationImpl>("kotlin.coroutines.jvm.internal.BaseContinuationImpl", context) {

    private val getCompletion = makeMethod("getCompletion", "()Lkotlin/coroutines/Continuation;")
    private val stackTraceElement = StackTraceElement(context)

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfBaseContinuationImpl? {
        val stackTraceObjectReference = debugMetadata.getStackTraceElement(value, context) ?: return null
        val stackTraceElementMirror = stackTraceElement.mirror(stackTraceObjectReference, context)
        val fieldVariables = getSpilledVariableFieldMapping(value, context)
        val completionValue = objectValue(value, getCompletion, context)
        val completion = if (completionValue != null && isCompatible(completionValue)) completionValue else null
        val coroutineOwner =
            if (completionValue != null && DebugProbesImplCoroutineOwner.instanceOf(completionValue)) completionValue else null
        return MirrorOfBaseContinuationImpl(value, stackTraceElementMirror, fieldVariables, completion, coroutineOwner)
    }

    private fun getSpilledVariableFieldMapping(value: ObjectReference, context: DefaultExecutionContext): List<FieldVariable> {
        val getSpilledVariableFieldMappingReference =
            debugMetadata.getSpilledVariableFieldMapping(value, context) ?: return emptyList()

        val length = getSpilledVariableFieldMappingReference.length() / 2
        val fieldVariables = ArrayList<FieldVariable>()
        for (index in 0 until length) {
            val fieldVariable = getFieldVariableName(getSpilledVariableFieldMappingReference, index) ?: continue
            fieldVariables.add(fieldVariable)
        }
        return fieldVariables
    }

    private fun getFieldVariableName(rawSpilledVariables: ArrayReference, index: Int): FieldVariable? {
        val fieldName = (rawSpilledVariables.getValue(2 * index) as? StringReference)?.value() ?: return null
        val variableName = (rawSpilledVariables.getValue(2 * index + 1) as? StringReference)?.value() ?: return null
        return FieldVariable(fieldName, variableName)
    }
}

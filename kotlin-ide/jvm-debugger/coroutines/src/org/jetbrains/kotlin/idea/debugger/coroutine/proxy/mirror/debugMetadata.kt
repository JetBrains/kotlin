/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror

import com.sun.jdi.ArrayReference
import com.sun.jdi.ClassType
import com.sun.jdi.ObjectReference
import com.sun.jdi.StringReference
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.isBaseContinuationImpl
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

class DebugMetadata(context: DefaultExecutionContext) :
    BaseMirror<MirrorOfDebugProbesImpl>("kotlin.coroutines.jvm.internal.DebugMetadataKt", context) {
    val getStackTraceElementMethod = makeMethod("getStackTraceElement")
    val getSpilledVariableFieldMappingMethod = makeMethod("getSpilledVariableFieldMapping", "(Lkotlin/coroutines/jvm/internal/BaseContinuationImpl;)[Ljava/lang/String;")
    val stackTraceElement = StackTraceElement(context)

    override fun fetchMirror(value: ObjectReference, context: DefaultExecutionContext): MirrorOfDebugProbesImpl? {
        return null
    }

    fun getStackTraceElement(value: ObjectReference, context: DefaultExecutionContext): MirrorOfStackTraceElement? {
        if (value.referenceType().isBaseContinuationImpl()) {
            val stackTraceObjectReference = staticMethodValue(getStackTraceElementMethod, context, value) ?: return null
            return stackTraceElement.mirror(stackTraceObjectReference, context)
        } else
            return null
    }

    fun getSpilledVariableFieldMapping(value: ObjectReference, context: DefaultExecutionContext): List<FieldVariable> {
        val getSpilledVariableFieldMappingReference = staticMethodValue(getSpilledVariableFieldMappingMethod, context, value) as? ArrayReference ?: return emptyList()

        val length = getSpilledVariableFieldMappingReference.length() / 2
        val fieldVariables = ArrayList<FieldVariable>()
        for (index in 0 until length) {
            fieldVariables.add(getFieldVariableName(getSpilledVariableFieldMappingReference, index) ?: continue)
        }
        return fieldVariables
    }

    private fun getFieldVariableName(rawSpilledVariables: ArrayReference, index: Int): FieldVariable? {
        val fieldName = (rawSpilledVariables.getValue(2 * index) as? StringReference)?.value() ?: return null
        val variableName = (rawSpilledVariables.getValue(2 * index + 1) as? StringReference)?.value() ?: return null
        return FieldVariable(fieldName, variableName)
    }

    companion object {
        fun instance(context: DefaultExecutionContext) =
            try {
                DebugMetadata(context)
            } catch (e : IllegalStateException) {
                null
            }
    }
}

data class FieldVariable(val fieldName: String, val variableName: String)
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy

import com.intellij.debugger.engine.JavaValue
import com.sun.jdi.ObjectReference
import org.jetbrains.kotlin.idea.debugger.coroutine.data.*
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror.*
import org.jetbrains.kotlin.idea.debugger.coroutine.util.isAbstractCoroutine
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

class ContinuationHolder private constructor(val context: DefaultExecutionContext) {
    private val debugMetadata: DebugMetadata? = DebugMetadata.instance(context)
    private val locationCache = LocationCache(context)
    private val debugProbesImpl = DebugProbesImpl.instance(context)
    private val log by logger

    fun extractCoroutineInfoData(continuation: ObjectReference): CoroutineInfoData? {
        try {
            val consumer = mutableListOf<CoroutineStackFrameItem>()
            val continuationStack = debugMetadata?.fetchContinuationStack(continuation, context) ?: return null
            for (frame in continuationStack.coroutineStack) {
                val coroutineStackFrame = createStackFrameItem(frame)
                if (coroutineStackFrame != null)
                    consumer.add(coroutineStackFrame)
            }
            val lastRestoredFrame = continuationStack.coroutineStack.lastOrNull()
            return findCoroutineInformation(lastRestoredFrame?.baseContinuationImpl?.coroutineOwner, consumer)
        } catch (e: Exception) {
            log.error("Error while looking for stack frame.", e)
        }
        return null
    }

    private fun findCoroutineInformation(
        input: ObjectReference?,
        stackFrameItems: List<CoroutineStackFrameItem>
    ): CoroutineInfoData? {
        val creationStackTrace = mutableListOf<CreationCoroutineStackFrameItem>()
        val realState = if (input?.type()?.isAbstractCoroutine() == true) {
            state(input) ?: return null
        } else {
            val ci = debugProbesImpl?.getCoroutineInfo(input, context)
            if (ci != null) {
                if (ci.creationStackTrace != null)
                    for (index in ci.creationStackTrace.indices) {
                        val frame = ci.creationStackTrace[index]
                        val ste = frame.stackTraceElement()
                        val location = locationCache.createLocation(ste)
                        creationStackTrace.add(CreationCoroutineStackFrameItem(ste, location, index == 0))
                    }
                CoroutineNameIdState.instance(ci)
            } else {
                CoroutineNameIdState(CoroutineInfoData.DEFAULT_COROUTINE_NAME, "-1", State.UNKNOWN, null)
            }
        }
        return CoroutineInfoData(realState, stackFrameItems, creationStackTrace)
    }

    fun state(value: ObjectReference?): CoroutineNameIdState? {
        value ?: return null
        val reference = JavaLangMirror(context)
        val standaloneCoroutine = StandaloneCoroutine.instance(context) ?: return null
        val standAloneCoroutineMirror = standaloneCoroutine.mirror(value, context)
        if (standAloneCoroutineMirror?.context is MirrorOfCoroutineContext) {
            val id = standAloneCoroutineMirror.context.id
            val name = standAloneCoroutineMirror.context.name ?: CoroutineInfoData.DEFAULT_COROUTINE_NAME
            val toString = reference.string(value, context)
            // trying to get coroutine information by calling JobSupport.toString(), ${nameString()}{${stateString(state)}}@$hexAddress
            val r = """\w+\{(\w+)}@([\w\d]+)""".toRegex()
            val matcher = r.toPattern().matcher(toString)
            if (matcher.matches()) {
                val state = stateOf(matcher.group(1))
                val hexAddress = matcher.group(2)
                return CoroutineNameIdState(name, id?.toString() ?: hexAddress, state, standAloneCoroutineMirror.context.dispatcher)
            }
        }
        return null
    }

    private fun createStackFrameItem(
        frame: MirrorOfStackFrame
    ): DefaultCoroutineStackFrameItem? {
        val stackTraceElement = frame.baseContinuationImpl.stackTraceElement?.stackTraceElement() ?: return null
        val locationClass = context.findClassSafe(stackTraceElement.className)
        val generatedLocation = locationCache.createLocation(locationClass, stackTraceElement.methodName, stackTraceElement.lineNumber)
        val spilledVariables = frame.baseContinuationImpl.spilledValues(context)
        return DefaultCoroutineStackFrameItem(generatedLocation, spilledVariables)
    }

    companion object {
        val log by logger

        fun instance(context: DefaultExecutionContext) =
            ContinuationHolder(context)

        private fun stateOf(state: String?): State =
            when (state) {
                "Active" -> State.RUNNING
                "Cancelling" -> State.SUSPENDED_CANCELLING
                "Completing" -> State.SUSPENDED_COMPLETING
                "Cancelled" -> State.CANCELLED
                "Completed" -> State.COMPLETED
                "New" -> State.NEW
                else -> State.UNKNOWN
            }
    }
}

fun MirrorOfBaseContinuationImpl.spilledValues(context: DefaultExecutionContext): List<JavaValue> {
    return fieldVariables.map {
        it.toJavaValue(that, context)
    }
}

fun FieldVariable.toJavaValue(continuation: ObjectReference, context: DefaultExecutionContext): JavaValue {
    val valueDescriptor = ContinuationVariableValueDescriptorImpl(
        context,
        continuation,
        fieldName,
        variableName
    )
    return JavaValue.create(
        null,
        valueDescriptor,
        context.evaluationContext,
        context.debugProcess.xdebugProcess!!.nodeManager,
        false
    )
}

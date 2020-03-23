/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy

import com.intellij.debugger.engine.JavaValue
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.jdi.GeneratedLocation
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.xdebugger.frame.XNamedValue
import com.intellij.xdebugger.frame.XStackFrame
import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.coroutine.coroutineDebuggerTraceEnabled
import org.jetbrains.kotlin.idea.debugger.coroutine.data.*
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror.DebugMetadata
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.mirror.FieldVariable
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext
import org.jetbrains.kotlin.idea.debugger.invokeInManagerThread
import org.jetbrains.kotlin.idea.debugger.safeLocation

data class ContinuationHolder(val continuation: ObjectReference, val context: DefaultExecutionContext) {
    val log by logger
    private val debugMetadata: DebugMetadata? = DebugMetadata.instance(context)

    fun getCoroutineInfoData(): CoroutineInfoData? {
        try {
            return collectCoroutineInfo()
        } catch (e: Exception) {
            log.error("Error while looking for stack frame.", e)
        }
        return null
    }

    private fun collectCoroutineInfo(): CoroutineInfoData? {
        val consumer = mutableListOf<CoroutineStackFrameItem>()
        var completion = this
        while (completion.isBaseContinuationImpl()) {
            val coroutineStackFrame = context.debugProcess.invokeInManagerThread {
                completion.createLocation()
            }
            if (coroutineStackFrame != null) {
                consumer.add(coroutineStackFrame)
            }
            completion = completion.findCompletion() ?: break
        }
        return CoroutineInfoData.lookup(completion.value(), context, consumer)
    }

    private fun createLocation(): DefaultCoroutineStackFrameItem? {
        val stackTraceElementMirror = debugMetadata?.getStackTraceElement(continuation, context) ?: return null
        val stackTraceElement = stackTraceElementMirror.stackTraceElement()
        val locationClass = context.findClassSafe(stackTraceElement.className) ?: return null
        val generatedLocation =
            GeneratedLocation(context.debugProcess, locationClass, stackTraceElement.methodName, stackTraceElement.lineNumber)
        val spilledVariables = getSpilledVariables() ?: emptyList()
        return DefaultCoroutineStackFrameItem(generatedLocation, spilledVariables)
    }

    fun getSpilledVariables(): List<XNamedValue>? {
        val variables: List<JavaValue> = context.debugProcess.invokeInManagerThread {
            debugMetadata?.getSpilledVariableFieldMapping(continuation, context)?.mapNotNull {
                fieldVariableToNamedValue(it, this)
            }
        } ?: emptyList()
        return variables
    }

    fun fieldVariableToNamedValue(fieldVariable: FieldVariable, continuation: ContinuationHolder): JavaValue {
        val valueDescriptor = ContinuationValueDescriptorImpl(
            context.project,
            continuation,
            fieldVariable.fieldName,
            fieldVariable.variableName
        )
        return JavaValue.create(
            null,
            valueDescriptor,
            context.evaluationContext,
            context.debugProcess.xdebugProcess!!.nodeManager,
            false
        )
    }

    fun referenceType(): ClassType? =
        continuation.referenceType() as? ClassType

    fun value() =
        continuation

    fun field(field: Field): Value? =
        continuation.getValue(field)

    fun findCompletion(): ContinuationHolder? {
        val type = continuation.type()
        if (type is ClassType && type.isBaseContinuationImpl()) {
            val completionField = type.completionField() ?: return null
            return ContinuationHolder(continuation.getValue(completionField) as? ObjectReference ?: return null, context)
        }
        return null
    }

    fun isBaseContinuationImpl() =
        continuation.type().isBaseContinuationImpl()

    companion object {
        val log by logger

        fun coroutineExitFrame(
            frame: StackFrameProxyImpl,
            suspendContext: SuspendContextImpl?
        ): XStackFrame? {
            suspendContext ?: return null
            return suspendContext.invokeInManagerThread {
                if (frame.location().isPreFlight() || frame.location().isPreExitFrame()) {
                    if (coroutineDebuggerTraceEnabled())
                        log.trace("Entry frame found: ${formatLocation(frame.location())}")
                    val leftThreadStack = leftThreadStack(frame) ?: return@invokeInManagerThread null
                    lookupContinuation(suspendContext, frame, leftThreadStack)
                } else
                    null
            }
        }

        fun leftThreadStack(frame: StackFrameProxyImpl): List<StackFrameProxyImpl>? {
            var frames = frame.threadProxy().frames()
            val indexOfCurrentFrame = frames.indexOf(frame)
            if (indexOfCurrentFrame >= 0) {
                val indexofGetCoroutineSuspended = hasGetCoroutineSuspended(frames)
                // @TODO if found - skip this thread stack
                if (indexofGetCoroutineSuspended >= 0)
                    return null
                return frames.drop(indexOfCurrentFrame)
            } else
                return null
        }

        fun lookupContinuation(
            suspendContext: SuspendContextImpl,
            previousFrame: StackFrameProxyImpl, // invokeSuspend
            framesLeft: List<StackFrameProxyImpl>
        ): CoroutinePreflightStackFrame? {
            val continuation =
                if (previousFrame.safeLocation()?.method()?.isSuspendLambda() ?: false ||
                    previousFrame.safeLocation()?.method()?.isContinuation() ?: false
                )
                    getThisContinuation(previousFrame)
                else
                    null

            if (continuation != null) {
                val context = suspendContext.executionContext() ?: return null
                val coroutineStackTrace = ContinuationHolder(continuation, context).getCoroutineInfoData() ?: return null
                return CoroutinePreflightStackFrame.preflight(
                    previousFrame,
                    coroutineStackTrace,
                    framesLeft
                )
            } else
                return null
        }

        private fun getCompletionContinuation(previousFrame: StackFrameProxyImpl?) =
            previousFrame?.completion1VariableValue()

        private fun getThisContinuation(previousFrame: StackFrameProxyImpl?): ObjectReference? =
            previousFrame?.thisVariableValue()


        private fun formatLocation(location: Location): String {
            return "${location.method().name()}:${location.lineNumber()}, ${location.method().declaringType()} in ${location.sourceName()}"
        }

        /**
         * Find continuation for the [frame]
         * Gets current CoroutineInfo.lastObservedFrame and finds next frames in it until null or needed stackTraceElement is found
         * @return null if matching continuation is not found or is not BaseContinuationImpl
         */
        fun lookup(
            context: SuspendContextImpl,
            initialContinuation: ObjectReference?,
        ): ContinuationHolder? {
            var continuation = initialContinuation ?: return null
            val executionContext = context.executionContext() ?: return null

            do {
                continuation = getNextFrame(executionContext, continuation) ?: return null
            } while (continuation.type().isBaseContinuationImpl()  /* && position != classLine */)

            return if (continuation.type().isBaseContinuationImpl())
                ContinuationHolder(continuation, executionContext)
            else
                return null
        }
    }

}


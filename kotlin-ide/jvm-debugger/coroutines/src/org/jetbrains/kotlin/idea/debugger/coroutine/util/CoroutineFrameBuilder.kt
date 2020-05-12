/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.util

import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.debugger.ui.impl.watch.MethodsTracker
import com.intellij.debugger.ui.impl.watch.StackFrameDescriptorImpl
import com.sun.jdi.ObjectReference
import org.jetbrains.kotlin.idea.debugger.coroutine.data.*
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.ContinuationHolder
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.SkipCoroutineStackFrameProxyImpl
import java.lang.Integer.min


class CoroutineFrameBuilder {

    companion object {
        val log by logger
        private const val PRE_FETCH_FRAME_COUNT = 5

        fun build(coroutine: CoroutineInfoData, suspendContext: SuspendContextImpl): DoubleFrameList? =
            when {
                coroutine.isRunning() -> buildStackFrameForActive(coroutine, suspendContext)
                coroutine.isSuspended() -> DoubleFrameList(coroutine.stackTrace, coroutine.creationStackTrace)
                else -> null
            }

        private fun buildStackFrameForActive(coroutine: CoroutineInfoData, suspendContext: SuspendContextImpl): DoubleFrameList? {
            val activeThread = coroutine.activeThread ?: return null

            val coroutineStackFrameList = mutableListOf<CoroutineStackFrameItem>()
            val threadReferenceProxyImpl = ThreadReferenceProxyImpl(suspendContext.debugProcess.virtualMachineProxy, activeThread)
            val realFrames = threadReferenceProxyImpl.forceFrames()
            for (runningStackFrameProxy in realFrames) {
                val preflightStackFrame = coroutineExitFrame(runningStackFrameProxy, suspendContext)
                if (preflightStackFrame != null) {
                    buildRealStackFrameItem(preflightStackFrame.stackFrameProxy)?.let {
                        coroutineStackFrameList.add(it)
                    }

                    val doubleFrameList = build(preflightStackFrame, suspendContext)
                    coroutineStackFrameList.addAll(doubleFrameList.stackTrace)
                    return DoubleFrameList(coroutineStackFrameList, doubleFrameList.creationStackTrace)
                } else {
                    buildRealStackFrameItem(runningStackFrameProxy)?.let {
                        coroutineStackFrameList.add(it)
                    }
                }
            }
            return DoubleFrameList(coroutineStackFrameList, emptyList())
        }

        /**
         * Used by CoroutineAsyncStackTraceProvider to build XFramesView
         */
        fun build(preflightFrame: CoroutinePreflightStackFrame, suspendContext: SuspendContextImpl): DoubleFrameList {
            val stackFrames = mutableListOf<CoroutineStackFrameItem>()

            stackFrames.addAll(preflightFrame.restoredStackTrace())

            // rest of the stack
            // @TODO perhaps we need to merge the dropped frame below with the last restored (at least variables).
            val framesLeft = preflightFrame.threadPreCoroutineFrames.drop(1)
            stackFrames.addAll(framesLeft.mapIndexedNotNull { index, stackFrameProxyImpl ->
                suspendContext.invokeInManagerThread { buildRealStackFrameItem(stackFrameProxyImpl) }
            })

            return DoubleFrameList(stackFrames, preflightFrame.coroutineInfoData.creationStackTrace)
        }

        data class DoubleFrameList(
            val stackTrace: List<CoroutineStackFrameItem>,
            val creationStackTrace: List<CreationCoroutineStackFrameItem>
        )

        private fun buildRealStackFrameItem(
            frame: StackFrameProxyImpl
        ): RunningCoroutineStackFrameItem? {
            val location = frame.location()
            return if (!location.safeCoroutineExitPointLineNumber())
                RunningCoroutineStackFrameItem(SkipCoroutineStackFrameProxyImpl(frame), location)
            else
                null
        }

        /**
         * Used by CoroutineStackFrameInterceptor to check if that frame is 'exit' coroutine frame.
         */
        fun coroutineExitFrame(
            frame: StackFrameProxyImpl,
            suspendContext: SuspendContextImpl
        ): CoroutinePreflightStackFrame? {
            return suspendContext.invokeInManagerThread {
                val sem = frame.location().isPreFlight()
                if (sem.isCoroutineFound()) {
                    lookupContinuation(suspendContext, frame, sem)
                } else
                    null

            }
        }

        fun lookupContinuation(
            suspendContext: SuspendContextImpl,
            frame: StackFrameProxyImpl,
            mode: SuspendExitMode
        ): CoroutinePreflightStackFrame? {
            if (!mode.isCoroutineFound())
                return null

            val theFollowingFrames = theFollowingFrames(frame) ?: emptyList()
            val suspendParameterFrame = if (mode.isSuspendMethodParameter()) {
                if (theFollowingFrames.isNotEmpty()) {
                    // have to check next frame if that's invokeSuspend:-1 before proceed, otherwise skip
                    lookForTheFollowingFrame(theFollowingFrames) ?: return null
                } else
                    return null
            } else
                null

            if (threadAndContextSupportsEvaluation(suspendContext, frame)) {
                val context = suspendContext.executionContext() ?: return null
                val continuation = when (mode) {
                    SuspendExitMode.SUSPEND_LAMBDA -> getThisContinuation(frame)
                    SuspendExitMode.SUSPEND_METHOD_PARAMETER -> getLVTContinuation(frame)
                    else -> null
                } ?: return null

                val continuationHolder = ContinuationHolder.instance(context)
                val coroutineInfo = continuationHolder.extractCoroutineInfoData(continuation) ?: return null
                val descriptor = StackFrameDescriptorImpl(frame, MethodsTracker())
                return CoroutinePreflightStackFrame(
                    coroutineInfo,
                    descriptor,
                    theFollowingFrames,
                    mode
                )
            }
            return null
        }

        private fun lookForTheFollowingFrame(theFollowingFrames: List<StackFrameProxyImpl>): StackFrameProxyImpl? {
            for (i in 0 until min(PRE_FETCH_FRAME_COUNT, theFollowingFrames.size)) { // pre-scan PRE_FETCH_FRAME_COUNT frames
                val nextFrame = theFollowingFrames[i]
                if (nextFrame.location().isPreFlight() == SuspendExitMode.SUSPEND_METHOD) {
                    return nextFrame
                }
            }
            return null
        }

        private fun getLVTContinuation(frame: StackFrameProxyImpl?) =
            frame?.continuationVariableValue()

        private fun getThisContinuation(frame: StackFrameProxyImpl?): ObjectReference? =
            frame?.thisVariableValue()

        private fun theFollowingFrames(frame: StackFrameProxyImpl): List<StackFrameProxyImpl>? {
            val frames = frame.threadProxy().frames()
            val indexOfCurrentFrame = frames.indexOf(frame)
            if (indexOfCurrentFrame >= 0) {
                val indexOfGetCoroutineSuspended = hasGetCoroutineSuspended(frames)
                // @TODO if found - skip this thread stack
                if (indexOfGetCoroutineSuspended < 0 && frames.size > indexOfCurrentFrame + 1)
                    return frames.drop(indexOfCurrentFrame + 1)
            } else {
                log.error("Frame isn't found on the thread stack.")
            }
            return null
        }
    }
}

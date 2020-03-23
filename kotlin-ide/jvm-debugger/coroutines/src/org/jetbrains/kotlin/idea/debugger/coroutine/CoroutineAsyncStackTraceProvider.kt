/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine

import com.intellij.debugger.engine.AsyncStackTraceProvider
import com.intellij.debugger.engine.JavaStackFrame
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.memory.utils.StackFrameItem
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineInfoData
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.*
import org.jetbrains.kotlin.idea.debugger.coroutine.data.PreCoroutineStackFrameItem
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineStackFrameItem
import org.jetbrains.kotlin.idea.debugger.hopelessAware
import org.jetbrains.kotlin.idea.debugger.isInKotlinSources

class CoroutineAsyncStackTraceProvider : AsyncStackTraceProvider {

    override fun getAsyncStackTrace(stackFrame: JavaStackFrame, suspendContext: SuspendContextImpl): List<StackFrameItem>? {
        val stackFrameList = hopelessAware {
            if (stackFrame is CoroutinePreflightStackFrame)
                lookupForAfterPreflight(stackFrame, suspendContext)
            else
                null
        }
        return if (stackFrameList == null || stackFrameList.isEmpty())
            null
        else stackFrameList
    }

    private fun lookupForAfterPreflight(
        stackFrame: CoroutinePreflightStackFrame,
        suspendContext: SuspendContextImpl
    ): List<CoroutineStackFrameItem>? {
        val resumeWithFrame = stackFrame.threadPreCoroutineFrames.firstOrNull()

        if (threadAndContextSupportsEvaluation(suspendContext, resumeWithFrame)) {
            val stackFrames = mutableListOf<CoroutineStackFrameItem>()
            stackFrames.addAll(stackFrame.coroutineInfoData.stackTrace)

//            val lastRestoredFrame = stackFrame.coroutineInfoData.stackTrace.first()

            stackFrames.addAll(stackFrame.threadPreCoroutineFrames.mapIndexed { index, stackFrameProxyImpl ->
//                if (index == 0)
//                    PreCoroutineStackFrameItem(stackFrameProxyImpl, lastRestoredFrame) // get location and variables also from restored part
//                else
                    PreCoroutineStackFrameItem(stackFrameProxyImpl)
            })
            stackFrames.addAll(stackFrame.coroutineInfoData.creationStackTrace)
            return stackFrames
        }
        return null
    }

    fun lookupForResumeContinuation(
        frameProxy: StackFrameProxyImpl,
        suspendContext: SuspendContextImpl,
        framesLeft: List<StackFrameProxyImpl>
    ): CoroutinePreflightStackFrame? {
        val location = frameProxy.location()
        if (!location.isInKotlinSources())
            return null

        if (threadAndContextSupportsEvaluation(suspendContext, frameProxy))
            return ContinuationHolder.lookupContinuation(suspendContext, frameProxy, framesLeft)
        return null
    }

    private fun threadAndContextSupportsEvaluation(suspendContext: SuspendContextImpl, frameProxy: StackFrameProxyImpl?) =
        suspendContext.supportsEvaluation() && frameProxy?.threadProxy()?.supportsEvaluation() ?: false
}


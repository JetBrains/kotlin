/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.DebuggerManagerThreadImpl
import com.intellij.debugger.engine.JVMStackFrameInfoProvider
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.ui.impl.watch.MethodsTracker
import com.intellij.debugger.ui.impl.watch.StackFrameDescriptorImpl
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XValueChildrenList
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineStackFrameItem
import org.jetbrains.kotlin.idea.debugger.invokeInManagerThread
import org.jetbrains.kotlin.idea.debugger.stackFrame.KotlinStackFrame

/**
 * Coroutine exit frame represented by a stack frames
 * invokeSuspend():-1
 * resumeWith()
 *
 */

class CoroutinePreflightStackFrame(
    val invokeSuspendFrame: StackFrameProxyImpl,
    val resumeWithFrame: StackFrameProxyImpl,
    val restoredStackFrame: List<CoroutineStackFrameItem>,
    val stackFrameDescriptorImpl: StackFrameDescriptorImpl,
    val threadPreCoroutineFrames: List<StackFrameProxyImpl>
) : KotlinStackFrame(stackFrameDescriptorImpl), JVMStackFrameInfoProvider {

    override fun computeChildren(node: XCompositeNode) {
        val childrenList = XValueChildrenList()
        val firstRestoredCoroutineStackFrameItem = restoredStackFrame.firstOrNull() ?: return
        firstRestoredCoroutineStackFrameItem.spilledVariables.forEach {
            childrenList.add(it)
        }
        node.addChildren(childrenList, false)
        super.computeChildren(node)
    }

    override fun isInLibraryContent() =
        false

    override fun isSynthetic() =
        false

    companion object {
        fun preflight(
            invokeSuspendFrame: StackFrameProxyImpl,
            resumeWithFrame: StackFrameProxyImpl,
            restoredStackFrame: List<CoroutineStackFrameItem>,
            originalFrames: List<StackFrameProxyImpl>
        ): CoroutinePreflightStackFrame? {
            val topRestoredFrame = restoredStackFrame.firstOrNull() ?: return null
            val descriptor = StackFrameDescriptorImpl(
                LocationStackFrameProxyImpl(topRestoredFrame.location, invokeSuspendFrame), MethodsTracker()
            )
            return CoroutinePreflightStackFrame(invokeSuspendFrame, resumeWithFrame, restoredStackFrame, descriptor, originalFrames)
        }
    }
}
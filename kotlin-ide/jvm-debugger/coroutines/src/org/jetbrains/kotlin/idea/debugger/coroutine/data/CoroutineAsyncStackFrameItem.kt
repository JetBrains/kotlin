/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.data

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.debugger.ui.impl.watch.MethodsTracker
import com.intellij.debugger.ui.impl.watch.StackFrameDescriptorImpl
import com.intellij.ui.ColoredTextContainer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XNamedValue
import com.intellij.xdebugger.frame.XStackFrame
import com.sun.jdi.Location
import com.sun.jdi.ObjectReference
import org.jetbrains.kotlin.idea.debugger.*
import org.jetbrains.kotlin.idea.debugger.coroutine.coroutineDebuggerTraceEnabled
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger

class CreationCoroutineStackFrameItem(
    val stackTraceElement: StackTraceElement,
    location: Location
) : CoroutineStackFrameItem(location, emptyList()) {
    fun emptyDescriptor(frame: StackFrameProxyImpl) =
        EmptyStackFrameDescriptor(stackTraceElement, frame)
}

/**
 * Suspended frames in Suspended coroutine
 */
class SuspendCoroutineStackFrameItem(
    val stackTraceElement: StackTraceElement,
    location: Location,
    spilledVariables: List<XNamedValue> = emptyList()
) : CoroutineStackFrameItem(location, spilledVariables) {
    fun emptyDescriptor(frame: StackFrameProxyImpl) =
        EmptyStackFrameDescriptor(stackTraceElement, frame)
}

class RunningCoroutineStackFrameItem(
    val frame: StackFrameProxyImpl,
//    val stackFrame: XStackFrame,
    spilledVariables: List<XNamedValue> = emptyList()
) : CoroutineStackFrameItem(frame.location(), spilledVariables)

/**
 * Restored frame in Running coroutine, attaching to running thread
 */
class RestoredCoroutineStackFrameItem(
    val frame: StackFrameProxyImpl,
    location: Location,
    spilledVariables: List<XNamedValue>
) : CoroutineStackFrameItem(location, spilledVariables) {
    fun emptyDescriptor() =
        StackFrameDescriptorImpl(frame, MethodsTracker())
}

/**
 * Restored from memory dump
 */
class DefaultCoroutineStackFrameItem(location: Location, spilledVariables: List<XNamedValue>) :
    CoroutineStackFrameItem(location, spilledVariables) {

    fun emptyDescriptor(frame: StackFrameProxyImpl) =
        StackFrameDescriptorImpl(frame, MethodsTracker())
}

/**
 * Original frame appeared before resumeWith call.
 *
 * Sequence is the following
 *
 * - KotlinStackFrame
 * - invokeSuspend(KotlinStackFrame) -|
 *                                    | replaced with CoroutinePreflightStackFrame
 * - resumeWith(KotlinStackFrame) ----|
 * - PreCoroutineStackFrameItem part of CoroutinePreflightStackFrame
 *
 */
class PreCoroutineStackFrameItem(val frame: StackFrameProxyImpl, location: Location, variables: List<XNamedValue> = emptyList()) :
    CoroutineStackFrameItem(location, variables) {
    constructor(frame: StackFrameProxyImpl, variables: List<XNamedValue> = emptyList()) : this(frame, frame.location(), variables)

    constructor(frame: StackFrameProxyImpl, restoredCoroutineStackFrameItem: CoroutineStackFrameItem) : this(
        frame,
        restoredCoroutineStackFrameItem.location,
        restoredCoroutineStackFrameItem.spilledVariables
    )

    override fun createFrame(debugProcess: DebugProcessImpl): CapturedStackFrame {
        return PreCoroutineStackFrame(frame, debugProcess, this)
    }
}

/**
 * Can act as a joint frame, take variables form restored frame and information from the original one.
 */
class PreCoroutineStackFrame(val frame: StackFrameProxyImpl, val debugProcess: DebugProcessImpl, item: StackFrameItem) :
    CoroutineStackFrame(debugProcess, item) {
    override fun computeChildren(node: XCompositeNode) {
        debugProcess.invokeInManagerThread {
            debugProcess.getPositionManager().createStackFrame(frame, debugProcess, frame.location())
                ?.computeChildren(node) // hack but works
        }
        super.computeChildren(node)
    }
}

open class CoroutineStackFrame(debugProcess: DebugProcessImpl, val item: StackFrameItem) :
    StackFrameItem.CapturedStackFrame(debugProcess, item) {
    override fun customizePresentation(component: ColoredTextContainer) {
        if (coroutineDebuggerTraceEnabled())
            component.append("${item.javaClass.simpleName} / ${this.javaClass.simpleName}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        super.customizePresentation(component)
    }

    override fun getCaptionAboveOf() = "CoroutineExit"

    override fun hasSeparatorAbove(): Boolean =
        false
}


sealed class CoroutineStackFrameItem(val location: Location, val spilledVariables: List<XNamedValue>) :
    StackFrameItem(location, spilledVariables) {
    val log by logger

    fun uniqueId(): String {
        return location.safeSourceName() + ":" + location.safeMethod().toString() + ":" +
                location.safeLineNumber() + ":" + location.safeKotlinPreferredLineNumber()
    }

    override fun createFrame(debugProcess: DebugProcessImpl): CapturedStackFrame {
        return CoroutineStackFrame(debugProcess, this)
    }
}

class EmptyStackFrameDescriptor(val frame: StackTraceElement, proxy: StackFrameProxyImpl) :
    StackFrameDescriptorImpl(proxy, MethodsTracker())

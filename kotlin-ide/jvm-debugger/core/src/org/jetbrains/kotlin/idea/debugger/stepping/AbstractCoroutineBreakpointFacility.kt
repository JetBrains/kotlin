/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.DebuggerManagerEx
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.engine.events.SuspendContextCommandImpl
import com.intellij.debugger.settings.DebuggerSettings
import com.intellij.debugger.ui.breakpoints.Breakpoint
import com.sun.jdi.Location
import com.sun.jdi.Method
import com.sun.jdi.request.EventRequest

abstract class AbstractCoroutineBreakpointFacility {
    abstract fun installCoroutineResumedBreakpoint(context: SuspendContextImpl, location: Location, method: Method): Boolean

    protected fun Breakpoint<*>.setSuspendPolicy(context: SuspendContextImpl) {
        suspendPolicy = when (context.suspendPolicy) {
            EventRequest.SUSPEND_ALL -> DebuggerSettings.SUSPEND_ALL
            EventRequest.SUSPEND_EVENT_THREAD -> DebuggerSettings.SUSPEND_THREAD
            EventRequest.SUSPEND_NONE -> DebuggerSettings.SUSPEND_NONE
            else -> DebuggerSettings.SUSPEND_ALL
        }
    }

    protected fun Breakpoint<*>.stepOverSuspendSwitch(action: SuspendContextCommandImpl, debugProcess: DebugProcessImpl) {
        val suspendContext = action.suspendContext
        if (suspendContext != null) {
            DebuggerSteppingHelper.createStepOverCommandForSuspendSwitch(suspendContext).contextAction(suspendContext)
        }
        debugProcess.requestsManager.deleteRequest(this)
    }


    protected fun applyEmptyThreadFilter(debugProcess: DebugProcessImpl) {
        // TODO this is nasty. Find a way to apply an empty thread filter only to the newly created breakpoint
        val breakpointManager = DebuggerManagerEx.getInstanceEx(debugProcess.project).breakpointManager
        breakpointManager.applyThreadFilter(debugProcess, null)
    }
}
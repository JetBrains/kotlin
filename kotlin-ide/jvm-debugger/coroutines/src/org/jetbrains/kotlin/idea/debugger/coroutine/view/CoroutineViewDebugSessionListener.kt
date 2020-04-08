/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.view

import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebugSessionListener
import com.intellij.xdebugger.frame.XSuspendContext
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil
import org.jetbrains.kotlin.idea.debugger.coroutine.util.logger

class CoroutineViewDebugSessionListener(
    private val session: XDebugSession,
    private val xCoroutineView: XCoroutineView
) : XDebugSessionListener {
    val log by logger

    override fun sessionPaused() {
        val suspendContext = session.suspendContext ?: return requestClear()
        xCoroutineView.alarm.cancel()
        renew(suspendContext)
    }

    override fun sessionResumed() {
        xCoroutineView.saveState()
        val suspendContext = session.suspendContext ?: return requestClear()
        renew(suspendContext)
    }

    override fun sessionStopped() {
        val suspendContext = session.suspendContext ?: return requestClear()
        renew(suspendContext)
    }

    override fun stackFrameChanged() {
        xCoroutineView.saveState()
    }

    override fun beforeSessionResume() {
    }

    override fun settingsChanged() {
        val suspendContext = session.suspendContext ?: return requestClear()
        renew(suspendContext)
    }

    fun renew(suspendContext: XSuspendContext) {
        if (suspendContext is SuspendContextImpl) {
            DebuggerUIUtil.invokeLater {
                xCoroutineView.renewRoot(suspendContext)
            }
        }
    }

    private fun requestClear() {
        if (ApplicationManager.getApplication().isUnitTestMode) { // no delay in tests
            xCoroutineView.resetRoot()
        } else {
            xCoroutineView.alarm.cancelAndRequest()
        }
    }
}
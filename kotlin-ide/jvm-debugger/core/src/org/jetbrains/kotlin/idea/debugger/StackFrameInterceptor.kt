/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.xdebugger.frame.XStackFrame
import com.sun.jdi.Location

interface StackFrameInterceptor {
    fun createStackFrame(
        frame: StackFrameProxyImpl,
        debugProcess: DebugProcessImpl,
        location: Location
    ): XStackFrame?
}

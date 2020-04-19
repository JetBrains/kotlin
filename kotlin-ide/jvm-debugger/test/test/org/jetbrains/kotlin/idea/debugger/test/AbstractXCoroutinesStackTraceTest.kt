/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test

import com.intellij.debugger.engine.JavaStackFrame
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.xdebugger.frame.XStackFrame
import org.jetbrains.kotlin.idea.debugger.coroutine.PreflightProvider
import org.jetbrains.kotlin.idea.debugger.test.preference.DebuggerPreferences
import org.jetbrains.kotlin.idea.debugger.test.util.XDebuggerTestUtil

abstract class AbstractXCoroutinesStackTraceTest : KotlinDescriptorTestCaseWithStackFrames() {
    override fun doMultiFileTest(files: TestFiles, preferences: DebuggerPreferences) {
        printStackFrame(files, preferences)
    }
}

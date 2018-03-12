// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.kotlin.exec.sequence

import com.intellij.debugger.streams.kotlin.exec.KotlinTraceEvaluationTestCase
import com.intellij.debugger.streams.kotlin.lib.sequence.KotlinSequenceSupportProvider
import com.intellij.debugger.streams.lib.LibrarySupportProvider

abstract class OperationsTestCase(private val packageName: String) : KotlinTraceEvaluationTestCase() {
    override val appName: String = "sequence"
    override val librarySupport: LibrarySupportProvider = KotlinSequenceSupportProvider()

    protected fun doTestWithResult() = doTest(false, fullyQualifiedClassName())
    protected fun doTestWithoutResult() = doTest(true, fullyQualifiedClassName())

    private fun fullyQualifiedClassName() = "$packageName.${getTestName(false)}"
}
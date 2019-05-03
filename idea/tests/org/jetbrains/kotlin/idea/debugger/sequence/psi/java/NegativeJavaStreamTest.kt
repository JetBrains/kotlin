/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.debugger.sequence.psi.java

import com.intellij.debugger.streams.wrapper.StreamChainBuilder
import org.jetbrains.kotlin.idea.debugger.sequence.KotlinPsiChainBuilderTestCase
import org.jetbrains.kotlin.idea.debugger.sequence.lib.java.JavaStandardLibrarySupportProvider
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class NegativeJavaStreamTest : KotlinPsiChainBuilderTestCase.Negative("streams/negative") {
    override val kotlinChainBuilder: StreamChainBuilder = JavaStandardLibrarySupportProvider().chainBuilder

    fun testFakeStream() = doTest()

    fun testWithoutTerminalOperation() = doTest()

    fun testNoBreakpoint() = doTest()

    fun testBreakpointOnMethod() = doTest()
    fun testBreakpointOnIfCondition() = doTest()
    fun testBreakpointOnNewScope() = doTest()
    fun testBreakpointOnElseBranch() = doTest()

    fun testInLambda() = doTest()
    fun testInLambdaWithBody() = doTest()
    fun testInAnonymous() = doTest()

    fun testAfterStatement() = doTest()

    fun testInPreviousStatement() = doTest()
    fun testInNextStatement() = doTest()

    fun testIdea173415() = doTest()
}
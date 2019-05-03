/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.debugger.sequence.psi.java

import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class LocationPositiveChainTest : PositiveJavaStreamTest("location") {
    fun testAnonymousBody() = doTest()

    fun testAssignExpression() = doTest()

    fun testFirstParameterOfFunction() = doTest()
    fun testLambdaBody() = doTest()

    fun testParameterInAssignExpression() = doTest()
    fun testParameterInReturnExpression() = doTest()

    fun testReturnExpression() = doTest()

    fun testSecondParameterOfFunction() = doTest()

    fun testSingleExpression() = doTest()

    fun testBeforeStatement() = doTest()

    fun testBetweenChainCallsBeforeDot() = doTest()
    fun testBetweenChainCallsAfterDot() = doTest()

    fun testInEmptyParameterList() = doTest()

    fun testBetweenParametersBeforeComma() = doTest()
    fun testBetweenParametersAfterComma() = doTest()

    fun testInAnyLambda() = doTest()
    fun testInAnyAnonymous() = doTest()
    fun testInString() = doTest()
    fun testInVariableName() = doTest()
    fun testInMethodReference() = doTest()

    fun testAsMethodExpression() = doTest()
}
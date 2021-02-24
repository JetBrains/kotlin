/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.blockingCallsDetection.BlockingMethodInNonBlockingContextInspection
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class CoroutineNonBlockingContextDetectionTest : KotlinLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String = PluginTestCaseBase.getTestDataPathBase() + "/inspections/blockingCallsDetection"

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    override fun setUp() {
        super.setUp()
        myFixture.addClass("""package org.jetbrains.annotations; public @interface BlockingContext {}""")
        myFixture.enableInspections(BlockingMethodInNonBlockingContextInspection::class.java)
    }

    fun testSimpleCoroutineScope() {
        doTest("InsideCoroutine.kt")
    }

    fun testCoroutineContextCheck() {
        doTest("ContextCheck.kt")
    }

    fun testLambdaReceiverType() {
        doTest("LambdaReceiverTypeCheck.kt")
    }

    fun testNestedFunctionsInsideSuspendLambda() {
        doTest("NestedFunctionsInsideSuspendLambda.kt")
    }

    fun testDispatchersTypeDetection() {
        doTest("DispatchersTypeCheck.kt")
    }

    private fun doTest(fileName: String) {
        myFixture.configureByFile(fileName)
        myFixture.testHighlighting(true, false, false, fileName)
    }
}
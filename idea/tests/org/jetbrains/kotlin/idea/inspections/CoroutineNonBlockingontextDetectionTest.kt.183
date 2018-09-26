/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.blockingCallsDetection.BlockingMethodInNonBlockingContextInspection
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.configureCompilerOptions

class CoroutineNonBlockingontextDetectionTest: KotlinLightCodeInsightFixtureTestCase() {
    override fun getTestDataPath(): String
            = PluginTestCaseBase.getTestDataPathBase() + "/inspections/blockingCallsDetection"

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    override fun setUp() {
        super.setUp()
        myFixture.addClass("""package org.jetbrains.annotations; public @interface BlockingContext {}""")
        myFixture.enableInspections(BlockingMethodInNonBlockingContextInspection::class.java)
    }

    fun testSimpleCoroutineScope() {
        myFixture.configureByFile("InsideCoroutine.kt")
        myFixture.testHighlighting(true, false, false, "InsideCoroutine.kt")
    }

    fun testCoroutineContextCheck() {
        myFixture.configureByFiles("ContextCheck.kt")
        myFixture.testHighlighting(true, false, false, "ContextCheck.kt")
    }

    fun testLambdaReceiverType() {
        myFixture.configureByFile("LambdaReceiverTypeCheck.kt")
        myFixture.testHighlighting(true, false, false, "LambdaReceiverTypeCheck.kt")
    }

    fun testNestedFunctionsInsideSuspendLambda() {
        myFixture.configureByFile("NestedFunctionsInsideSuspendLambda.kt")
        myFixture.testHighlighting(true, false, false, "NestedFunctionsInsideSuspendLambda.kt")
    }
}
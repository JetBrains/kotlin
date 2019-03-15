/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiModificationTrackerImpl
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor

class HighlightingPerformanceTest : KotlinLightCodeInsightFixtureTestCase() {
    private val text = """
        fun test(a: String, b: ArrayList<java.util.Date>) {
            println(a + b)
        }
    """.trimIndent()

    fun testModificationCountInc() {
        myFixture.configureByText("temp.kt", text)
        highlight()

        val tracker = PsiManager.getInstance(myFixture.project).modificationTracker as PsiModificationTrackerImpl

        PlatformTestUtil.assertTiming("Update local cache", 100) { // actual timing is ~55
            tracker.modificationCount.inc()
            highlight()
        }
    }

    fun testOutOfBlockModificationCountInc() {
        myFixture.configureByText("temp.kt", text)
        highlight()

        val tracker = PsiManager.getInstance(myFixture.project).modificationTracker as PsiModificationTrackerImpl

        PlatformTestUtil.assertTiming("Update all trackers", 200) { // actual timing is ~165
            tracker.incOutOfCodeBlockModificationCounter()
            highlight()
        }
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE_FULL_JDK

    private fun highlight() {
        myFixture.checkHighlighting(true, false, true)
    }
}
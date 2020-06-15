/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.findUsages

import com.intellij.find.findUsages.CustomUsageSearcher
import com.intellij.find.findUsages.FindUsagesOptions
import com.intellij.psi.PsiElement
import com.intellij.usageView.UsageInfo
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.maskExtensions
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCaseBase
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith

@RunWith(JUnit38ClassRunner::class)
class CustomUsageSearcherTest : KotlinLightCodeInsightFixtureTestCaseBase() {

    fun testAddCustomUsagesForKotlin() {
        val customUsageSearcher = object : CustomUsageSearcher() {
            override fun processElementUsages(element: PsiElement, processor: ProcessorInCompat<Usage>, options: FindUsagesOptions) {
                runReadAction { processor.process(UsageInfo2UsageAdapter(UsageInfo(element))) }
            }
        }
        maskExtensions(CustomUsageSearcher.EP_NAME, listOf(customUsageSearcher), testRootDisposable)
        myFixture.configureByText(KotlinFileType.INSTANCE, """val <caret>selfUsed = 1""")

        val usages = myFixture.getUsageViewTreeTextRepresentation(myFixture.elementAtCaret)
        assertTrue(usages.contains("val selfUsed"))
    }
}
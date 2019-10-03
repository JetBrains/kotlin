/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.NameSuggestionProvider
import com.intellij.refactoring.rename.PreferrableNameSuggestionProvider
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.ProjectDescriptorWithStdlibSources
import org.jetbrains.kotlin.test.InTextDirectivesUtils

abstract class AbstractNameSuggestionProviderTest : KotlinLightCodeInsightFixtureTestCase() {
    private fun getSuggestNames(element: PsiElement): List<String> {
        val names = HashSet<String>()
        for (provider in NameSuggestionProvider.EP_NAME.extensions) {
            val info = provider.getSuggestedNames(element, null, names)
            if (info != null) {
                if (provider is PreferrableNameSuggestionProvider && !provider.shouldCheckOthers()) break
            }
        }
        return names.sorted()
    }

    protected fun doTest(path: String) {
        val file = myFixture.configureByFile(fileName())
        val targetElement = TargetElementUtil.findTargetElement(
                myFixture.editor,
                TargetElementUtil.ELEMENT_NAME_ACCEPTED or TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED
        )!!
        val expectedNames = InTextDirectivesUtils.findListWithPrefixes(file.text, "// SUGGESTED_NAMES: ")
        val actualNames = getSuggestNames(targetElement)
        TestCase.assertEquals(expectedNames, actualNames)
    }

    override fun getProjectDescriptor() = ProjectDescriptorWithStdlibSources.INSTANCE
}
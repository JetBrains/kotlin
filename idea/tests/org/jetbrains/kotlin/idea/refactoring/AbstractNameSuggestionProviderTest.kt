/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        val file = myFixture.configureByFile(path)
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
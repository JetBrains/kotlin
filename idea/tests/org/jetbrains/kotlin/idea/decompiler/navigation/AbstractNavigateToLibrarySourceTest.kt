/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiElement
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.JdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.KotlinCodeInsightTestCase
import org.jetbrains.kotlin.idea.PluginTestCaseBase
import org.jetbrains.kotlin.idea.navigation.NavigationTestUtils
import org.jetbrains.kotlin.idea.references.JetReference
import org.jetbrains.kotlin.idea.testUtils.ModuleKind
import org.jetbrains.kotlin.idea.testUtils.configureAs
import org.jetbrains.kotlin.test.JetTestUtils
import java.io.File
import java.util.LinkedHashMap

public abstract class AbstractNavigateToLibrarySourceTest : KotlinCodeInsightTestCase() {

    protected fun doTest(path: String): Unit = doTestEx(path)

    protected fun doWithJSModuleTest(path: String): Unit = doTestEx(path) {
        val jsModule = this.createModule("js-module")
        jsModule.configureAs(ModuleKind.KOTLIN_JAVASCRIPT)
    }

    protected fun doTestEx(path: String, additionalConfig: (() -> Unit)? = null) {
        configureByFile(path)
        getModule().configureAs(getProjectDescriptor())

        if (additionalConfig != null) {
            additionalConfig()
        }

        checkAnnotatedLibraryCode(false)
        checkAnnotatedLibraryCode(true)
    }

    override fun tearDown() {
        JetSourceNavigationHelper.setForceResolve(false)
        super.tearDown()
    }

    override fun getTestDataPath(): String =
            JetTestUtils.getHomeDirectory() + File.separator

    private fun checkAnnotatedLibraryCode(forceResolve: Boolean) {
        JetSourceNavigationHelper.setForceResolve(forceResolve)
        val actualCode = NavigationTestUtils.getNavigateElementsText(getProject(), collectInterestingNavigationElements())
        val expectedCode = getExpectedAnnotatedLibraryCode()
        UsefulTestCase.assertSameLines(expectedCode, actualCode)
    }

    private fun collectInterestingReferences(): Collection<JetReference> {
        val psiFile = getFile()
        val referenceContainersToReferences = LinkedHashMap<PsiElement, JetReference>()
        for (offset in 0..psiFile.getTextLength() - 1) {
            val ref = psiFile.findReferenceAt(offset)
            if (ref is JetReference && !referenceContainersToReferences.containsKey(ref.getElement())) {
                val target = ref.resolve()
                if (target == null) continue

                val targetNavPsiFile = target.getNavigationElement().getContainingFile()
                if (targetNavPsiFile == null) continue

                val targetNavFile = targetNavPsiFile.getVirtualFile()
                if (targetNavFile == null) continue

                if (ProjectFileIndex.SERVICE.getInstance(getProject()).isInLibrarySource(targetNavFile)) {
                    referenceContainersToReferences.put(ref.getElement(), ref)
                }
            }
        }
        return referenceContainersToReferences.values()
    }

    private fun collectInterestingNavigationElements() =
            collectInterestingReferences().map {
                val target = it.resolve()
                TestCase.assertNotNull(target)
                target!!.getNavigationElement()
            }

    private fun getExpectedAnnotatedLibraryCode(): String {
        val document = getDocument(getFile())
        TestCase.assertNotNull(document)
        return JetTestUtils.getLastCommentedLines(document)
    }

    private fun getProjectDescriptor(): LightProjectDescriptor =
            JdkAndMockLibraryProjectDescriptor(PluginTestCaseBase.getTestDataPathBase() + "/decompiler/navigation/library", true)
}
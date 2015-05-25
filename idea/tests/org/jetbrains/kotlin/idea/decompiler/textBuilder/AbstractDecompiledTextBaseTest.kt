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

package org.jetbrains.kotlin.idea.decompiler.textBuilder

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.kotlin.idea.test.JdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.JetLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import kotlin.test.fail

public abstract class AbstractDecompiledTextBaseTest(private val isJsLibrary: Boolean = false) : JetLightCodeInsightFixtureTestCase() {
    protected val TEST_DATA_PATH: String = PluginTestCaseBase.getTestDataPathBase() + "/decompiler/decompiledText"

    protected val TEST_PACKAGE: String = "test"

    protected abstract fun getFileToDecompile(): VirtualFile

    protected abstract fun checkPsiFile(psiFile: PsiFile)

    public fun doTest(path: String) {
        val fileToDecompile = getFileToDecompile()
        val psiFile = PsiManager.getInstance(getProject()).findFile(fileToDecompile)!!
        checkPsiFile(psiFile)
        UsefulTestCase.assertSameLinesWithFile(path.substring(0, path.length() - 1) + ".expected.kt", psiFile.getText())
        checkThatFileWasParsedCorrectly(psiFile)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        if (isAllFilesPresentInTest()) {
            return JetLightProjectDescriptor.INSTANCE
        }
        return JdkAndMockLibraryProjectDescriptor(TEST_DATA_PATH + "/" + getTestName(false), false, isJsLibrary)
    }

    private fun checkThatFileWasParsedCorrectly(clsFile: PsiFile) {
        clsFile.accept(object : PsiRecursiveElementVisitor() {
            override fun visitErrorElement(element: PsiErrorElement) {
                fail("Decompiled file should not contain error elements!\n${element.getElementTextWithContext()}")
            }
        })
    }
}
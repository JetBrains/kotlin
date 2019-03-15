/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.textBuilder

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

abstract class AbstractDecompiledTextBaseTest(
        baseDirectory: String,
        private val isJsLibrary: Boolean = false,
        private val allowKotlinPackage: Boolean = false,
        private val withRuntime: Boolean = false
) : KotlinLightCodeInsightFixtureTestCase() {
    protected val TEST_DATA_PATH: String = PluginTestCaseBase.getTestDataPathBase() + baseDirectory

    protected val TEST_PACKAGE: String = "test"

    protected abstract fun getFileToDecompile(): VirtualFile

    protected abstract fun checkPsiFile(psiFile: PsiFile)

    protected abstract fun textToCheck(psiFile: PsiFile): String

    protected open fun checkStubConsistency(file: VirtualFile, decompiledText: String) {}

    fun doTest(path: String) {
        val fileToDecompile = getFileToDecompile()
        val psiFile = PsiManager.getInstance(project).findFile(fileToDecompile)!!
        checkPsiFile(psiFile)

        val checkedText = textToCheck(psiFile)

        KotlinTestUtils.assertEqualsToFile(File(path.substring(0, path.length - 1) + ".expected.kt"), checkedText)

        checkStubConsistency(fileToDecompile, checkedText)

        checkThatFileWasParsedCorrectly(psiFile)
    }

    override fun getProjectDescriptor(): LightProjectDescriptor {
        if (isAllFilesPresentInTest()) {
            return KotlinLightProjectDescriptor.INSTANCE
        }
        return SdkAndMockLibraryProjectDescriptor(
            TEST_DATA_PATH + "/" + getTestName(false),
            false,
            withRuntime,
            isJsLibrary,
            allowKotlinPackage
        )
    }

    private fun checkThatFileWasParsedCorrectly(clsFile: PsiFile) {
        clsFile.accept(object : PsiRecursiveElementVisitor() {
            override fun visitErrorElement(element: PsiErrorElement) {
                fail("Decompiled file should not contain error elements!\n${element.getElementTextWithContext()}")
            }
        })
    }
}

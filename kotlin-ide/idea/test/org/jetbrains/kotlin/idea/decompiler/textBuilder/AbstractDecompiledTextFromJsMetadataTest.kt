/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.textBuilder

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFile
import org.jetbrains.kotlin.idea.test.MockLibraryFacility
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportList
import org.jetbrains.kotlin.psi.KtPackageDirective
import org.jetbrains.kotlin.serialization.js.JsSerializerProtocol
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File
import kotlin.test.assertTrue

private const val CHECK_PACKAGE_DIRECTIVE = "CHECK_PACKAGE"

abstract class AbstractDecompiledTextFromJsMetadataTest(baseDirectory: String) :
    AbstractDecompiledTextBaseTest(baseDirectory, isJsLibrary = true, withRuntime = true) {
    override fun getFileToDecompile(): VirtualFile = getKjsmFile(module)

    override fun fileName(): String {
        val testName = getTestName(false)
        return "$testName/$testName.kt"
    }

    override fun checkPsiFile(psiFile: PsiFile) {
        assertTrue(psiFile is KtDecompiledFile, "Expecting decompiled kotlin javascript file, was: " + psiFile::class.java)
    }

    override fun textToCheck(psiFile: PsiFile): String {
        if (psiFile !is KtFile) {
            return psiFile.text
        }

        val singleClass = findSingleClassToCheck(psiFile) ?: return psiFile.text

        // Take top-comments and spaces after them, package directive with space after it, and single class element
        return psiFile.children.filter { child ->
            when (child) {
                is PsiComment -> true
                is KtPackageDirective -> true
                singleClass -> true
                is PsiWhiteSpace -> {
                    child.prevSibling is KtPackageDirective || child.prevSibling is KtImportList || child.prevSibling is PsiComment
                }
                else -> false
            }
        }.joinToString(separator = "") { it.text }
    }

    private fun findSingleClassToCheck(psiFile: PsiFile): PsiElement? {
        val singleClassName = getTestName(false)
        val singleClass = psiFile.children.find { child -> child is KtClass && child.name == singleClassName } ?: return null

        val mainFilePath = "$mockSourcesBase/$singleClassName/$singleClassName.kt"
        val mainFile = File(mainFilePath)
        if (mainFile.exists() && InTextDirectivesUtils.isDirectiveDefined(File(mainFilePath).readText(), CHECK_PACKAGE_DIRECTIVE)) {
            return null
        }

        return singleClass
    }

    private fun getKjsmFile(module: Module): VirtualFile {
        val root = findTestLibraryRoot(module) ?: error("Test library not found for module ${module.name}")
        root.refresh(false, true)

        val kjsmRoot = File(MockLibraryFacility.MOCK_LIBRARY_NAME, TEST_PACKAGE.replace('.', '/'))
        val kjsmFile = File(kjsmRoot, JsSerializerProtocol.getKjsmFilePath(FqName(TEST_PACKAGE)))

        return root.findFileByRelativePath(kjsmFile.path) ?: error("KJSM file not found in JS library ${root.name}")
    }
}

abstract class AbstractCommonDecompiledTextFromJsMetadataTest : AbstractDecompiledTextFromJsMetadataTest("/decompiler/decompiledText")

abstract class AbstractJsDecompiledTextFromJsMetadataTest : AbstractDecompiledTextFromJsMetadataTest("/decompiler/decompiledTextJs")
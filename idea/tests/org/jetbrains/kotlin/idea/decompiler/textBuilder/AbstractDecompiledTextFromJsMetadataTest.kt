/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.textBuilder

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFile
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor
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
    AbstractDecompiledTextBaseTest(baseDirectory, true, withRuntime = true) {
    override fun getFileToDecompile(): VirtualFile = getKjsmFile(TEST_PACKAGE, myModule!!)

    override fun checkPsiFile(psiFile: PsiFile) =
        assertTrue(psiFile is KtDecompiledFile, "Expecting decompiled kotlin javascript file, was: " + psiFile::class.java)

    override fun textToCheck(psiFile: PsiFile): String {
        if (psiFile !is KtFile) return psiFile.text
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

        val mainFilePath = "$TEST_DATA_PATH/$singleClassName/$singleClassName.kt"
        val mainFile = File(mainFilePath)
        if (mainFile.exists() && InTextDirectivesUtils.isDirectiveDefined(File(mainFilePath).readText(), CHECK_PACKAGE_DIRECTIVE)) {
            return null
        }

        return singleClass
    }
}

abstract class AbstractCommonDecompiledTextFromJsMetadataTest : AbstractDecompiledTextFromJsMetadataTest("/decompiler/decompiledText")

abstract class AbstractJsDecompiledTextFromJsMetadataTest : AbstractDecompiledTextFromJsMetadataTest("/decompiler/decompiledTextJs")

fun getKjsmFile(packageName: String, module: Module): VirtualFile {
    val root = findTestLibraryRoot(module)!!
    root.refresh(false, true)
    val packageDir = root.findFileByRelativePath(SdkAndMockLibraryProjectDescriptor.LIBRARY_NAME + "/" + packageName.replace(".", "/"))!!
    return packageDir.findChild(JsSerializerProtocol.getKjsmFilePath(FqName(packageName)).substringAfterLast('/'))!!
}

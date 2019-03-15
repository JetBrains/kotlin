/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.textBuilder

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.idea.decompiler.classFile.KotlinClsStubBuilder
import org.jetbrains.kotlin.idea.decompiler.classFile.KtClsFile
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.serializeToString
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.stubs.elements.KtFileStubBuilder
import org.junit.Assert
import kotlin.test.assertTrue

abstract class AbstractDecompiledTextTest(baseDirectory: String, allowKotlinPackage: Boolean)
    : AbstractDecompiledTextBaseTest(baseDirectory, allowKotlinPackage = allowKotlinPackage) {
    override fun getFileToDecompile(): VirtualFile = getClassFile(TEST_PACKAGE, getTestName(false), myModule!!)

    override fun checkStubConsistency(file: VirtualFile, decompiledText: String) {
        val fileWithDecompiledText = KtPsiFactory(project).createFile(decompiledText)
        val stubTreeFromDecompiledText = KtFileStubBuilder().buildStubTree(fileWithDecompiledText)
        val expectedText = stubTreeFromDecompiledText.serializeToString()

        val fileStub = KotlinClsStubBuilder().buildFileStub(FileContentImpl.createByFile(file))!!
        Assert.assertEquals(expectedText, fileStub.serializeToString())
    }

    override fun checkPsiFile(psiFile: PsiFile) =
            assertTrue(psiFile is KtClsFile, "Expecting decompiled kotlin file, was: " + psiFile::class.java)

    override fun textToCheck(psiFile: PsiFile) = psiFile.text
}

abstract class AbstractCommonDecompiledTextTest : AbstractDecompiledTextTest("/decompiler/decompiledText", true)

abstract class AbstractJvmDecompiledTextTest : AbstractDecompiledTextTest("/decompiler/decompiledTextJvm", false)

fun findTestLibraryRoot(module: Module): VirtualFile? {
    for (orderEntry in ModuleRootManager.getInstance(module).orderEntries) {
        if (orderEntry is LibraryOrderEntry) {
            return orderEntry.getFiles(OrderRootType.CLASSES)[0]
        }
    }
    return null
}

fun getClassFile(
        packageName: String,
        className: String,
        module: Module
): VirtualFile {
    val root = findTestLibraryRoot(module)!!
    val packageDir = root.findFileByRelativePath(packageName.replace(".", "/"))!!
    return packageDir.findChild(className + ".class")!!
}
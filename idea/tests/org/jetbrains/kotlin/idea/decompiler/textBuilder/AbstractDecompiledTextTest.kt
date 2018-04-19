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
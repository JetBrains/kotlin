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

package org.jetbrains.kotlin.idea.decompiler.stubBuilder

import com.google.common.io.Files
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.idea.decompiler.classFile.KotlinClsStubBuilder
import org.jetbrains.kotlin.idea.stubs.AbstractStubBuilderTest
import org.jetbrains.kotlin.psi.stubs.elements.KtFileStubBuilder
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.utils.addIfNotNull
import org.junit.Assert
import java.io.File
import java.util.*

abstract class AbstractClsStubBuilderTest : LightCodeInsightFixtureTestCase() {
    fun doTest(sourcePath: String) {
        val classFile = getClassFileToDecompile(sourcePath)
        val txtFilePath = File("$sourcePath/${lastSegment(sourcePath)}.txt")
        testClsStubsForFile(classFile, txtFilePath)
    }

    protected fun testClsStubsForFile(classFile: VirtualFile, txtFile: File?) {
        val stubTreeFromCls = KotlinClsStubBuilder().buildFileStub(FileContentImpl.createByFile(classFile))!!
        myFixture.configureFromExistingVirtualFile(classFile)
        val psiFile = myFixture.file
        val stubTreeFromDecompiledText = KtFileStubBuilder().buildStubTree(psiFile)
        val expectedText = stubTreeFromDecompiledText.serializeToString()
        Assert.assertEquals(expectedText, stubTreeFromCls.serializeToString())
        if (txtFile != null) {
            KotlinTestUtils.assertEqualsToFile(txtFile, expectedText)
        }
    }

    private fun getClassFileToDecompile(sourcePath: String): VirtualFile {
        val outDir = KotlinTestUtils.tmpDir("libForStubTest-" + sourcePath)
        MockLibraryUtil.compileKotlin(sourcePath, outDir, extraOptions = listOf("-Xallow-kotlin-package"))
        val root = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(outDir)!!
        return root.findClassFileByName(lastSegment(sourcePath))
    }

    private fun lastSegment(sourcePath: String): String {
        return Files.getNameWithoutExtension(sourcePath.split('/').last { !it.isEmpty() })!!
    }
}

fun StubElement<out PsiElement>.serializeToString(): String {
    return AbstractStubBuilderTest.serializeStubToString(this)
}

fun VirtualFile.findClassFileByName(className: String): VirtualFile {
    val files = LinkedHashSet<VirtualFile>()
    VfsUtilCore.iterateChildrenRecursively(
            this,
            { virtualFile -> virtualFile.isDirectory || virtualFile.name == "$className.class" },
            { virtualFile -> if (!virtualFile.isDirectory) files.addIfNotNull(virtualFile); true })

    return files.single()
}

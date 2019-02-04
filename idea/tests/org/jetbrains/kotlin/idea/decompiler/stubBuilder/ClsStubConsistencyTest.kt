/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.stubBuilder

import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.idea.decompiler.classFile.KotlinClsStubBuilder
import org.jetbrains.kotlin.idea.decompiler.classFile.buildDecompiledTextForClassFile
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.stubs.elements.KtFileStubBuilder
import org.junit.Assert

class ClsStubConsistencyTest : KotlinLightCodeInsightFixtureTestCase() {
    private fun doTest(id: ClassId) {
        val packageFile = VirtualFileFinder.SERVICE.getInstance(project).findVirtualFileWithHeader(id)
                ?: throw AssertionError("File not found for id: $id")
        val decompiledText = buildDecompiledTextForClassFile(packageFile).text
        val fileWithDecompiledText = KtPsiFactory(project).createFile(decompiledText)
        val stubTreeFromDecompiledText = KtFileStubBuilder().buildStubTree(fileWithDecompiledText)
        val expectedText = stubTreeFromDecompiledText.serializeToString()

        val fileStub = KotlinClsStubBuilder().buildFileStub(FileContentImpl.createByFile(packageFile))!!
        Assert.assertEquals(expectedText, fileStub.serializeToString())
    }

    override fun getProjectDescriptor(): LightProjectDescriptor = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    fun testConsistency() {
        doTest(ClassId.topLevel(FqName("kotlin.collections.CollectionsKt")))
    }
}

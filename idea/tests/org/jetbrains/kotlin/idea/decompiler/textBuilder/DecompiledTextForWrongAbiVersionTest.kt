/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.textBuilder

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.decompiler.AbstractInternalCompiledClassesTest
import org.jetbrains.kotlin.idea.decompiler.classFile.KtClsFile
import org.jetbrains.kotlin.idea.decompiler.common.INCOMPATIBLE_ABI_VERSION_GENERAL_COMMENT
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.findClassFileByName
import org.jetbrains.kotlin.idea.test.KotlinJdkAndLibraryProjectDescriptor
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import java.io.File

class DecompiledTextForWrongAbiVersionTest : AbstractInternalCompiledClassesTest() {

    override fun getProjectDescriptor(): LightProjectDescriptor {
        return KotlinJdkAndLibraryProjectDescriptor(File(KotlinTestUtils.getTestDataPathBase() + "/cli/jvm/wrongAbiVersionLib/bin"))
    }

    fun testSyntheticClassIsInvisibleWrongAbiVersion() = doTestNoPsiFilesAreBuiltForSyntheticClasses()

    fun testClassWithWrongAbiVersion() = doTest("ClassWithWrongAbiVersion")

    fun testPackagePartWithWrongAbiVersion() = doTest("Wrong_packageKt")

    fun doTest(name: String) {
        val root = findTestLibraryRoot(myModule!!)!!
        checkFileWithWrongAbiVersion(root.findClassFileByName(name))
    }

    private fun checkFileWithWrongAbiVersion(file: VirtualFile) {
        val psiFile = PsiManager.getInstance(project).findFile(file)
        Assert.assertTrue(psiFile is KtClsFile)
        val decompiledText = psiFile!!.text!!
        Assert.assertTrue(decompiledText.contains(INCOMPATIBLE_ABI_VERSION_GENERAL_COMMENT))
    }
}

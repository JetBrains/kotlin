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

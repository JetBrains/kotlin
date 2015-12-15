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

import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.idea.decompiler.builtIns.KotlinBuiltInClassFileType
import org.jetbrains.kotlin.idea.decompiler.builtIns.KotlinBuiltInPackageFileType
import org.jetbrains.kotlin.idea.decompiler.builtIns.KotlinBuiltInStubBuilder
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.stubs.elements.KtFileStubBuilder
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import java.io.File

class BuiltInDecompilerTest : LightCodeInsightFixtureTestCase() {

    fun testAny() {
        doTest("Any.${KotlinBuiltInClassFileType.defaultExtension}", "Any")
    }

    fun testInt() {
        doTest("Int.${KotlinBuiltInClassFileType.defaultExtension}", "Int")
    }

    fun testKotlinPackage() {
        doTest("kotlin.${KotlinBuiltInPackageFileType.defaultExtension}", "kotlin_package")
    }

    private fun doTest(fileName: String, testDataName: String) {
        val kotlinDirInRuntime = findDir("kotlin", project)
        val anyKotlinClass = kotlinDirInRuntime.findChild(fileName)!!
        val stubTreeFromDecompiler = KotlinBuiltInStubBuilder().buildFileStub(FileContentImpl.createByFile(anyKotlinClass))!!
        myFixture.configureFromExistingVirtualFile(anyKotlinClass)
        val psiFile = myFixture.file
        KotlinTestUtils.assertEqualsToFile(File(testDataPath + "$testDataName.text"), psiFile.text)

        val stubTreeFromDecompiledText = KtFileStubBuilder().buildStubTree(psiFile)
        val expectedText = stubTreeFromDecompiledText.serializeToString()
        Assert.assertEquals(expectedText, stubTreeFromDecompiler.serializeToString())
        KotlinTestUtils.assertEqualsToFile(File(testDataPath + "$testDataName.stubs"), expectedText)
    }

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE

    companion object {
        private val testDataPath = PluginTestCaseBase.getTestDataPathBase() + "/decompiler/builtIns/"
    }
}

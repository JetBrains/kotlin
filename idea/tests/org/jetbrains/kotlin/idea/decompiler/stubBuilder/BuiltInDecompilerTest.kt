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

import com.intellij.psi.stubs.PsiFileStub
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.idea.decompiler.builtIns.KotlinBuiltInDecompiler
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.stubs.elements.KtFileStubBuilder
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.Assert
import java.io.File

abstract class AbstractBuiltInDecompilerTest : LightCodeInsightFixtureTestCase() {
    protected fun doTest(packageFqName: String): String {
        val stubTreeFromDecompiler = configureAndBuildFileStub(packageFqName)
        val stubTreeFromDecompiledText = KtFileStubBuilder().buildStubTree(myFixture.file)
        val expectedText = stubTreeFromDecompiledText.serializeToString()
        Assert.assertEquals("Stub mismatch for package $packageFqName", expectedText, stubTreeFromDecompiler.serializeToString())
        return expectedText
    }

    abstract fun configureAndBuildFileStub(packageFqName: String): PsiFileStub<*>

    override fun getProjectDescriptor() = KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}

class BuiltInDecompilerTest : AbstractBuiltInDecompilerTest() {
    override fun configureAndBuildFileStub(packageFqName: String): PsiFileStub<*> {
        val dirInRuntime = findDir(packageFqName, project)
        val kotlinBuiltInsVirtualFile = dirInRuntime.children.single { it.extension == BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION }
        myFixture.configureFromExistingVirtualFile(kotlinBuiltInsVirtualFile)
        return KotlinBuiltInDecompiler().stubBuilder.buildFileStub(FileContentImpl.createByFile(kotlinBuiltInsVirtualFile))!!
    }

    fun testBuiltInStubTreeEqualToStubTreeFromDecompiledText() {
        doTest("kotlin")
        doTest("kotlin.collections")
    }
}

class BuiltInDecompilerForWrongAbiVersionTest : AbstractBuiltInDecompilerTest() {
    override fun getTestDataPath() = PluginTestCaseBase.TEST_DATA_DIR + "/decompiler/builtins/"

    override fun configureAndBuildFileStub(packageFqName: String): PsiFileStub<*> {
        myFixture.configureByFile(testDataPath + BuiltInSerializerProtocol.getBuiltInsFilePath(FqName(packageFqName)))
        return KotlinBuiltInDecompiler().stubBuilder.buildFileStub(FileContentImpl.createByFile(myFixture.file.virtualFile))!!
    }

    fun testStubTreesEqualForIncompatibleAbiVersion() {
        val serializedStub = doTest("test")
        KotlinTestUtils.assertEqualsToFile(
                File(testDataPath + "test.text"),
                myFixture.file.text.replace(BuiltInsBinaryVersion.INSTANCE.toString(), "\$VERSION\$")
        )
        KotlinTestUtils.assertEqualsToFile(File(testDataPath + "test.stubs"), serializedStub)
    }
}

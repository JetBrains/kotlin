/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.stubBuilder

import com.intellij.psi.stubs.PsiFileStub
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.idea.decompiler.builtIns.KotlinBuiltInDecompiler
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.stubs.elements.KtFileStubBuilder
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
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
        myFixture.configureByFile(BuiltInSerializerProtocol.getBuiltInsFilePath(FqName(packageFqName)))
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

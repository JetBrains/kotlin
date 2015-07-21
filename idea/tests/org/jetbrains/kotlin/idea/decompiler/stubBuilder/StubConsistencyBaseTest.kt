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

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.compiled.ClsStubBuilder
import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinder
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.stubs.elements.JetFileStubBuilder
import org.junit.Assert

public abstract class StubConsistencyBaseTest : JetLightCodeInsightFixtureTestCase() {

    protected abstract fun getPackages(): List<FqName>
    protected abstract fun getVirtualFileFinder(): VirtualFileFinder

    protected abstract fun createStubBuilder(): ClsStubBuilder

    protected abstract fun getDecompiledText(packageFile: VirtualFile): String

    public fun testConsistency() {
        getPackages().forEach { doTest(it) }
    }

    private fun doTest(packageFqName: FqName) {
        val project = getProject()
        val packageFile = getVirtualFileFinder().findVirtualFileWithHeader(PackageClassUtils.getPackageClassId(packageFqName))!!
        val decompiledText = getDecompiledText(packageFile)
        val fileWithDecompiledText = JetPsiFactory(project).createFile(decompiledText)
        val stubTreeFromDecompiledText = JetFileStubBuilder().buildStubTree(fileWithDecompiledText)
        val expectedText = stubTreeFromDecompiledText.serializeToString()

        val fileStub = createStubBuilder().buildFileStub(FileContentImpl.createByFile(packageFile))!!
        Assert.assertEquals(expectedText, fileStub.serializeToString())
    }
}
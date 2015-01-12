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

import org.jetbrains.kotlin.idea.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.load.kotlin.VirtualFileFinderFactory
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.decompiler.textBuilder.buildDecompiledText
import org.jetbrains.kotlin.idea.JetWithJdkAndRuntimeLightProjectDescriptor
import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.stubs.elements.JetFileStubBuilder
import org.junit.Assert

public class ClsStubConsistencyTest : JetLightCodeInsightFixtureTestCase() {

    private val STANDARD_LIBRARY_FQNAME = FqName("kotlin")

    public fun testConsistencyForKotlinPackage() {
        val project = getProject()
        val packageClassFqName = PackageClassUtils.getPackageClassFqName(STANDARD_LIBRARY_FQNAME)
        val virtualFileFinder = VirtualFileFinderFactory.SERVICE.getInstance(project).create(GlobalSearchScope.allScope(project))
        val kotlinPackageFile = virtualFileFinder.findVirtualFileWithHeader(packageClassFqName)!!

        val decompiledText = buildDecompiledText(kotlinPackageFile).text
        val clsStub = KotlinClsStubBuilder().buildFileStub(FileContentImpl.createByFile(kotlinPackageFile))!!

        val fileWithDecompiledText = JetPsiFactory(project).createFile(decompiledText)
        val stubTreeFromDecompiledText = JetFileStubBuilder().buildStubTree(fileWithDecompiledText)

        val expectedText = stubTreeFromDecompiledText.serializeToString()
        Assert.assertEquals(expectedText, clsStub.serializeToString())
    }

    override fun getProjectDescriptor() = JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
}

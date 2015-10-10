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

package org.jetbrains.kotlin.idea.decompiler

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.decompiler.navigation.NavigateToDecompiledLibraryTest
import org.jetbrains.kotlin.idea.test.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.junit.Assert

public abstract class AbstractInternalCompiledClassesTest : JetLightCodeInsightFixtureTestCase() {
    private fun isFileWithHeader(predicate: (KotlinClassHeader) -> Boolean) : VirtualFile.() -> Boolean = {
        val header = KotlinBinaryClassCache.getKotlinBinaryClass(this)?.getClassHeader()
        header != null && predicate(header)
    }

    protected fun isSyntheticClass(): VirtualFile.() -> Boolean =
            isFileWithHeader { it.kind == KotlinClassHeader.Kind.SYNTHETIC_CLASS }

    protected fun doTestNoPsiFilesAreBuiltForLocalClass(): Unit =
            doTestNoPsiFilesAreBuiltFor("local", isFileWithHeader { it.isLocalClass })

    protected fun doTestNoPsiFilesAreBuiltForSyntheticClasses(): Unit =
            doTestNoPsiFilesAreBuiltFor("synthetic", isSyntheticClass())

    protected fun doTestNoPsiFilesAreBuiltFor(fileKind: String, acceptFile: VirtualFile.() -> Boolean) {
        val project = getProject()
        doTest(fileKind, acceptFile) {
            val psiFile = PsiManager.getInstance(project).findFile(this)
            Assert.assertNull("PSI files for $fileKind classes should not be build, is was build for: ${this.getPresentableName()}",
                              psiFile)
        }
    }

    protected fun doTest(fileKind: String, acceptFile: VirtualFile.() -> Boolean, performTest: VirtualFile.() -> Unit) {
        val root = NavigateToDecompiledLibraryTest.findTestLibraryRoot(myModule!!)!!
        var foundAtLeastOneFile = false
        root.checkRecursively {
            if (acceptFile()) {
                foundAtLeastOneFile = true
                performTest()
            }
        }
        Assert.assertTrue("Should find at least one file of kind ($fileKind). This assertion can fail in following scenarios:\n" +
                          "1. Test data is bad and doesn't cover this case.\n2. ABI has changed and test no longer checks anything.",
                          foundAtLeastOneFile)
    }

    protected fun VirtualFile.checkRecursively(body: VirtualFile.() -> Unit) {
        if (!isDirectory()) {
            body()
        }
        else {
            for (file in getChildren()!!) {
                file.checkRecursively(body)
            }
        }
    }
}


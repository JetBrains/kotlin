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

package org.jetbrains.kotlin.idea.decompiler

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.idea.caches.IDEKotlinBinaryClassCache
import org.jetbrains.kotlin.idea.decompiler.textBuilder.findTestLibraryRoot
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassId
import org.junit.Assert

abstract class AbstractInternalCompiledClassesTest : KotlinLightCodeInsightFixtureTestCase() {
    private fun isFileWithHeader(predicate: (KotlinClassHeader, ClassId) -> Boolean) : VirtualFile.() -> Boolean = {
        val info = IDEKotlinBinaryClassCache.getKotlinBinaryClassHeaderData(this)
        info != null && predicate(info.classHeader, info.classId)
    }

    protected fun isSyntheticClass(): VirtualFile.() -> Boolean =
            isFileWithHeader { header, _ -> header.kind == KotlinClassHeader.Kind.SYNTHETIC_CLASS }

    protected fun doTestNoPsiFilesAreBuiltForLocalClass(): Unit =
            doTestNoPsiFilesAreBuiltFor("local", isFileWithHeader { _, classId -> classId.isLocal })

    protected fun doTestNoPsiFilesAreBuiltForSyntheticClasses(): Unit =
            doTestNoPsiFilesAreBuiltFor("synthetic", isSyntheticClass())

    protected fun doTestNoPsiFilesAreBuiltFor(fileKind: String, acceptFile: VirtualFile.() -> Boolean) {
        val project = project
        doTest(fileKind, acceptFile) {
            val psiFile = PsiManager.getInstance(project).findFile(this)
            Assert.assertNull("PSI files for $fileKind classes should not be build, is was build for: ${this.presentableName}",
                              psiFile)
        }
    }

    protected fun doTest(fileKind: String, acceptFile: VirtualFile.() -> Boolean, performTest: VirtualFile.() -> Unit) {
        val root = findTestLibraryRoot(myModule!!)!!
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
        if (!isDirectory) {
            body()
        }
        else {
            for (file in children!!) {
                file.checkRecursively(body)
            }
        }
    }
}


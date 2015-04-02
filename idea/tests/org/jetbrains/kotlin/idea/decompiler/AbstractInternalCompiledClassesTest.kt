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
import com.intellij.psi.PsiCompiledFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.compiled.ClsFileImpl
import org.jetbrains.kotlin.idea.JetLightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.idea.decompiler.navigation.NavigateToDecompiledLibraryTest
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinClass
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinSyntheticClass
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinSyntheticClass.Kind.TRAIT_IMPL
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.junit.Assert

public abstract class AbstractInternalCompiledClassesTest : JetLightCodeInsightFixtureTestCase() {
    private fun isFileWithHeader(predicate: (KotlinClassHeader) -> Boolean) : VirtualFile.() -> Boolean = {
        val header = KotlinBinaryClassCache.getKotlinBinaryClass(this)?.getClassHeader()
        header != null && predicate(header)
    }

    private fun isSyntheticClassOfKind(kind: KotlinSyntheticClass.Kind) : VirtualFile.() -> Boolean =
            isFileWithHeader { it.syntheticClassKind == kind }

    private fun isClassOfKind(kind: KotlinClass.Kind) : VirtualFile.() -> Boolean =
            isFileWithHeader { it.classKind == kind }

    protected fun doTestTraitImplClassIsVisibleAsJavaClass() {
        val project = getProject()
        doTest("trait impl", isSyntheticClassOfKind(TRAIT_IMPL)) {
            val psiFile = PsiManager.getInstance(project).findFile(this)!!
            Assert.assertTrue("Should not be kotlin file",
                              psiFile !is JetClsFile)
            Assert.assertTrue("Should be java file, was ${psiFile.javaClass.getSimpleName()}",
                              psiFile is ClsFileImpl)

            val decompiledPsiFile = (psiFile as PsiCompiledFile).getDecompiledPsiFile()!!
            Assert.assertTrue("Should be java decompiled file, was ${decompiledPsiFile.javaClass.getSimpleName()}",
                              decompiledPsiFile is PsiJavaFile)
            val classes = (decompiledPsiFile as PsiJavaFile).getClasses()
            Assert.assertTrue("Should have some decompiled text",
                              classes.size() == 1 && classes[0].getName()!!.endsWith(JvmAbi.TRAIT_IMPL_SUFFIX))
        }
    }

    protected fun doTestNoPsiFilesAreBuiltForLocalClass(kind: KotlinClass.Kind): Unit =
            doTestNoPsiFilesAreBuiltFor(kind.toString(), isClassOfKind(kind))

    protected fun doTestNoPsiFilesAreBuiltForSyntheticClass(kind: KotlinSyntheticClass.Kind): Unit =
            doTestNoPsiFilesAreBuiltFor(kind.toString(), isSyntheticClassOfKind(kind))

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


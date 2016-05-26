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

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFile
import org.jetbrains.kotlin.idea.test.ModuleKind
import org.jetbrains.kotlin.idea.test.configureAs
import kotlin.test.assertTrue

abstract class AbstractDecompiledTextFromJsMetadataTest(baseDirectory: String) : AbstractDecompiledTextBaseTest(baseDirectory, true) {
    override fun getFileToDecompile(): VirtualFile = getKjsmFile(TEST_PACKAGE, getTestName(false), myModule!!)

    override fun checkPsiFile(psiFile: PsiFile) =
            assertTrue(psiFile is KtDecompiledFile, "Expecting decompiled kotlin javascript file, was: " + psiFile.javaClass)

    override fun setUp() {
        super.setUp()
        myModule!!.configureAs(ModuleKind.KOTLIN_JAVASCRIPT)
    }
}

abstract class AbstractCommonDecompiledTextFromJsMetadataTest : AbstractDecompiledTextFromJsMetadataTest("/decompiler/decompiledText")

abstract class AbstractJsDecompiledTextFromJsMetadataTest : AbstractDecompiledTextFromJsMetadataTest("/decompiler/decompiledTextJs")

fun getKjsmFile(
        packageName: String,
        className: String,
        module: Module
): VirtualFile {
    val root = findTestLibraryRoot(module)!!
    root.refresh(false, true)
    val packageDir = root.findFileByRelativePath(root.nameWithoutExtension + "/" + packageName.replace(".", "/"))!!
    return packageDir.findChild(className + ".kjsm")!!
}
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

package org.jetbrains.kotlin.idea.decompiler.textBuilder

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.decompiler.KtClsFile
import org.jetbrains.kotlin.idea.decompiler.navigation.NavigateToDecompiledLibraryTest
import kotlin.test.assertTrue

public abstract class AbstractDecompiledTextTest(baseDirectory: String) : AbstractDecompiledTextBaseTest(baseDirectory) {
    protected override fun getFileToDecompile(): VirtualFile =
        NavigateToDecompiledLibraryTest.getClassFile(TEST_PACKAGE, getTestName(false), myModule!!)

    protected override fun checkPsiFile(psiFile: PsiFile) =
            assertTrue(psiFile is KtClsFile, "Expecting decompiled kotlin file, was: " + psiFile.javaClass)
}

public abstract class AbstractCommonDecompiledTextTest : AbstractDecompiledTextTest("/decompiler/decompiledText")

public abstract class AbstractJvmDecompiledTextTest : AbstractDecompiledTextTest("/decompiler/decompiledTextJvm")

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.findUsages

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.doTestWithFIRFlagsByPath
import org.jetbrains.kotlin.idea.test.KotlinJdkAndMultiplatformStdlibDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.test.SdkAndMockLibraryProjectDescriptor

abstract class AbstractKotlinFindUsagesWithStdlibFirTest : AbstractKotlinFindUsagesWithStdlibTest() {
    override fun isFirPlugin(): Boolean = true

    override fun <T : PsiElement> doTest(path: String) = doTestWithFIRFlagsByPath(path) {
        super.doTest<T>(path)
    }
}

abstract class AbstractKotlinFindUsagesWithStdlibTest : AbstractFindUsagesTest() {
    override fun getProjectDescriptor() =
        KotlinJdkAndMultiplatformStdlibDescriptor.JDK_AND_MULTIPLATFORM_STDLIB_WITH_SOURCES
}

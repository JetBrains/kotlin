/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import java.io.File

abstract class AbstractKotlinSourceInJavaCompletionTest : KotlinFixtureCompletionBaseTestCase() {
    override fun getPlatform() = JvmPlatforms.unspecifiedJvmPlatform

    override fun doTest(testPath: String) {
        val mockLibDir = File(COMPLETION_TEST_DATA_BASE, "injava/mockLib")

        val files = mockLibDir.walk().filter { it.isFile }
        for (file in files) {
            val localPath = file.toRelativeString(File(testDataPath))
            val vFile = myFixture.copyFileToProject(localPath)
            myFixture.configureFromExistingVirtualFile(vFile)
        }

        LightClassComputationControl.testWithControl(project, FileUtil.loadFile(File(testPath))) {
            super.doTest(testPath)
        }
    }

    override fun getProjectDescriptor() = LightCodeInsightFixtureTestCase.JAVA_LATEST
    override fun defaultCompletionType() = CompletionType.BASIC
}
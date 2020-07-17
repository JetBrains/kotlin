/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.copy

import org.jetbrains.kotlin.idea.refactoring.rename.loadTestConfiguration
import org.jetbrains.kotlin.idea.test.KotlinMultiFileTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase.IDEA_TEST_DATA_DIR
import java.io.File

abstract class AbstractMultiModuleCopyTest : KotlinMultiFileTestCase() {
    override fun getTestRoot(): String = "/refactoring/copyMultiModule/"

    override fun getTestDataDirectory() = IDEA_TEST_DATA_DIR

    fun doTest(path: String) {
        val config = loadTestConfiguration(File(path))

        isMultiModule = true

        doTestCommittingDocuments { rootDir, _ ->
            AbstractCopyTest.runCopyRefactoring(path, config, rootDir, project)
        }
    }
}
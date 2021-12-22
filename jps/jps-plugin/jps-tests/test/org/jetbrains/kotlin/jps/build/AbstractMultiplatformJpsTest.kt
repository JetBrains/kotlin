/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.kotlin.jps.build.dependeciestxt.ModulesTxt
import org.jetbrains.kotlin.jps.build.dependeciestxt.MppJpsIncTestsGenerator
import java.io.File

abstract class AbstractMultiplatformJpsTestWithGeneratedContent : AbstractIncrementalJpsTest() {
    override val modulesTxtFile: File
        get() = File(testDataDir.parent, "dependencies.txt").also {
            check(it.exists()) {
                "`dependencies.txt` should be in parent dir. " +
                        "See `jps-plugin/testData/incremental/multiModule/multiplatform/withGeneratedContent/README.md` for details"
            }
        }

    override val testDataSrc: File
        get() = File(workDir, "generatedTestDataSources")

    override fun generateModuleSources(modulesTxt: ModulesTxt) {
        testDataSrc.mkdirs()

        val testCaseName = testDataDir.name

        val generator = MppJpsIncTestsGenerator(modulesTxt) { testDataSrc }
        val testCase = generator.testCases.find { it.name == testCaseName }
            ?: error("Test case `$testCaseName` is not configured in ${modulesTxt.fileName}")
        testCase.generate()
    }

    override val ModulesTxt.Module.sourceFilePrefix: String
        get() = "${indexedName}_"
}
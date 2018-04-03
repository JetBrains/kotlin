/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.build

import org.jetbrains.kotlin.jps.build.dependeciestxt.DependenciesTxt
import org.jetbrains.kotlin.jps.build.dependeciestxt.MppJpsIncTestsGenerator
import java.io.File

abstract class AbstractMultiplatformJpsTest : AbstractIncrementalJpsTest() {
    override val dependenciesTxtFile: File
        get() = File(testDataDir.parent, "dependencies.txt").also {
            check(it.exists()) {
                "`dependencies.txt` should be in parent dir. " +
                        "See jps-plugin/testData/incremental/multiplatform/multiModule/README.md for details"
            }
        }

    override val testDataSrc: File
        get() = File(workDir, "generatedTestDataSources")

    override fun generateModuleSources(dependenciesTxt: DependenciesTxt) {
        testDataSrc.mkdirs()

        val testCaseName = testDataDir.name

        val generator = MppJpsIncTestsGenerator(dependenciesTxt) { testDataSrc }
        val testCase = generator.testCases.find { it.name == testCaseName } ?: error("Unsupported test case name: $testCaseName")
        testCase.generate()
    }

    override fun prepareModuleSources(module: DependenciesTxt.Module?) {
        prepareIndexedModuleSources(module!!)
    }
}
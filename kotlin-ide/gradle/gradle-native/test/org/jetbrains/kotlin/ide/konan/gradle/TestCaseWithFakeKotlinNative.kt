/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan.gradle

import org.jetbrains.kotlin.idea.codeInsight.gradle.KotlinGradleImportingTestCase
import java.io.File

abstract class TestCaseWithFakeKotlinNative : KotlinGradleImportingTestCase() {
    protected fun configureProject() {
        configureByFiles()

        // include data dir with fake Kotlin/Native libraries
        val testSuiteDataDir = testDataDirectory().parentFile
        val kotlinNativeHome = testSuiteDataDir.resolve(FAKE_KOTLIN_NATIVE_HOME_RELATIVE_PATH)

        kotlinNativeHome.walkTopDown()
            .filter { it.isFile }
            .forEach { pathInTestSuite ->
                // need to put copied file one directory upper than the project root, so adding ".." to the beginning of relative path
                // reason: distribution KLIBs should not be appear in IDEA indexes, so they should be located outside of the project root
                val relativePathInProject = DOUBLE_DOT_PATH.resolve(pathInTestSuite.relativeTo(testSuiteDataDir))
                createProjectSubFile(relativePathInProject.toString(), pathInTestSuite.readText())
            }
    }
}

internal val FAKE_KOTLIN_NATIVE_HOME_RELATIVE_PATH = File("kotlin-native-data-dir", "kotlin-native-PLATFORM-VERSION")
internal val DOUBLE_DOT_PATH = File("..")

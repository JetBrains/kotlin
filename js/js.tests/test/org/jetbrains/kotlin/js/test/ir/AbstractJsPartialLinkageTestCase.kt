/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.klib.KlibCompilerChangeScenario
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils

abstract class AbstractJsPartialLinkageNoICTestCase : AbstractJsPartialLinkageTestCase(CompilerType.NO_IC)
abstract class AbstractJsPartialLinkageNoICES6TestCase : AbstractJsPartialLinkageTestCase(CompilerType.NO_IC_WITH_ES6)
abstract class AbstractJsPartialLinkageWithICTestCase : AbstractJsPartialLinkageTestCase(CompilerType.WITH_IC)

abstract class AbstractJsPartialLinkageTestCase(compilerType: CompilerType) : AbstractJsCompilerInvocationTest(compilerType) {
    // The entry point to generated test classes.
    fun runTest(@TestDataFile testPath: String) {
        val configuration = JsTestConfiguration(
            testPath = testPath,
            buildDir = buildDir,
            compilerType = compilerType,
        )
        val artifactBuilder = JsCompilerInvocationTestArtifactBuilder(configuration)

        KlibCompilerInvocationTestUtils.runTest(
            testConfiguration = configuration,
            artifactBuilder = artifactBuilder,
            compilerEditionChange = KlibCompilerChangeScenario.NoChange,
        )
    }
}

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.klib.CodegenBoxTestStructureExtractor
import org.jetbrains.kotlin.klib.KlibCompilerChangeScenario
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils
import java.io.File

abstract class AbstractJsOldArtifactNewCompilerTest : AbstractJsCompilerInvocationTest(CompilerType.NO_IC) {
    // The entry point to generated test classes.
    fun runTest(@TestDataFile testPath: String) {
        val configuration = JsCompilerInvocationTestConfiguration(
            buildDir = buildDir,
            compilerType = compilerType,
        )

        KlibCompilerInvocationTestUtils.runTest(
            testStructure = CodegenBoxTestStructureExtractor().extractTestStructure(File(testPath).absoluteFile),
            testConfiguration = configuration,
            artifactBuilder = JsCompilerInvocationTestArtifactBuilder(configuration),
            binaryRunner = JsCompilerInvocationTestBinaryRunner,
            compilerEditionChange = KlibCompilerChangeScenario.CustomCompilerForKlibs,
        )
    }
}

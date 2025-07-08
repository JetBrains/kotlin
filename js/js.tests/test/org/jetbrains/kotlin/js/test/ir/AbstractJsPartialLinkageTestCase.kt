/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.klib.KlibCompilerChangeScenario
import org.jetbrains.kotlin.klib.KlibCompilerEdition
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils.Dependencies
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils.ModuleBuildDirs
import java.io.File

abstract class AbstractJsPartialLinkageNoICTestCase : AbstractJsPartialLinkageTestCase(CompilerType.NO_IC)
abstract class AbstractJsPartialLinkageNoICES6TestCase : AbstractJsPartialLinkageTestCase(CompilerType.NO_IC_WITH_ES6)
abstract class AbstractJsPartialLinkageWithICTestCase : AbstractJsPartialLinkageTestCase(CompilerType.WITH_IC)

abstract class AbstractJsPartialLinkageTestCase(compilerType: CompilerType) : AbstractJsKlibLinkageTestCase(compilerType) {
    // The entry point to generated test classes.
    fun runTest(@TestDataFile testPath: String) = KlibCompilerInvocationTestUtils.runTest(
        testConfiguration = JsTestConfiguration(testPath),
        compilerEditionChange = KlibCompilerChangeScenario.NoChange,
    )

    override fun buildKlib(
        moduleName: String,
        buildDirs: ModuleBuildDirs,
        dependencies: Dependencies,
        klibFile: File,
        compilerEdition: KlibCompilerEdition,
        compilerArguments: List<String>,
    ) {
        require(compilerEdition == KlibCompilerEdition.CURRENT) { "Partial Linkage tests accept only Current compiler" }

        val kotlinSourceFilePaths = composeSourceFile(buildDirs)

        // Build KLIB:
        runCompilerViaCLI(
            listOf(
                K2JSCompilerArguments::irProduceKlibFile.cliArgument,
                K2JSCompilerArguments::outputDir.cliArgument, klibFile.parentFile.absolutePath,
                K2JSCompilerArguments::moduleName.cliArgument, moduleName,
                // Halt on any unexpected warning.
                K2JSCompilerArguments::allWarningsAsErrors.cliArgument,
                // Tests suppress the INVISIBLE_REFERENCE check.
                // However, JS doesn't produce the INVISIBLE_REFERENCE error;
                // As result, it triggers a suppression error warning about the redundant suppression.
                // This flag is used to disable the warning.
                K2JSCompilerArguments::dontWarnOnErrorSuppression.cliArgument
            ),
            dependencies.toCompilerArgs(),
            compilerArguments,
            kotlinSourceFilePaths
        )
    }
}

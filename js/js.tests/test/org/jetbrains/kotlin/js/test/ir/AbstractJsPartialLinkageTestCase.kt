/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.ir

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.klib.KlibCompilerEdition
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.Dependencies
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.ModuleBuildDirs
import java.io.File

abstract class AbstractJsPartialLinkageNoICTestCase : AbstractJsPartialLinkageTestCase(CompilerType.K1_NO_IC)
abstract class AbstractJsPartialLinkageNoICES6TestCase : AbstractJsPartialLinkageTestCase(CompilerType.K1_NO_IC_WITH_ES6)
abstract class AbstractJsPartialLinkageWithICTestCase : AbstractJsPartialLinkageTestCase(CompilerType.K1_WITH_IC)
abstract class AbstractFirJsPartialLinkageNoICTestCase : AbstractJsPartialLinkageTestCase(CompilerType.K2_NO_IC)

abstract class AbstractJsPartialLinkageTestCase(compilerType: CompilerType) : AbstractJsKlibLinkageTestCase(compilerType) {
    // The entry point to generated test classes.
    fun runTest(@TestDataFile testPath: String) = PartialLinkageTestUtils.runTest(JsTestConfiguration(testPath))

    override fun buildKlib(
        moduleName: String,
        buildDirs: ModuleBuildDirs,
        dependencies: Dependencies,
        klibFile: File,
        compilerEdition: KlibCompilerEdition,
    ) {
        require(compilerEdition == KlibCompilerEdition.CURRENT) { "Partial Linkage tests accept only Current compiler" }

        val kotlinSourceFilePaths = composeSourceFile(buildDirs)

        // Build KLIB:
        runCompilerViaCLI(
            listOf(
                "-Xir-produce-klib-file",
                "-ir-output-dir", klibFile.parentFile.absolutePath,
                "-ir-output-name", moduleName,
                // Halt on any unexpected warning.
                "-Werror",
                // Tests suppress the INVISIBLE_REFERENCE check.
                // However, JS doesn't produce the INVISIBLE_REFERENCE error;
                // As result, it triggers a suppression error warning about the redundant suppression.
                // This flag is used to disable the warning.
                "-Xdont-warn-on-error-suppression"
            ),
            dependencies.toCompilerArgs(),
            listOf(
                "-language-version", "2.0",
                // Don't fail on language version warnings.
                "-Xsuppress-version-warnings"
            ).takeIf { compilerType.useFir },
            kotlinSourceFilePaths
        )
    }
}

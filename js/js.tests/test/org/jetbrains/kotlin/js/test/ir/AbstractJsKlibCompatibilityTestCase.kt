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
import org.jetbrains.kotlin.klib.KlibCompilerEdition.CURRENT
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.Dependencies
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.Dependency
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.ModuleBuildDirs
import java.io.File

abstract class AbstractJsKlibCompatibilityNoICTestCase : AbstractJsKlibCompatibilityTestCase(CompilerType.K1_NO_IC)
abstract class AbstractJsKlibCompatibilityNoICES6TestCase : AbstractJsKlibCompatibilityTestCase(CompilerType.K1_NO_IC_WITH_ES6)
abstract class AbstractJsKlibCompatibilityWithICTestCase : AbstractJsKlibCompatibilityTestCase(CompilerType.K1_WITH_IC)

abstract class AbstractJsKlibCompatibilityTestCase(compilerType: CompilerType) : AbstractJsKlibLinkageTestCase(compilerType) {
    // The entry point to generated test classes.
    fun runTest(@TestDataFile testPath: String) {
        KlibCompilerChangeScenario.entries.forEach { scenario ->
            PartialLinkageTestUtils.runTest(JsTestConfiguration(testPath), scenario)
        }
    }

    override fun buildKlib(
        moduleName: String,
        buildDirs: ModuleBuildDirs,
        dependencies: Dependencies,
        klibFile: File,
        compilerEdition: KlibCompilerEdition,
    ) {
        val kotlinSourceFilePaths = composeSourceFile(buildDirs)

        runCompilerViaCLI(
            listOf(
                K2JSCompilerArguments::irProduceKlibFile.cliArgument,
                K2JSCompilerArguments::outputDir.cliArgument, klibFile.parentFile.absolutePath,
                K2JSCompilerArguments::moduleName.cliArgument, moduleName,
            ),
            dependencies.replaceStdlib(compilerEdition).toCompilerArgs(),
            kotlinSourceFilePaths,
            compilerEdition = compilerEdition
        )
    }
}

private fun Dependencies.replaceStdlib(compilerEdition: KlibCompilerEdition): Dependencies {
    if (compilerEdition == CURRENT) return this
    return Dependencies(
        regularDependencies = regularDependencies.replaceStdLib(),
        friendDependencies = friendDependencies
    )
}

private fun Set<Dependency>.replaceStdLib() = map {
    if (it.moduleName == "stdlib") Dependency("stdlib", JsKlibTestSettings.releasedArtifactHome.jsStdLib)
    else it
}.toSet()
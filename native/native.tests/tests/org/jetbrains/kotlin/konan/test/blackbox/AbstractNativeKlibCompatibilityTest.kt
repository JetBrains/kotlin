/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.klib.KlibCompilerChangeScenario
import org.jetbrains.kotlin.klib.KlibCompilerEdition
import org.jetbrains.kotlin.klib.KlibCompilerEdition.*
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.Dependencies
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.LibraryCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.UsePartialLinkage
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.ReleasedCompiler
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.withCustomCompiler
import org.junit.jupiter.api.Tag
import java.io.File

/**
 * Testing area: klibs binary compatibility in compiler variance (compiler version or compiler modes).
 * The usual scenario looks like
 *
 *       intermediate -> bottom
 *           \           /
 *               main
 *
 * Where we build intermediate module with bottom(V1), after we rebuild bottom with V2 and
 * build and run main against intermediate and bottom(V2).
 */
@Tag("klib")
@UsePartialLinkage(UsePartialLinkage.Mode.ENABLED_WITH_ERROR)
abstract class AbstractNativeKlibCompatibilityTest : AbstractKlibLinkageTest() {

    // The entry point to generated test classes.
    protected fun runTest(@TestDataFile testPath: String) =
        KlibCompilerChangeScenario.entries.forEach {
            try {
                PartialLinkageTestUtils.runTest(NativeTestConfiguration(testPath), it)
            } catch (e: Throwable) {
                println("Failure during the test for scenario $it. Error message: ${e.message}")
                throw e
            }
        }

    override fun buildKlib(
        moduleName: String,
        moduleSourceDir: File,
        dependencies: Dependencies,
        klibFile: File,
        compilerEdition: KlibCompilerEdition,
    ) {
        val klibArtifact = KLIB(klibFile)

        val compilerArgs = if (compilerEdition.args.isNotEmpty()) {
            TestCompilerArgs(COMPILER_ARGS.compilerArgs + compilerEdition.args)
        } else COMPILER_ARGS

        val testCase = createTestCase(moduleName, moduleSourceDir, compilerArgs)

        val compilation = when (compilerEdition) {
            LATEST_RELEASE -> releasedCompilation(testCase, klibArtifact, dependencies)
            CURRENT -> currentCompilation(testCase, klibArtifact, dependencies)
        }

        compilation.result.assertSuccess() // <-- trigger compilation
        producedKlibs += ProducedKlib(moduleName, klibArtifact, dependencies) // Remember the artifact with its dependencies.
    }

    private fun currentCompilation(
        testCase: TestCase,
        klibArtifact: KLIB,
        dependencies: Dependencies,
    ) = LibraryCompilation(
        settings = testRunSettings,
        freeCompilerArgs = testCase.freeCompilerArgs,
        sourceModules = testCase.modules,
        dependencies = createLibraryDependencies(dependencies),
        expectedArtifact = klibArtifact
    )

    private fun releasedCompilation(
        testCase: TestCase,
        klibArtifact: KLIB,
        dependencies: Dependencies,
    ): LibraryCompilation {
        val releasedCompiler = testRunSettings.get<ReleasedCompiler>()

        val customDeps = Dependencies(
            dependencies.regularDependencies.replaceStdlibWithBundled(releasedCompiler.nativeHome),
            dependencies.friendDependencies
        )

        return LibraryCompilation(
            settings = testRunSettings.withCustomCompiler(releasedCompiler),
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            dependencies = createLibraryDependencies(customDeps),
            expectedArtifact = klibArtifact
        )
    }
}

private fun Set<PartialLinkageTestUtils.Dependency>.replaceStdlibWithBundled(nativeHome: KotlinNativeHome) = map {
    if (it.moduleName == "stdlib")
        PartialLinkageTestUtils.Dependency("stdlib", nativeHome.stdlibFile)
    else it
}.toSet()
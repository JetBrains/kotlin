/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.*
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.*
import java.io.File

abstract class AbstractNativeInteropIndexerBaseTest : AbstractNativeSimpleTest() {
    private val buildDir: File get() = testRunSettings.get<SimpleTestDirectories>().testBuildDir

    internal fun TestCase.cinteropToLibrary(vararg dependencies: TestCompilationDependency<*>): TestCompilationResult.Success<out KLIB> {
        val compilation = CInteropCompilation(
            settings = testRunSettings,
            freeCompilerArgs = freeCompilerArgs,
            sourceModules = modules,
            dependencies = dependencies.toList(),
            expectedArtifact = toLibraryArtifact()
        )
        return compilation.result.assertSuccess()
    }

    private fun TestCase.toLibraryArtifact() = KLIB(buildDir.resolve(modules.first().name + ".klib"))

    internal fun generateCInteropTestCaseWithSingleDef(defFile: File, extraArgs: List<String>): TestCase {
        val moduleName: String = defFile.name
        val module = TestModule.Exclusive(moduleName, emptySet(), emptySet())
        module.files += TestFile.createCommitted(defFile, module)

        return TestCase(
            id = TestCaseId.Named(moduleName),
            kind = TestKind.STANDALONE,
            modules = setOf(module),
            freeCompilerArgs = TestCompilerArgs(extraArgs),
            nominalPackageName = PackageName.EMPTY,
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
            extras = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT)
        ).apply {
            initialize(null)
        }
    }
}

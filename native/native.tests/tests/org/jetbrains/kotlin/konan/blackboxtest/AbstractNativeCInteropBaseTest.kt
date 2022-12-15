/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.*
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.*
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.*
import java.io.File

abstract class AbstractNativeCInteropBaseTest : AbstractNativeSimpleTest() {
    private val buildDir: File get() = testRunSettings.get<SimpleTestDirectories>().testBuildDir
    internal val targets: KotlinNativeTargets get() = testRunSettings.get<KotlinNativeTargets>()
    internal val kotlinNativeClassLoader: KotlinNativeClassLoader get() = testRunSettings.get<KotlinNativeClassLoader>()

    internal fun TestCase.cinteropToLibrary(): TestCompilationResult<out KLIB> {
        modules.singleOrNull()
        val compilation = CInteropCompilation(
            classLoader = testRunSettings.get(),
            targets = targets,
            freeCompilerArgs = freeCompilerArgs,
            defFile = modules.singleOrNull()!!.files.singleOrNull()!!.location,
            expectedArtifact = toLibraryArtifact()
        )
        return compilation.result
    }

    private fun TestCase.toLibraryArtifact() = KLIB(buildDir.resolve(modules.singleOrNull()!!.name + ".klib"))

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
            initialize(null, null)
        }
    }
}

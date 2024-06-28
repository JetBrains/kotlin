/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExecutableCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationDependency
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts

@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
abstract class AbstractNativeLinkerOutputTest : AbstractNativeCInteropBaseTest() {
    private fun createTestCaseNoTestRun(module: TestModule.Exclusive, compilerArgs: TestCompilerArgs) = TestCase(
        id = TestCaseId.Named(module.name),
        kind = TestKind.STANDALONE_NO_TR,
        modules = setOf(module),
        freeCompilerArgs = compilerArgs,
        nominalPackageName = PackageName.EMPTY,
        checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
        extras = TestCase.NoTestRunnerExtras(".${module.name}")
    ).apply {
        initialize(null, null)
    }

    internal fun compileToExecutable(
        module: TestModule.Exclusive,
        dependencies: List<TestCompilationDependency<*>>,
        args: List<String> = emptyList()
    ) = compileToExecutable(
        createTestCaseNoTestRun(module, TestCompilerArgs(args)),
        dependencies
    )

    internal fun compileToExecutable(
        testCase: TestCase,
        dependencies: List<TestCompilationDependency<*>>
    ): TestCompilationResult<out TestCompilationArtifact.Executable> {
        val compilation = ExecutableCompilation(
            settings = testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            extras = TestCase.NoTestRunnerExtras(".${testCase.modules.singleOrNull()!!.name}"),
            dependencies = dependencies,
            expectedArtifact = getExecutableArtifact()
        )
        return compilation.result
    }

    private fun getExecutableArtifact() =
        TestCompilationArtifact.Executable(buildDir.resolve("app." + testRunSettings.get<KotlinNativeTargets>().testTarget.family.exeSuffix))
}
/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.konan.blackboxtest.support.PackageName
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCaseId
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.blackboxtest.support.TestDirectives
import org.jetbrains.kotlin.konan.blackboxtest.support.TestFile
import org.jetbrains.kotlin.konan.blackboxtest.support.TestKind
import org.jetbrains.kotlin.konan.blackboxtest.support.TestModule
import org.jetbrains.kotlin.konan.blackboxtest.support.TestRunnerType
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.ExecutableCompilation
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.ExistingDependency
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.LibraryCompilation
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationDependency
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationDependencyType
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.PipelineType
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.SimpleTestDirectories
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Timeouts
import org.jetbrains.kotlin.konan.blackboxtest.support.util.LAUNCHER_MODULE_NAME
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.junit.jupiter.api.Assumptions
import java.io.File

private val DEFAULT_EXTRAS = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT)

internal val AbstractNativeSimpleTest.buildDir: File get() = testRunSettings.get<SimpleTestDirectories>().testBuildDir

internal fun TestCompilationArtifact.KLIB.asLibraryDependency() =
    ExistingDependency(this, TestCompilationDependencyType.Library)

internal fun TestCompilationArtifact.KLIB.asIncludedLibraryDependency() =
    ExistingDependency(this, TestCompilationDependencyType.IncludedLibrary)

internal fun AbstractNativeSimpleTest.compileToLibrary(sourcesDir: File, vararg dependencies: TestCompilationArtifact.KLIB) =
    compileToLibrary(sourcesDir, buildDir, *dependencies)

internal fun AbstractNativeSimpleTest.compileToLibrary(testCase: TestCase, vararg dependencies: TestCompilationDependency<*>) =
    compileToLibrary(testCase, buildDir, dependencies.asList())

internal fun AbstractNativeSimpleTest.compileToLibrary(
    sourcesDir: File,
    outputDir: File,
    vararg dependencies: TestCompilationArtifact.KLIB
): TestCompilationArtifact.KLIB {
    val testCase: TestCase = generateTestCaseWithSingleModule(sourcesDir)
    val compilationResult = compileToLibrary(testCase, outputDir, dependencies.map { it.asLibraryDependency() })
    return compilationResult.resultingArtifact
}

internal fun AbstractNativeSimpleTest.compileToExecutable(
    sourcesDir: File,
    freeCompilerArgs: TestCompilerArgs,
    vararg dependencies: TestCompilationArtifact.KLIB
): TestCompilationResult<out TestCompilationArtifact.Executable> {
    val testCase: TestCase = generateTestCaseWithSingleModule(sourcesDir, freeCompilerArgs)
    return compileToExecutable(testCase, dependencies.map { it.asLibraryDependency() })
}

internal fun AbstractNativeSimpleTest.compileToExecutable(testCase: TestCase, vararg dependencies: TestCompilationDependency<*>) =
    compileToExecutable(testCase, dependencies.asList())

internal fun AbstractNativeSimpleTest.generateTestCaseWithSingleModule(
    moduleDir: File?,
    freeCompilerArgs: TestCompilerArgs = TestCompilerArgs.EMPTY
): TestCase {
    val moduleName: String = moduleDir?.name ?: LAUNCHER_MODULE_NAME
    val module = TestModule.Exclusive(moduleName, emptySet(), emptySet())

    moduleDir?.walkTopDown()
        ?.filter { it.isFile && it.extension == "kt" }
        ?.forEach { file -> module.files += TestFile.createCommitted(file, module) }

    return TestCase(
        id = TestCaseId.Named(moduleName),
        kind = TestKind.STANDALONE,
        modules = setOf(module),
        freeCompilerArgs = freeCompilerArgs,
        nominalPackageName = PackageName.EMPTY,
        checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
        extras = DEFAULT_EXTRAS
    ).apply {
        initialize(null, null)
    }
}

private fun AbstractNativeSimpleTest.compileToLibrary(
    testCase: TestCase,
    dir: File,
    dependencies: List<TestCompilationDependency<*>>
): TestCompilationResult.Success<out TestCompilationArtifact.KLIB> {
    val compilation = LibraryCompilation(
        settings = testRunSettings,
        freeCompilerArgs = testCase.freeCompilerArgs,
        sourceModules = testCase.modules,
        dependencies = dependencies,
        expectedArtifact = getLibraryArtifact(testCase, dir)
    )
    return compilation.result.assertSuccess()
}

private fun AbstractNativeSimpleTest.compileToExecutable(
    testCase: TestCase,
    dependencies: List<TestCompilationDependency<*>>
): TestCompilationResult<out TestCompilationArtifact.Executable> {
    val compilation = ExecutableCompilation(
        settings = testRunSettings,
        freeCompilerArgs = testCase.freeCompilerArgs,
        sourceModules = testCase.modules,
        extras = DEFAULT_EXTRAS,
        dependencies = dependencies,
        expectedArtifact = getExecutableArtifact()
    )
    return compilation.result
}

private fun getLibraryArtifact(testCase: TestCase, dir: File) =
    TestCompilationArtifact.KLIB(dir.resolve(testCase.modules.first().name + ".klib"))

private fun AbstractNativeSimpleTest.getExecutableArtifact() =
    TestCompilationArtifact.Executable(buildDir.resolve("app." + testRunSettings.get<KotlinNativeTargets>().testTarget.family.exeSuffix))

private fun directiveValues(testDataFileContents: String, directive: String) =
    InTextDirectivesUtils.findListWithPrefixes(testDataFileContents, "// $directive: ")

internal fun AbstractNativeSimpleTest.muteTestIfNecessary(testDataFile: File) = muteTestIfNecessary(FileUtil.loadFile(testDataFile))
internal fun AbstractNativeSimpleTest.muteTestIfNecessary(testDataFileContents: String) {
    val pipelineType = testRunSettings.get<PipelineType>()
    val mutedWhenValues = directiveValues(testDataFileContents, TestDirectives.MUTED_WHEN.name)
    Assumptions.assumeFalse(mutedWhenValues.any { it == pipelineType.mutedOption.name })
}

internal fun AbstractNativeSimpleTest.freeCompilerArgs(testDataFile: File) = freeCompilerArgs(FileUtil.loadFile(testDataFile))
internal fun AbstractNativeSimpleTest.freeCompilerArgs(testDataFileContents: String) =
    directiveValues(testDataFileContents, TestDirectives.FREE_COMPILER_ARGS.name)

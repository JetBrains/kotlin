/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.konan.blackboxtest.CachesAutoBuildTest.Companion.TEST_SUITE_PATH
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase.WithTestRunnerExtras
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.ExecutableCompilation
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.ExistingDependency
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.LibraryCompilation
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.Executable
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationDependency
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationDependencyType.Library
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Success
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.*
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.CacheMode
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.SimpleTestDirectories
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Timeouts
import org.jetbrains.kotlin.konan.blackboxtest.support.util.LAUNCHER_MODULE_NAME
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertTrue
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertFalse
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

@Tag("caches")
@EnforcedHostTarget
@TestMetadata(TEST_SUITE_PATH)
@TestDataPath("\$PROJECT_ROOT")
class CachesAutoBuildTest : AbstractNativeSimpleTest() {
    @Test
    @TestMetadata("simple")
    fun testSimple() {
        val rootDir = File("$TEST_SUITE_PATH/simple")
        val lib = compileToLibrary(rootDir.resolve("lib"), buildDir)
        val main = compileToExecutable(rootDir.resolve("main"), autoCacheFrom = buildDir, lib)

        assertTrue(main.executableFile.exists())
        assertTrue(autoCacheDir.resolve(cacheFlavor).resolve("lib").exists())
    }

    @Test
    @TestMetadata("dontCacheUserLib")
    fun testDontCacheUserLib() {
        val rootDir = File("$TEST_SUITE_PATH/dontCacheUserLib")
        val externalLib = compileToLibrary(rootDir.resolve("externalLib"), buildDir.resolve("external"))
        val userLib = compileToLibrary(rootDir.resolve("userLib"), buildDir.resolve("user"), externalLib)
        val main = compileToExecutable(rootDir.resolve("main"), autoCacheFrom = buildDir.resolve("external"), externalLib, userLib)

        assertTrue(main.executableFile.exists())
        assertTrue(autoCacheDir.resolve(cacheFlavor).resolve("externalLib").exists())
        assertFalse(autoCacheDir.resolve(cacheFlavor).resolve("userLib").exists())
    }


    private fun compileToLibrary(sourcesDir: File, outputDir: File, vararg dependencies: KLIB): KLIB {
        val testCase: TestCase = generateTestCaseWithSingleModule(sourcesDir)
        val compilationResult: Success<out KLIB> = testCase.compileToLibrary(outputDir, dependencies.map { it.asLibraryDependency() })
        return compilationResult.resultingArtifact
    }

    private fun compileToExecutable(sourcesDir: File, autoCacheFrom: File, vararg dependencies: KLIB): Executable {
        val testCase: TestCase = generateTestCaseWithSingleModule(
            sourcesDir,
            freeCompilerArgs = TestCompilerArgs(
                listOf(
                    "-Xauto-cache-from=${autoCacheFrom.absolutePath}",
                    "-Xauto-cache-dir=${autoCacheDir.absolutePath}",
                )
            )
        )
        val compilationResult = testCase.compileToExecutable(dependencies.map { it.asLibraryDependency() })
        return compilationResult.resultingArtifact
    }

    private fun generateTestCaseWithSingleModule(moduleDir: File?, freeCompilerArgs: TestCompilerArgs = TestCompilerArgs.EMPTY): TestCase {
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

    private fun TestCase.compileToLibrary(dir: File, vararg dependencies: TestCompilationDependency<*>) =
        compileToLibrary(dir, dependencies.asList())

    private fun TestCase.compileToLibrary(dir: File, dependencies: List<TestCompilationDependency<*>>): Success<out KLIB> {
        val compilation = LibraryCompilation(
            settings = testRunSettings,
            freeCompilerArgs = freeCompilerArgs,
            sourceModules = modules,
            dependencies = dependencies,
            expectedArtifact = toLibraryArtifact(dir)
        )
        return compilation.result.assertSuccess()
    }

    private fun TestCase.compileToExecutable(vararg dependencies: TestCompilationDependency<*>) =
        compileToExecutable(dependencies.asList())

    private fun TestCase.compileToExecutable(dependencies: List<TestCompilationDependency<*>>): Success<out Executable> {
        val compilation = ExecutableCompilation(
            settings = testRunSettings,
            freeCompilerArgs = freeCompilerArgs,
            sourceModules = modules,
            extras = DEFAULT_EXTRAS,
            dependencies = dependencies,
            expectedArtifact = toExecutableArtifact()
        )
        return compilation.result.assertSuccess()
    }

    private val buildDir: File get() = testRunSettings.get<SimpleTestDirectories>().testBuildDir
    private val autoCacheDir: File get() = buildDir.resolve("__auto_cache__")
    private val cacheFlavor: String
        get() = CacheMode.computeCacheDirName(
            testRunSettings.get<KotlinNativeTargets>().testTarget,
            "STATIC",
            testRunSettings.get<OptimizationMode>() == OptimizationMode.DEBUG
        )

    private fun TestCase.toLibraryArtifact(dir: File) = KLIB(dir.resolve(modules.first().name + ".klib"))
    private fun toExecutableArtifact() =
        Executable(buildDir.resolve("app." + testRunSettings.get<KotlinNativeTargets>().testTarget.family.exeSuffix))

    private fun KLIB.asLibraryDependency() = ExistingDependency(this, Library)

    companion object {
        const val TEST_SUITE_PATH = "native/native.tests/testData/caches/testAutoBuild"

        private val DEFAULT_EXTRAS = WithTestRunnerExtras(TestRunnerType.DEFAULT)
    }
}

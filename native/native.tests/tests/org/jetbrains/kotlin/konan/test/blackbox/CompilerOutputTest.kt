/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.forcesPreReleaseBinariesIfEnabled
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ObjCFrameworkCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CacheMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.klib.KlibCrossCompilationOutputTest.Companion.DEPRECATED_K1_LANGUAGE_VERSIONS_DIAGNOSTIC_REGEX
import org.jetbrains.kotlin.test.TestDataAssertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertIs

abstract class CompilerOutputTestBase : AbstractNativeSimpleTest() {
    @Test
    fun testReleaseCompilerAgainstPreReleaseLibrary() {
        val rootDir = File("native/native.tests/testData/compilerOutput/releaseCompilerAgainstPreReleaseLibrary")

        doTestPreReleaseKotlinLibrary(rootDir)
    }

    @Test
    fun testReleaseCompilerAgainstPreReleaseLibrarySkipPrereleaseCheck() {
        // We intentionally use JS testdata, because the compilers should behave the same way in such a test.
        // To be refactored later, after
        // CompileKotlinAgainstCustomBinariesTest.testReleaseCompilerAgainstPreReleaseLibraryJsSkipPrereleaseCheck is fixed.
        val rootDir =
            File("compiler/testData/compileKotlinAgainstCustomBinaries/releaseCompilerAgainstPreReleaseLibraryJsSkipPrereleaseCheck")

        doTestPreReleaseKotlinLibrary(
            rootDir = rootDir,
            additionalOptions = listOf("-Xskip-prerelease-check", "-Xsuppress-version-warnings")
        )
    }

    private fun doTestPreReleaseKotlinLibrary(rootDir: File, additionalOptions: List<String> = emptyList()) {
        val someNonStableVersion = LanguageVersion.entries.firstOrNull { it > LanguageVersion.LATEST_STABLE } ?: return
        doTestPreReleaseKotlin(
            rootDir = rootDir,
            libraryOptions = listOf("-language-version", someNonStableVersion.versionString, "-Xsuppress-version-warnings"),
            additionalOptions = additionalOptions
        )
    }

    protected fun doTestPreReleaseKotlin(
        rootDir: File,
        libraryOptions: List<String>,
        additionalOptions: List<String> = emptyList(),
        sanitizeCompilerOutput: (String) -> String = { it },
    ) {
        val library = compileLibrary(
            settings = testRunSettings,
            source = rootDir.resolve("library"),
            freeCompilerArgs = libraryOptions,
            dependencies = emptyList()
        ).assertSuccess().resultingArtifact

        val compilationResult = compileLibrary(
            testRunSettings,
            source = rootDir.resolve("source.kt"),
            freeCompilerArgs = additionalOptions,
            dependencies = listOf(library)
        )

        val goldenData = rootDir.resolve("output.txt")

        TestDataAssertions.assertEqualsToFile(goldenData, sanitizeCompilerOutput(compilationResult.toOutput()))
    }

    @Test
    fun testObjCExportDiagnostics() {
        val rootDir = File("native/native.tests/testData/compilerOutput/ObjCExportDiagnostics")
        val compilationResult = doBuildObjCFrameworkWithNameCollisions(rootDir, listOf("-Xbinary=objcExportReportNameCollisions=true"))
        val goldenData = rootDir.resolve("output.txt")

        TestDataAssertions.assertEqualsToFile(goldenData, compilationResult.toOutput().sanitizeCompilationOutput())
    }

    @Test
    fun testObjCExportDiagnosticsErrors() {
        val rootDir = File("native/native.tests/testData/compilerOutput/ObjCExportDiagnostics")
        val compilationResult = doBuildObjCFrameworkWithNameCollisions(rootDir, listOf("-Xbinary=objcExportErrorOnNameCollisions=true"))
        assertIs<TestCompilationResult.Failure>(compilationResult)
        val goldenData = rootDir.resolve("error.txt")

        TestDataAssertions.assertEqualsToFile(goldenData, compilationResult.toOutput().sanitizeCompilationOutput())
    }

    @Test
    fun testLoggingWarningWithDistCache() {
        val rootDir = File("native/native.tests/testData/compilerOutput/runtimeLogging")
        val testCase = generateTestCaseWithSingleFile(
            rootDir.resolve("main.kt"),
            freeCompilerArgs = TestCompilerArgs("-Xruntime-logs=gc=info"),
            extras = TestCase.NoTestRunnerExtras("main"),
            testKind = TestKind.STANDALONE_NO_TR,
        )
        val expectedArtifact = TestCompilationArtifact.Executable(buildDir.resolve("logging_warning_with_cache"))
        val compilation = ExecutableCompilation(
            testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            extras = testCase.extras,
            dependencies = emptyList(),
            expectedArtifact = expectedArtifact,
        )
        val compilationResult = compilation.result
        val goldenData = rootDir.resolve(
            if (testRunSettings.get<CacheMode>().useStaticCacheForDistributionLibraries) "logging_cache_warning.txt" else "empty.txt"
        )

        TestDataAssertions.assertEqualsToFile(goldenData, compilationResult.toOutput().sanitizeCompilationOutput())
    }

    @Test
    fun testCacheLinkageErrorMessage() {
        val rootDir = File("native/native.tests/testData/compilerOutput/cacheLinkageErrorMessage")

        // Trigger the binary linkage to fail by requesting a non-existing library:
        val secondStageCompilerArgs = listOf("-linker-option", "-ldoes-not-exist")

        val compilationResult = compileToExecutableInTwoStages(rootDir, secondStageCompilerArgs)

        assertIs<TestCompilationResult.Failure>(compilationResult)
        val output = compilationResult.toOutput().sanitizeCompilationOutput()

        if (testRunSettings.get<CacheMode>() == CacheMode.WithoutCache) {
            assertFalse(output.contains("compiler caches")) {
                "The compiler output wrongly mentions compiler caches:\n```\n$output\n```\n"
            }
        } else {
            val presetName = targets.testTarget.presetName
            assertContains(
                output,
                """
                |Please try to disable compiler caches and rerun the build. To disable compiler caches, add the following line to the gradle.properties file in the project's root directory:
                |    
                |    kotlin.native.cacheKind.$presetName=none
                |    
                |Also, consider filing an issue with full Gradle log here: https://kotl.in/issue""".trimMargin()
            )
        }
    }

    @Test
    fun testCacheBuildErrorMessage() {
        // Disable when not using cache:
        Assumptions.assumeFalse(testRunSettings.get<CacheMode>() == CacheMode.WithoutCache)
        // MinGW support for caches is limited to stdlib, so these tests won't work as expected.
        Assumptions.assumeFalse(targets.testTarget == KonanTarget.MINGW_X64)

        val rootDir = File("native/native.tests/testData/compilerOutput/cacheBuildErrorMessage")

        // Make sure the stdlib cache is built. Otherwise, it will fail instead of the main.klib cache.
        compileToExecutableInTwoStages(rootDir).assertSuccess()

        val autoCacheDir = buildDir.resolve("cache")
        autoCacheDir.mkdirs()

        val secondStageCompilerArgs = listOf(
            // Trigger the cache compilation to crash by passing an invalid flag:
            "-Xoverride-konan-properties=additionalCacheFlags=-Xpartial-linkage=invalid-mode",

            "-Xauto-cache-from=$buildDir",
            "-Xauto-cache-dir=$autoCacheDir",
        )
        val compilationResult = compileToExecutableInTwoStages(rootDir, secondStageCompilerArgs)

        assertIs<TestCompilationResult.Failure>(compilationResult)

        // Check only the first 5 lines because the rest contains a stack trace.
        val output = compilationResult.toOutput().sanitizeCompilationOutput()
            .lines().take(5).joinToString("\n")

        val goldenData = rootDir.resolve("output.txt")

        TestDataAssertions.assertEqualsToFile(goldenData, output)
    }

    @Test
    fun testIncrementalBuildErrorMessage() {
        // Disable when not using cache:
        Assumptions.assumeFalse(testRunSettings.get<CacheMode>() == CacheMode.WithoutCache)
        // MinGW support for caches is limited to stdlib, so these tests won't work as expected.
        Assumptions.assumeFalse(targets.testTarget == KonanTarget.MINGW_X64)

        val rootDir = File("native/native.tests/testData/compilerOutput/incrementalBuildErrorMessage")

        // Make sure the stdlib cache is built. Otherwise, it will fail instead of the main.klib incremental cache.
        compileToExecutableInTwoStages(rootDir).assertSuccess()

        val icCacheDir = buildDir.resolve("ic_cache")
        icCacheDir.mkdirs()

        val secondStageCompilerArgs = listOf(
            // Trigger the cache compilation to crash by passing an invalid flag:
            "-Xoverride-konan-properties=additionalCacheFlags=-Xpartial-linkage=invalid-mode",
            "-Xic-cache-dir=${icCacheDir.absolutePath}",
            "-Xenable-incremental-compilation"
        )
        val compilationResult = compileToExecutableInTwoStages(rootDir, secondStageCompilerArgs)

        assertIs<TestCompilationResult.Failure>(compilationResult)

        // Check only the first 5 lines because the rest contains a stack trace.
        val output = compilationResult.toOutput().sanitizeCompilationOutput()
            .lines().take(5).joinToString("\n")

        val goldenData = rootDir.resolve("output.txt")

        TestDataAssertions.assertEqualsToFile(goldenData, output)
    }

    private fun compileToExecutableInTwoStages(
        rootDir: File,
        secondStageCompilerArgs: List<String> = emptyList(),
    ): TestCompilationResult<out TestCompilationArtifact.Executable> {
        val testCase = generateTestCaseWithSingleFile(
            rootDir.resolve("main.kt"),
            freeCompilerArgs = TestCompilerArgs.EMPTY,
            extras = TestCase.NoTestRunnerExtras("main"),
            testKind = TestKind.STANDALONE_NO_TR,
        )

        val libraryCompilation = LibraryCompilation(
            testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            dependencies = emptyList(),
            expectedArtifact = TestCompilationArtifact.KLIB(buildDir.resolve("main.klib"))
        )

        val libraryCompilationAsDependency = CompiledDependency(
            libraryCompilation,
            TestCompilationDependencyType.IncludedLibrary
        )
        val compilation = ExecutableCompilation(
            testRunSettings,
            freeCompilerArgs = TestCompilerArgs(secondStageCompilerArgs),
            sourceModules = emptyList(),
            extras = testCase.extras,
            dependencies = listOf(libraryCompilationAsDependency),
            expectedArtifact = TestCompilationArtifact.Executable(buildDir.resolve("main")),
        )

        return compilation.result
    }

    fun String.sanitizeCompilationOutput(): String = lines().joinToString(separator = "\n") { line ->
        when {
            DEPRECATED_K1_LANGUAGE_VERSIONS_DIAGNOSTIC_REGEX.matches(line) -> ""
            else -> line
        }
    }

    @Test
    fun testLoggingInvalid() {
        Assumptions.assumeFalse(testRunSettings.get<CacheMode>().useStaticCacheForDistributionLibraries)
        val rootDir = File("native/native.tests/testData/compilerOutput/runtimeLogging")
        val testCase = generateTestCaseWithSingleFile(
            rootDir.resolve("main.kt"),
            freeCompilerArgs = TestCompilerArgs("-Xruntime-logs=invalid=unknown,logging=debug"),
            extras = TestCase.NoTestRunnerExtras("main"),
            testKind = TestKind.STANDALONE_NO_TR,
        )
        val expectedArtifact = TestCompilationArtifact.Executable(buildDir.resolve("logging_invalid"))
        val compilation = ExecutableCompilation(
            testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            extras = testCase.extras,
            dependencies = emptyList(),
            expectedArtifact = expectedArtifact,
        )
        val compilationResult = compilation.result
        val goldenData = rootDir.resolve("logging_invalid_error.txt")

        TestDataAssertions.assertEqualsToFile(goldenData, compilationResult.toOutput().sanitizeCompilationOutput())
    }

    private fun doBuildObjCFrameworkWithNameCollisions(rootDir: File, additionalOptions: List<String>): TestCompilationResult<out TestCompilationArtifact.ObjCFramework> {
        Assumptions.assumeTrue(targets.hostTarget.family.isAppleFamily)

        val settings = testRunSettings
        val lib1 = compileLibrary(settings, rootDir.resolve("lib1.kt")).assertSuccess().resultingArtifact
        val lib2 = compileLibrary(settings, rootDir.resolve("lib2.kt")).assertSuccess().resultingArtifact

        val freeCompilerArgs = TestCompilerArgs(
            listOf(
                "-Xinclude=${lib1.path}",
                "-Xinclude=${lib2.path}"
            ) + additionalOptions
        )
        val expectedArtifact = TestCompilationArtifact.ObjCFramework(buildDir, "testObjCExportDiagnostics")

        return ObjCFrameworkCompilation(
            settings,
            freeCompilerArgs,
            sourceModules = emptyList(),
            dependencies = emptyList(),
            expectedArtifact
        ).result
    }

    private val testClashingBindClassToObjCNameRootDir = File("native/native.tests/testData/compilerOutput/clashingBindClassToObjCName")

    private fun doTestClashingBindClassToObjCName(
        name: String,
        modules: Set<TestModule.Exclusive>,
    ) {
        Assumptions.assumeTrue(targets.hostTarget.family.isAppleFamily)
        // https://youtrack.jetbrains.com/issue/KT-71097/Kotlin-Native-add-a-flag-to-catch-duplicating-symbols-in-caches
        Assumptions.assumeFalse(testRunSettings.get<CacheMode>().useStaticCacheForUserLibraries)

        val testCase = TestCase(
            id = TestCaseId.Named(name),
            kind = TestKind.STANDALONE_NO_TR,
            modules = modules,
            freeCompilerArgs = TestCompilerArgs("-opt-in=kotlin.native.internal.InternalForKotlinNative"),
            nominalPackageName = PackageName("clashingBindClassToObjCName"),
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
            extras = TestCase.NoTestRunnerExtras(),
        ).apply {
            initialize(null, null)
        }

        // The order of errors is not be defined.
        val output = TestCompilationFactory().testCasesToExecutable(listOf(testCase), testRunSettings).result.toOutput()
            .replace("file\\d+\\.kt".toRegex(), "file*.kt")
            .replace("MyClassObjC\\d+".toRegex(), "MyClassObjC*")
        val goldenData = testClashingBindClassToObjCNameRootDir.resolve("${name}.output.txt")

        TestDataAssertions.assertEqualsToFile(goldenData, output.sanitizeCompilationOutput())
    }

    @Test
    fun testClashingBindClassToObjCName_privateClass() = doTestClashingBindClassToObjCName("privateClass", buildSet {
        val module = TestModule.newDefaultModule()
        module.files += TestFile.createCommitted(testClashingBindClassToObjCNameRootDir.resolve("privateClass.kt"), module)
        add(module)
    })

    @Test
    fun testClashingBindClassToObjCName_singleLib() = doTestClashingBindClassToObjCName("singleLib", buildSet {
        val module = TestModule.newDefaultModule()
        module.files += TestFile.createCommitted(testClashingBindClassToObjCNameRootDir.resolve("class.kt"), module)
        module.files += TestFile.createCommitted(testClashingBindClassToObjCNameRootDir.resolve("file1.kt"), module)
        module.files += TestFile.createCommitted(testClashingBindClassToObjCNameRootDir.resolve("file2.kt"), module)
        module.files += TestFile.createCommitted(testClashingBindClassToObjCNameRootDir.resolve("main.kt"), module)
        add(module)
    })

    @Test
    fun testClashingBindClassToObjCName_singleFile() = doTestClashingBindClassToObjCName("singleFile", buildSet {
        val module = TestModule.newDefaultModule()
        module.files += TestFile.createCommitted(testClashingBindClassToObjCNameRootDir.resolve("class.kt"), module)
        module.files += TestFile.createCommitted(testClashingBindClassToObjCNameRootDir.resolve("file3.kt"), module)
        module.files += TestFile.createCommitted(testClashingBindClassToObjCNameRootDir.resolve("main.kt"), module)
        add(module)
    })

    @Test
    fun testClashingBindClassToObjCName_classAndMain() = doTestClashingBindClassToObjCName("classAndMain", buildSet {
        val module1 = TestModule.Exclusive("classAndMain_1", emptySet(), emptySet(), emptySet())
        module1.files += TestFile.createCommitted(testClashingBindClassToObjCNameRootDir.resolve("class.kt"), module1)
        module1.files += TestFile.createCommitted(testClashingBindClassToObjCNameRootDir.resolve("file1.kt"), module1)
        add(module1)
        val module2 = TestModule.Exclusive("classAndMain_2", setOf(module1.name), emptySet(), emptySet())
        module2.files += TestFile.createCommitted(testClashingBindClassToObjCNameRootDir.resolve("file2.kt"), module2)
        module2.files += TestFile.createCommitted(testClashingBindClassToObjCNameRootDir.resolve("main.kt"), module2)
        add(module2)
    })

    @Test
    fun testClashingBindClassToObjCName_separateLibs() = doTestClashingBindClassToObjCName("separateLibs", buildSet {
        val module1 = TestModule.Exclusive("separateLibs_1", emptySet(), emptySet(), emptySet())
        module1.files += TestFile.createCommitted(testClashingBindClassToObjCNameRootDir.resolve("class.kt"), module1)
        add(module1)
        val module2 = TestModule.Exclusive("separateLibs_2", setOf(module1.name), emptySet(), emptySet())
        module2.files += TestFile.createCommitted(testClashingBindClassToObjCNameRootDir.resolve("file1.kt"), module2)
        add(module2)
        val module3 = TestModule.Exclusive("separateLibs_3", setOf(module1.name), emptySet(), emptySet())
        module3.files += TestFile.createCommitted(testClashingBindClassToObjCNameRootDir.resolve("file2.kt"), module3)
        add(module3)
        val module4 = TestModule.Exclusive("separateLibs_4", setOf(module1.name, module2.name, module3.name), emptySet(), emptySet())
        module4.files += TestFile.createCommitted(testClashingBindClassToObjCNameRootDir.resolve("main.kt"), module4)
        add(module4)
    })
}

@TestDataPath("\$PROJECT_ROOT")
@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
class CompilerOutputTest : CompilerOutputTestBase() {
    @Test
    fun testReleaseCompilerAgainstPreReleaseFeature() {
        val rootDir = File("native/native.tests/testData/compilerOutput/releaseCompilerAgainstPreReleaseFeature")

        val arbitraryPoisoningFeature = LanguageFeature.entries.firstOrNull { it.forcesPreReleaseBinariesIfEnabled() } ?: return

        val poisonedLibrary = compileLibrary(
            settings = testRunSettings,
            source = rootDir.resolve("poisonedLibrary"),
            freeCompilerArgs = listOf("-XXLanguage:+$arbitraryPoisoningFeature",),
        ).assertSuccess().resultingArtifact

        val library = compileLibrary(
            settings = testRunSettings,
            source = rootDir.resolve("library"),
        ).assertSuccess().resultingArtifact

        val compilationResult = compileLibrary(
            testRunSettings,
            source = rootDir.resolve("source.kt"),
            dependencies = listOf(poisonedLibrary, library)
        ).toOutput()

        TestDataAssertions.assertEqualsToFile(
            rootDir.resolve("output.txt"),
            compilationResult.replace(arbitraryPoisoningFeature.name, "<!POISONING_LANGUAGE_FEATURE!>")
        )
    }

    @Test
    fun testReleaseCompilerWithoutUsageOfPreReleaseFeature() {
        val rootDir = File("native/native.tests/testData/compilerOutput/releaseCompilerWithoutUsageOfPreReleaseFeature")

        val arbitraryPoisoningFeature = LanguageFeature.entries.firstOrNull { it.forcesPreReleaseBinariesIfEnabled() } ?: return

        val poisonedLibrary = compileLibrary(
            settings = testRunSettings,
            source = rootDir.resolve("poisonedLibrary"),
            freeCompilerArgs = listOf("-XXLanguage:+$arbitraryPoisoningFeature",),
        ).assertSuccess().resultingArtifact

        val library = compileLibrary(
            settings = testRunSettings,
            source = rootDir.resolve("library"),
        ).assertSuccess().resultingArtifact

        val compilationResult = compileLibrary(
            testRunSettings,
            source = rootDir.resolve("source.kt"),
            dependencies = listOf(poisonedLibrary, library)
        ).toOutput()

        TestDataAssertions.assertEqualsToFile(
            rootDir.resolve("output.txt"),
            compilationResult.replace(arbitraryPoisoningFeature.name, "<!POISONING_LANGUAGE_FEATURE!>")
        )
    }
}

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.klib.KlibABITestUtils
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase.WithTestRunnerExtras
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationDependencyType.Library
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.*
import org.jetbrains.kotlin.konan.blackboxtest.support.util.*
import org.junit.jupiter.api.Tag
import org.opentest4j.TestAbortedException
import java.io.File

@Tag("klib-abi")
abstract class AbstractNativeKlibABITest : AbstractNativeSimpleTest() {
    private val producedKlibs = linkedMapOf<KLIB, KlibABITestUtils.Dependencies>() // IMPORTANT: The order makes sense!

    private inner class NativeTestConfiguration(testPath: String) : KlibABITestUtils.TestConfiguration {
        override val testDir = getAbsoluteFile(testPath)
        override val buildDir get() = this@AbstractNativeKlibABITest.buildDir
        override val stdlibFile get() = this@AbstractNativeKlibABITest.stdlibFile

        override val testModeName = with(testRunSettings.get<CacheMode>()) {
            val cacheModeAlias = when {
                staticCacheRootDir == null -> CacheMode.Alias.NO
                !staticCacheRequiredForEveryLibrary -> CacheMode.Alias.STATIC_ONLY_DIST
                else -> CacheMode.Alias.STATIC_EVERYWHERE
            }

            "NATIVE_CACHE_${cacheModeAlias}"
        }

        override fun buildKlib(moduleName: String, moduleSourceDir: File, dependencies: KlibABITestUtils.Dependencies, klibFile: File) =
            this@AbstractNativeKlibABITest.buildKlib(moduleName, moduleSourceDir, dependencies, klibFile)

        override fun buildBinaryAndRun(mainModuleKlibFile: File, dependencies: KlibABITestUtils.Dependencies) =
            this@AbstractNativeKlibABITest.buildBinaryAndRun(dependencies)

        override fun onNonEmptyBuildDirectory(directory: File) = backupDirectoryContents(directory)

        override fun onIgnoredTest() = throw TestAbortedException()
    }

    // The entry point to generated test classes.
    protected fun runTest(@TestDataFile testPath: String) = KlibABITestUtils.runTest(NativeTestConfiguration(testPath))

    private fun buildKlib(moduleName: String, moduleSourceDir: File, dependencies: KlibABITestUtils.Dependencies, klibFile: File) {
        val module = createModule(moduleName)
        moduleSourceDir.walk()
            .filter { file -> file.isFile && file.extension == "kt" }
            .forEach { file -> module.files += TestFile.createCommitted(file, module) }

        val testCase = createTestCase(module, COMPILER_ARGS_FOR_KLIB)

        val klibArtifact = KLIB(klibFile)
        val compilation = LibraryCompilation(
            settings = testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            dependencies = createLibraryDependencies(dependencies),
            expectedArtifact = klibArtifact
        )

        compilation.result.assertSuccess() // <-- trigger compilation

        producedKlibs[klibArtifact] = dependencies // Remember the artifact with its dependencies.
    }

    private fun buildBinaryAndRun(allDependencies: KlibABITestUtils.Dependencies) {
        val cacheDependencies = if (staticCacheRequiredForEveryLibrary) {
            producedKlibs.map { (klibArtifact, moduleDependencies) ->
                buildCacheForKlib(moduleDependencies, klibArtifact)
                klibArtifact.toStaticCacheArtifact().toDependency()
            }
        } else
            emptyList()

        val (sourceDir, outputDir) = KlibABITestUtils.createModuleDirs(buildDir, LAUNCHER_MODULE_NAME)
        val executableFile = outputDir.resolve("app." + testRunSettings.get<KotlinNativeTargets>().testTarget.family.exeSuffix)

        val module = createModule(LAUNCHER_MODULE_NAME)
        module.files += TestFile.createUncommitted(
            location = sourceDir.resolve(LAUNCHER_FILE_NAME),
            module = module,
            text = generateBoxFunctionLauncher("box")
        )

        val testCase = createTestCase(module, COMPILER_ARGS_FOR_STATIC_CACHE_AND_EXECUTABLE)

        val compilation = ExecutableCompilation(
            settings = testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            extras = testCase.extras,
            dependencies = createLibraryDependencies(allDependencies) + cacheDependencies,
            expectedArtifact = Executable(executableFile)
        )

        val compilationResult = compilation.result.assertSuccess() // <-- trigger compilation
        val executable = TestExecutable.fromCompilationResult(testCase, compilationResult)

        runExecutableAndVerify(testCase, executable) // <-- run executable and verify
    }

    private fun buildCacheForKlib(moduleDependencies: KlibABITestUtils.Dependencies, klibArtifact: KLIB) {
        val compilation = StaticCacheCompilation(
            settings = testRunSettings,
            freeCompilerArgs = COMPILER_ARGS_FOR_STATIC_CACHE_AND_EXECUTABLE,
            options = StaticCacheCompilation.Options.Regular,
            dependencies = createLibraryCacheDependencies(moduleDependencies) + klibArtifact.toDependency(),
            expectedArtifact = klibArtifact.toStaticCacheArtifact()
        )

        compilation.result.assertSuccess() // <-- trigger compilation
    }

    private fun createModule(moduleName: String) = TestModule.Exclusive(
        name = moduleName,
        directDependencySymbols = emptySet(), /* Don't need to pass any dependency symbols here.
                                                 Dependencies are already handled by the AbstractKlibABITestCase class. */
        directFriendSymbols = emptySet()
    )

    private fun createTestCase(module: TestModule.Exclusive, compilerArgs: TestCompilerArgs) = TestCase(
        id = TestCaseId.Named(module.name),
        kind = TestKind.STANDALONE,
        modules = setOf(module),
        freeCompilerArgs = compilerArgs,
        nominalPackageName = PackageName.EMPTY,
        checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
        extras = DEFAULT_EXTRAS
    ).apply {
        initialize(null, null)
    }

    private fun createLibraryDependencies(dependencies: KlibABITestUtils.Dependencies): Iterable<TestCompilationDependency<KLIB>> =
        with(dependencies) {
            regularDependencies.map { KLIB(it).toDependency() } + friendDependencies.map { KLIB(it).toFriendDependency() }
        }

    private fun createLibraryCacheDependencies(dependencies: KlibABITestUtils.Dependencies): Iterable<TestCompilationDependency<KLIBStaticCache>> =
        with(dependencies) {
            regularDependencies.mapNotNull { klibFile ->
                if (klibFile != stdlibFile) KLIB(klibFile).toStaticCacheArtifact().toDependency() else null
            }
        }

    private fun KLIB.toDependency() = ExistingDependency(this, Library)
    private fun KLIB.toFriendDependency() = ExistingDependency(this, TestCompilationDependencyType.FriendLibrary)
    private fun KLIBStaticCache.toDependency() = ExistingDependency(this, TestCompilationDependencyType.LibraryStaticCache)

    private fun KLIB.toStaticCacheArtifact() = KLIBStaticCache(
        cacheDir = klibFile.parentFile.resolve(STATIC_CACHE_DIR_NAME).apply { mkdirs() },
        klib = this
    )

    private val buildDir: File get() = testRunSettings.get<SimpleTestDirectories>().testBuildDir
    private val stdlibFile: File get() = testRunSettings.get<KotlinNativeHome>().stdlibFile
    private val staticCacheRequiredForEveryLibrary: Boolean get() = testRunSettings.get<CacheMode>().staticCacheRequiredForEveryLibrary

    companion object {
        private val COMPILER_ARGS_FOR_KLIB = TestCompilerArgs(
            listOf("-nostdlib") // stdlib is passed explicitly.
        )

        private val COMPILER_ARGS_FOR_STATIC_CACHE_AND_EXECUTABLE = TestCompilerArgs(
            COMPILER_ARGS_FOR_KLIB.compilerArgs + "-Xpartial-linkage"
        )

        private val DEFAULT_EXTRAS = WithTestRunnerExtras(TestRunnerType.DEFAULT)

        private const val BACKED_UP_DIRECTORY_PREFIX = "__backup-"

        private fun backupDirectoryContents(directory: File) {
            val filesToBackup = directory.listFiles()?.mapNotNull { file ->
                if (file.isDirectory && file.name.startsWith(BACKED_UP_DIRECTORY_PREFIX)) null else file
            }

            if (!filesToBackup.isNullOrEmpty()) {
                val backupDirectory = directory.resolve("$BACKED_UP_DIRECTORY_PREFIX${System.currentTimeMillis()}__")
                backupDirectory.mkdirs()

                filesToBackup.forEach { file -> file.renameTo(backupDirectory.resolve(file.name)) }
            }
        }
    }
}

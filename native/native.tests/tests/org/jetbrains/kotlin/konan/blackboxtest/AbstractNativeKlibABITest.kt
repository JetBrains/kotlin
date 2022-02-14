/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.klib.AbstractKlibABITestCase
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase.WithTestRunnerExtras
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationDependencyType.Library
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.CacheMode
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.SimpleTestDirectories
import org.jetbrains.kotlin.konan.blackboxtest.support.util.*
import org.junit.jupiter.api.Tag
import java.io.File

@Tag("klib-abi")
abstract class AbstractNativeKlibABITest : AbstractNativeSimpleTest() {
    private val producedKlibs = linkedMapOf<KLIB, Collection<File>>() // IMPORTANT: The order makes sense!

    protected fun runTest(@TestDataFile testPath: String): Unit = AbstractKlibABITestCase.doTest(
        testDir = getAbsoluteFile(testPath),
        buildDir = buildDir,
        stdlibFile = stdlibFile,
        buildKlib = ::buildKlib,
        buildBinaryAndRun = { _, allDependencies -> buildBinaryAndRun(allDependencies) },
        onNonEmptyBuildDirectory = ::backupDirectoryContents
    )

    private fun buildKlib(moduleName: String, moduleSourceDir: File, moduleDependencies: Collection<File>, klibFile: File) {
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
            dependencies = createLibraryDependencies(moduleDependencies),
            expectedArtifact = klibArtifact
        )

        compilation.result.assertSuccess() // <-- trigger compilation

        producedKlibs[klibArtifact] = moduleDependencies // Remember the artifact with its dependencies.
    }

    private fun buildBinaryAndRun(allDependencies: Collection<File>) {
        val cacheDependencies = if (staticCacheRequiredForEveryLibrary) {
            producedKlibs.map { (klibArtifact, moduleDependencies) ->
                buildCacheForKlib(moduleDependencies, klibArtifact)
                klibArtifact.toStaticCacheArtifact().toDependency()
            }
        } else
            emptyList()

        val (sourceDir, outputDir) = AbstractKlibABITestCase.createModuleDirs(buildDir, LAUNCHER_MODULE_NAME)
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

    private fun buildCacheForKlib(moduleDependencies: Collection<File>, klibArtifact: KLIB) {
        val compilation = StaticCacheCompilation(
            settings = testRunSettings,
            freeCompilerArgs = COMPILER_ARGS_FOR_STATIC_CACHE_AND_EXECUTABLE,
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
        expectedOutputDataFile = null,
        extras = DEFAULT_EXTRAS
    ).apply {
        initialize(null)
    }

    private fun createLibraryDependencies(klibFiles: Iterable<File>): Iterable<TestCompilationDependency<KLIB>> =
        klibFiles.map { klibFile -> KLIB(klibFile).toDependency() }

    private fun createLibraryCacheDependencies(klibFiles: Iterable<File>): Iterable<TestCompilationDependency<KLIBStaticCache>> =
        klibFiles.mapNotNull { klibFile -> if (klibFile != stdlibFile) KLIB(klibFile).toStaticCacheArtifact().toDependency() else null }

    private fun KLIB.toDependency() = ExistingDependency(this, Library)
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

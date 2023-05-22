/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.codegen.ProjectInfo
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.Dependencies
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.MAIN_MODULE_NAME
import org.jetbrains.kotlin.klib.PartialLinkageTestUtils.ModuleBuildDirs
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase.WithTestRunnerExtras
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationDependencyType.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.blackboxtest.support.group.UsePartialLinkage
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.*
import org.jetbrains.kotlin.konan.blackboxtest.support.util.*
import org.junit.jupiter.api.Tag
import org.opentest4j.TestAbortedException
import java.io.File

@Tag("partial-linkage")
@UsePartialLinkage(UsePartialLinkage.Mode.ENABLED_WITH_WARNING)
abstract class AbstractNativePartialLinkageTest : AbstractNativeSimpleTest() {
    private inner class NativeTestConfiguration(testPath: String) : PartialLinkageTestUtils.TestConfiguration {
        override val testDir = getAbsoluteFile(testPath)
        override val buildDir get() = this@AbstractNativePartialLinkageTest.buildDir
        override val stdlibFile get() = this@AbstractNativePartialLinkageTest.stdlibFile

        override val testModeName = with(testRunSettings.get<CacheMode>()) {
            val cacheModeAlias = when {
                !useStaticCacheForDistributionLibraries -> CacheMode.Alias.NO
                !useStaticCacheForUserLibraries -> CacheMode.Alias.STATIC_ONLY_DIST
                else -> CacheMode.Alias.STATIC_EVERYWHERE
            }

            "NATIVE_CACHE_${cacheModeAlias}"
        }

        override fun customizeModuleSources(moduleName: String, moduleSourceDir: File) {
            if (moduleName == MAIN_MODULE_NAME)
                customizeMainModuleSources(moduleSourceDir)
        }

        override fun buildKlib(
            moduleName: String,
            buildDirs: ModuleBuildDirs,
            dependencies: Dependencies,
            klibFile: File
        ) = this@AbstractNativePartialLinkageTest.buildKlib(moduleName, buildDirs.sourceDir, dependencies, klibFile)

        override fun buildBinaryAndRun(mainModuleKlibFile: File, dependencies: Dependencies) =
            this@AbstractNativePartialLinkageTest.buildBinaryAndRun(dependencies)

        override fun onNonEmptyBuildDirectory(directory: File) = backupDirectoryContents(directory)

        override fun isIgnoredTest(projectInfo: ProjectInfo) =
            super.isIgnoredTest(projectInfo) || projectInfo.name == "externalDeclarations"

        override fun onIgnoredTest() = throw TestAbortedException()
    }

    private class ProducedKlib(val moduleName: String, val klibArtifact: KLIB, val dependencies: Dependencies) {
        override fun equals(other: Any?) = (other as? ProducedKlib)?.moduleName == moduleName
        override fun hashCode() = moduleName.hashCode()
    }

    private val producedKlibs = linkedSetOf<ProducedKlib>() // IMPORTANT: The order makes sense!

    private val executableArtifact: Executable by lazy {
        val (_, outputDir) = PartialLinkageTestUtils.createModuleDirs(buildDir, LAUNCHER_MODULE_NAME)
        val executableFile = outputDir.resolve("app." + testRunSettings.get<KotlinNativeTargets>().testTarget.family.exeSuffix)
        Executable(executableFile)
    }

    // The entry point to generated test classes.
    protected fun runTest(@TestDataFile testPath: String) = PartialLinkageTestUtils.runTest(NativeTestConfiguration(testPath))

    private fun customizeMainModuleSources(moduleSourceDir: File) {
        // Add a "box" function launcher to the main module.
        moduleSourceDir.resolve(LAUNCHER_FILE_NAME).writeText(generateBoxFunctionLauncher("box"))
    }

    private fun buildKlib(moduleName: String, moduleSourceDir: File, dependencies: Dependencies, klibFile: File) {
        val klibArtifact = KLIB(klibFile)

        val testCase = createTestCase(moduleName, moduleSourceDir, COMPILER_ARGS)

        val compilation = LibraryCompilation(
            settings = testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            dependencies = createLibraryDependencies(dependencies, forExecutable = false),
            expectedArtifact = klibArtifact
        )

        compilation.result.assertSuccess() // <-- trigger compilation

        producedKlibs += ProducedKlib(moduleName, klibArtifact, dependencies) // Remember the artifact with its dependencies.
    }

    private fun buildBinaryAndRun(allDependencies: Dependencies) {
        val cacheDependencies = if (useStaticCacheForUserLibraries) {
            producedKlibs.map { producedKlib ->
                buildCacheForKlib(producedKlib)
                producedKlib.klibArtifact.toStaticCacheArtifact().toDependency()
            }
        } else
            emptyList()

        val testCase = createTestCase(
            moduleName = LAUNCHER_MODULE_NAME,
            moduleSourceDir = null, // No sources.
            compilerArgs = COMPILER_ARGS
        )

        val compilation = ExecutableCompilation(
            settings = testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            extras = testCase.extras,
            dependencies = createLibraryDependencies(allDependencies, forExecutable = true) + cacheDependencies,
            expectedArtifact = executableArtifact
        )

        val compilationResult = compilation.result.assertSuccess() // <-- trigger compilation
        val executable = TestExecutable.fromCompilationResult(testCase, compilationResult)

        runExecutableAndVerify(testCase, executable) // <-- run executable and verify
    }

    private fun buildCacheForKlib(producedKlib: ProducedKlib) {
        val compilation = StaticCacheCompilation(
            settings = testRunSettings,
            freeCompilerArgs = COMPILER_ARGS,
            options = if (producedKlib.moduleName == MAIN_MODULE_NAME)
                StaticCacheCompilation.Options.ForIncludedLibraryWithTests(executableArtifact, DEFAULT_EXTRAS)
            else
                StaticCacheCompilation.Options.Regular,
            pipelineType = testRunSettings.get(),
            dependencies = createLibraryCacheDependencies(producedKlib.dependencies) + producedKlib.klibArtifact.toDependency(),
            expectedArtifact = producedKlib.klibArtifact.toStaticCacheArtifact()
        )

        compilation.result.assertSuccess() // <-- trigger compilation
    }

    private fun createTestCase(moduleName: String, moduleSourceDir: File?, compilerArgs: TestCompilerArgs): TestCase {
        // Note: Don't generate a module if there are no actual sources to compile.
        val module: TestModule.Exclusive? = moduleSourceDir?.let {
            TestModule.Exclusive(
                name = moduleName,
                directDependencySymbols = emptySet(), /* Don't need to pass any dependency symbols here.
                                                         Dependencies are already handled by the AbstractNativePartialLinkageTest class. */
                directFriendSymbols = emptySet(),
                directDependsOnSymbols = emptySet(),
            ).also { module ->
                moduleSourceDir.walk()
                    .filter { file -> file.isFile && file.extension == "kt" }
                    .forEach { file -> module.files += TestFile.createCommitted(file, module) }
            }
        }

        return TestCase(
            id = TestCaseId.Named(moduleName),
            kind = TestKind.STANDALONE,
            modules = setOfNotNull(module),
            freeCompilerArgs = compilerArgs,
            nominalPackageName = PackageName.EMPTY,
            checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
            extras = DEFAULT_EXTRAS
        ).apply {
            initialize(null, null)
        }
    }

    private fun createLibraryDependencies(
        dependencies: Dependencies,
        forExecutable: Boolean
    ): Iterable<TestCompilationDependency<KLIB>> =
        dependencies.regularDependencies.map { dependency ->
            val klib = KLIB(dependency.libraryFile)
            if (forExecutable && dependency.moduleName == MAIN_MODULE_NAME) klib.toIncludedDependency() else klib.toDependency()
        } + dependencies.friendDependencies.map { KLIB(it.libraryFile).toFriendDependency() }

    private fun createLibraryCacheDependencies(
        dependencies: Dependencies
    ): Iterable<TestCompilationDependency<KLIBStaticCache>> = dependencies.regularDependencies.mapNotNull { dependency ->
        if (dependency.libraryFile != stdlibFile) KLIB(dependency.libraryFile).toStaticCacheArtifact().toDependency() else null
    }

    private fun KLIB.toDependency() = ExistingDependency(this, Library)
    private fun KLIB.toIncludedDependency() = ExistingDependency(this, IncludedLibrary)
    private fun KLIB.toFriendDependency() = ExistingDependency(this, FriendLibrary)
    private fun KLIBStaticCache.toDependency() = ExistingDependency(this, LibraryStaticCache)

    private fun KLIB.toStaticCacheArtifact() = KLIBStaticCache(
        cacheDir = klibFile.parentFile.resolve(STATIC_CACHE_DIR_NAME).apply { mkdirs() },
        klib = this
    )

    private val buildDir: File get() = testRunSettings.get<Binaries>().testBinariesDir
    private val stdlibFile: File get() = testRunSettings.get<KotlinNativeHome>().stdlibFile
    private val useStaticCacheForUserLibraries: Boolean get() = testRunSettings.get<CacheMode>().useStaticCacheForUserLibraries

    companion object {
        private val COMPILER_ARGS = TestCompilerArgs(
            listOf("-nostdlib") // stdlib is passed explicitly.
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

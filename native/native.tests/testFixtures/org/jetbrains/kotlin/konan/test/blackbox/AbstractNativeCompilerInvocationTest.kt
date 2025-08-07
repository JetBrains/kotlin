/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.klib.KlibCompilerEdition
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils.Dependencies
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils.Dependency
import org.jetbrains.kotlin.klib.KlibCompilerInvocationTestUtils.MAIN_MODULE_NAME
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase.WithTestRunnerExtras
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationDependencyType.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.*
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Binaries
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.util.*
import org.jetbrains.kotlin.test.TargetBackend
import org.opentest4j.TestAbortedException
import java.io.File

/**
 * This is a base class for tests with repetitive Kotlin/Native compiler invocations.
 *
 * Examples:
 * - Partial linkage tests:
 *   - Build multiple KLIBs with dependencies between some of them
 *   - Rebuild and substitute some of those KLIBs
 *   - Finally, build a binary and run it
 * - KLIB compatibility tests:
 *   - Build KLIBs with one [KlibCompilerEdition]
 *   - Then build a binary with another [KlibCompilerEdition]
 */
abstract class AbstractNativeCompilerInvocationTest :
    AbstractNativeSimpleTest(),
    KlibCompilerInvocationTestUtils.BinaryRunner<NativeCompilerInvocationTestBinaryArtifact> {

    final override fun runBinary(binaryArtifact: NativeCompilerInvocationTestBinaryArtifact) =
        with(binaryArtifact) { runExecutableAndVerify(testCase, executable) }
}

class NativeCompilerInvocationTestConfiguration(
    val settings: Settings
) : KlibCompilerInvocationTestUtils.TestConfiguration {
    override val buildDir get() = settings.get<Binaries>().testBinariesDir
    override val stdlibFile get() = settings.get<KotlinNativeHome>().stdlibFile
    override val targetBackend get() = TargetBackend.NATIVE
    override fun onIgnoredTest() = throw TestAbortedException()
}

class NativeCompilerInvocationTestBinaryArtifact(
    val testCase: TestCase,
    val executable: TestExecutable,
) : KlibCompilerInvocationTestUtils.BinaryArtifact

class NativeCompilerInvocationTestArtifactBuilder(
    private val configuration: NativeCompilerInvocationTestConfiguration
) : KlibCompilerInvocationTestUtils.ArtifactBuilder<NativeCompilerInvocationTestBinaryArtifact> {
    private val settings get() = configuration.settings

    private class ProducedKlib(val moduleName: String, val klibArtifact: KLIB, val dependencies: Dependencies) {
        override fun equals(other: Any?) = (other as? ProducedKlib)?.moduleName == moduleName
        override fun hashCode() = moduleName.hashCode()
    }

    private val producedKlibs = linkedSetOf<ProducedKlib>() // IMPORTANT: The order makes sense!

    private val executableArtifact: Executable by lazy {
        val outputDir = settings.get<Binaries>().testBinariesDir.resolve(LAUNCHER_MODULE_NAME).apply { mkdirs() }
        val executableFile = outputDir.resolve("app." + settings.get<KotlinNativeTargets>().testTarget.family.exeSuffix)
        Executable(executableFile)
    }

    override fun buildKlib(
        module: KlibCompilerInvocationTestUtils.TestStructure.ModuleUnderTest,
        dependencies: Dependencies,
        compilerEdition: KlibCompilerEdition,
        compilerArguments: List<String>,
    ) {
        require(compilerEdition == KlibCompilerEdition.CURRENT) { "Partial Linkage tests accept only Current compiler" }

        val srcFiles = module.sourceDir.listFiles()
        check(srcFiles != null && srcFiles.isNotEmpty()) { "No source files could be found" }
        val hasKtFiles = srcFiles.any { it.name.endsWith(".kt") }
        val defFiles = srcFiles.filter { it.name.endsWith(".def") }
        check(!hasKtFiles || defFiles.isEmpty()) { "Both .kt and .def files are specified" }
        check(defFiles.size <= 1) { "More than one .def file is specified" }

        val klibArtifact = KLIB(module.klibFile)

        if (hasKtFiles) {
            val testCase = createTestCase(
                moduleName = module.moduleInfo.moduleName,
                moduleSourceDir = module.sourceDir,
                compilerArgs = COMPILER_ARGS.plusCompilerArgs(compilerArguments)
            )

            val compilation = LibraryCompilation(
                settings = settings,
                freeCompilerArgs = testCase.freeCompilerArgs,
                sourceModules = testCase.modules,
                dependencies = createLibraryDependencies(dependencies),
                expectedArtifact = klibArtifact
            )

            compilation.result.assertSuccess() // <-- trigger compilation
        } else {
            val compilation = CInteropCompilation(
                settings.get(),
                settings.get(),
                TestCompilerArgs(compilerArguments),
                srcFiles[0],
                sources = srcFiles.filterNot { it == srcFiles[0] },
                dependencies = emptyList(),
                klibArtifact,
            )

            compilation.result.assertSuccess() // <-- trigger compilation
        }

        // Remember the artifact with its dependencies.
        producedKlibs += ProducedKlib(
            module.moduleInfo.moduleName,
            klibArtifact,
            dependencies
        )
    }

    override fun buildBinary(
        mainModule: Dependency,
        otherDependencies: Dependencies,
        compilerEdition: KlibCompilerEdition,
    ): NativeCompilerInvocationTestBinaryArtifact {
        require(compilerEdition == KlibCompilerEdition.CURRENT) { "Partial Linkage tests accept only Current compiler" }

        val cacheDependencies = if (settings.get<CacheMode>().useStaticCacheForUserLibraries) {
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
            settings = settings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            extras = testCase.extras,
            dependencies = buildList {
                this += createIncludedDependency(mainModule)
                this += createLibraryDependencies(otherDependencies)
                this += cacheDependencies
            },
            expectedArtifact = executableArtifact
        )

        val compilationResult = compilation.result.assertSuccess() // <-- trigger compilation
        val executable = TestExecutable.fromCompilationResult(testCase, compilationResult)

        return NativeCompilerInvocationTestBinaryArtifact(testCase, executable)
    }

    private fun createTestCase(moduleName: String, moduleSourceDir: File?, compilerArgs: TestCompilerArgs): TestCase {
        // Note: Don't generate a module if there are no actual sources to compile.
        val module: TestModule.Exclusive? = moduleSourceDir?.let {
            TestModule.Exclusive(
                name = moduleName,
                /* Don't need to pass any dependency symbols here. Dependencies are already handled by the NativeArtifactBuilder class. */
                directRegularDependencySymbols = emptySet(),
                directFriendDependencySymbols = emptySet(),
                directDependsOnDependencySymbols = emptySet(),
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
            checks = TestRunChecks.Default(settings.get<Timeouts>().executionTimeout),
            extras = DEFAULT_EXTRAS
        ).apply {
            initialize(null, null)
        }
    }

    private fun buildCacheForKlib(producedKlib: ProducedKlib) {
        val compilation = StaticCacheCompilation(
            settings = settings,
            freeCompilerArgs = COMPILER_ARGS,
            options = if (producedKlib.moduleName == MAIN_MODULE_NAME)
                StaticCacheCompilation.Options.ForIncludedLibraryWithTests(
                    expectedExecutableArtifact = executableArtifact,
                    extras = DEFAULT_EXTRAS
                )
            else
                StaticCacheCompilation.Options.Regular,
            dependencies = createLibraryCacheDependencies(producedKlib.dependencies) + producedKlib.klibArtifact.toDependency(),
            expectedArtifact = producedKlib.klibArtifact.toStaticCacheArtifact()
        )

        compilation.result.assertSuccess() // <-- trigger compilation
    }

    private fun createIncludedDependency(dependency: Dependency): TestCompilationDependency<KLIB> =
        KLIB(dependency.libraryFile).toIncludedDependency()

    private fun createLibraryDependencies(dependencies: Dependencies): Iterable<TestCompilationDependency<KLIB>> =
        dependencies.regularDependencies.map { dependency -> KLIB(dependency.libraryFile).toDependency() } +
                dependencies.friendDependencies.map { KLIB(it.libraryFile).toFriendDependency() }

    private fun createLibraryCacheDependencies(dependencies: Dependencies): Iterable<TestCompilationDependency<KLIBStaticCache>> =
        dependencies.regularDependencies.mapNotNull { dependency ->
            if (dependency.libraryFile != configuration.stdlibFile)
                KLIB(dependency.libraryFile).toStaticCacheArtifact().toDependency()
            else null
        }

    private fun KLIB.toDependency() = ExistingDependency(this, Library)
    private fun KLIB.toIncludedDependency() = ExistingDependency(this, IncludedLibrary)
    private fun KLIB.toFriendDependency() = ExistingDependency(this, FriendLibrary)
    private fun KLIBStaticCache.toDependency() = ExistingDependency(this, LibraryStaticCache)

    private fun KLIB.toStaticCacheArtifact() = KLIBStaticCacheImpl(
        cacheDir = klibFile.parentFile.resolve(STATIC_CACHE_DIR_NAME).apply { mkdirs() },
        klib = this
    )

    companion object {
        private val DEFAULT_EXTRAS = WithTestRunnerExtras(TestRunnerType.DEFAULT)

        private val COMPILER_ARGS = TestCompilerArgs(
            listOf(
                "-nostdlib", // stdlib is passed explicitly.
            )
        )
    }
}

/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.testFramework.TestDataFile
import org.jetbrains.kotlin.compatibility.binary.AbstractKlibBinaryCompatibilityTest
import org.jetbrains.kotlin.konan.blackboxtest.support.*
import org.jetbrains.kotlin.konan.blackboxtest.support.TestFile
import org.jetbrains.kotlin.konan.blackboxtest.support.TestModule
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.KLIBStaticCache
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestExecutable
import org.jetbrains.kotlin.konan.blackboxtest.support.runner.TestRunChecks
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.CacheMode
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.SimpleTestDirectories
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Timeouts
import org.jetbrains.kotlin.konan.blackboxtest.support.util.*
import org.jetbrains.kotlin.utils.DFS
import org.junit.jupiter.api.Tag
import java.io.File
import kotlin.math.max
import org.jetbrains.kotlin.compatibility.binary.TestFile as BinaryCompatibilityTestFile
import org.jetbrains.kotlin.compatibility.binary.TestModule as BinaryCompatibilityTestModule

@Tag("klib-abi")
@Tag("klib-binary-compatibility")
abstract class AbstractNativeKlibBinaryCompatibilityTest : AbstractNativeSimpleTest() {

    protected fun runTest(@TestDataFile testPath: String): Unit = AbstractKlibBinaryCompatibilityTest.doTest(
        filePath = testPath,
        expectedResult = "OK",
        produceKlib = ::buildKlib,
        produceAndRunProgram = ::buildAndExecuteBinary
    )

    private fun buildKlib(
        binaryCompatibilityTestModule: BinaryCompatibilityTestModule,
        version: Int
    ) {
        val module = binaryCompatibilityTestModule.toLightDependencyWithVersion(version)
        val moduleDependencies = collectDependencies(
            binaryCompatibilityTestModule.dependenciesSymbols,
            withVersion = version
        )
        val klibFile = module.klibFile

        val testCase = mkTestCase(module, COMPILER_ARGS_FOR_KLIB)

        val compilation = LibraryCompilation(
            settings = testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            dependencies = moduleDependencies.map { it.klibFile.toKlib().toDependency() },
            expectedArtifact = klibFile.toKlib()
        )

        compilation.trigger()

        updateModule(module, moduleDependencies)
    }

    private fun buildAndExecuteBinary(
        mainTestModule: BinaryCompatibilityTestModule,
        expectedResult: String
    ) {
        val binaryVersion = moduleVersions.values.maxOrNull() ?: 2

        buildKlib(mainTestModule, binaryVersion)

        val (binarySourceDir, binaryOutputDir) = listOf(BINARY_SOURCE_DIR_NAME, BINARY_OUTPUT_DIR_NAME).map {
            buildDir.resolve(LAUNCHER_MODULE_NAME).resolve(it).apply { mkdirs() }
        }

        val launcherModule = LightDependencyWithVersion(LAUNCHER_MODULE_NAME, binaryVersion).apply {
            module.files += TestFile.createUncommitted(
                location = binarySourceDir.resolve(LAUNCHER_FILE_NAME),
                module = module,
                text = generateBoxFunctionLauncher("box", expectedResult)
            )
        }
        val launcherDependencies = collectDependencies(mainTestModule.name, binaryVersion)
        val executableFile = binaryOutputDir.resolve(
            "app." + testRunSettings.get<KotlinNativeTargets>().testTarget.family.exeSuffix
        )

        val cachedDependencies = if (staticCacheRequiredForEveryLibrary) {
            latestDependencies.map { (module, deps) ->
                buildCacheForKlib(module, deps)
                module.klibFile.toStaticCacheArtifact().toDependency()
            }
        } else {
            emptyList()
        }

        val testCase = mkTestCase(launcherModule, COMPILER_ARGS_FOR_STATIC_CACHE_AND_EXECUTABLE)

        val compilation = ExecutableCompilation(
            settings = testRunSettings,
            freeCompilerArgs = testCase.freeCompilerArgs,
            sourceModules = testCase.modules,
            extras = testCase.extras,
            dependencies = launcherDependencies.map {
                it.klibFile.toKlib().toDependency()
            } + cachedDependencies,
            expectedArtifact = TestCompilationArtifact.Executable(executableFile)
        )

        val compilationResult = compilation.trigger()
        val executable = TestExecutable.fromCompilationResult(testCase, compilationResult)

        runExecutableAndVerify(testCase, executable)
    }

    private fun buildCacheForKlib(
        module: LightDependencyWithVersion,
        moduleDependencies: Collection<LightDependencyWithVersion>
    ) {
        val klib = module.klibFile

        val compilation = StaticCacheCompilation(
            settings = testRunSettings,
            freeCompilerArgs = COMPILER_ARGS_FOR_STATIC_CACHE_AND_EXECUTABLE,
            options = StaticCacheCompilation.Options.Regular,
            pipelineType = testRunSettings.get(),
            dependencies = moduleDependencies.map {
                it.klibFile.toStaticCacheArtifact().toDependency()
            } + klib.toKlib().toDependency(),
            expectedArtifact = klib.toStaticCacheArtifact()
        )

        compilation.trigger()
    }

    private inner class LightDependencyWithVersion(
        val name: String, val version: Int
    ) {
        val module: TestModule.Exclusive = TestModule.Exclusive(
            name = name,
            directDependencySymbols = emptySet(),
            directFriendSymbols = emptySet(),
            directDependsOnSymbols = emptySet(),
        )

        val localBuildDir: File =
            buildDir.resolveModuleWithVersion(name, version).apply { mkdirs() }

        val klibFile: File =
            buildDir.resolveKlibFileWithVersion(name, version).apply { mkdirs() }

        //region Poor person's inner data class
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as LightDependencyWithVersion

            if (name != other.name) return false
            if (version != other.version) return false

            return true
        }

        override fun hashCode(): Int {
            var result = name.hashCode()
            result = 31 * result + version.hashCode()
            return result
        }

        override fun toString(): String = "LightDependencyWithVersion($name:$version)"
        //endregion
    }

    private val moduleVersions = mutableMapOf<String, Int>()
    private val moduleDependencies = mutableMapOf<String, Set<String>>()

    private fun updateModule(
        module: LightDependencyWithVersion,
        dependencies: Collection<LightDependencyWithVersion>
    ) {
        moduleVersions[module.name] = max(
            moduleVersions.getOrDefault(module.name, 0),
            module.version
        )
        moduleDependencies[module.name] = union(
            moduleDependencies.getOrElse(module.name) { emptySet() },
            dependencies.mapToSet { it.name }
        )
    }

    private val latestDependencies: Map<LightDependencyWithVersion, Collection<LightDependencyWithVersion>>
        get() {
            val res: MutableMap<LightDependencyWithVersion, Collection<LightDependencyWithVersion>> = mutableMapOf()
            for ((name, version) in moduleVersions) {
                val module = LightDependencyWithVersion(name, version)
                val symbols = moduleDependencies[name] ?: continue
                res[module] = symbols.map { LightDependencyWithVersion(it, moduleVersions[it]!!) }
            }
            return res
        }

    private fun collectDependencies(
        rootDependency: String,
        withVersion: Int
    ): Collection<LightDependencyWithVersion> =
        collectDependencies(listOf(rootDependency), withVersion)

    private fun collectDependencies(
        rootDependencies: Collection<String>,
        withVersion: Int
    ): Collection<LightDependencyWithVersion> {
        fun findLightDependency(dependency: String): LightDependencyWithVersion? {
            val latestVersion = moduleVersions[dependency] ?: return null
            return LightDependencyWithVersion(dependency, withVersion.coerceAtMost(latestVersion))
        }

        return DFS.topologicalOrder(rootDependencies.mapNotNull(::findLightDependency)) { module ->
            val moduleDependencies = moduleDependencies[module.name] ?: return@topologicalOrder emptyList()
            moduleDependencies.mapNotNull { name -> findLightDependency(name) }
        }
    }

    private fun BinaryCompatibilityTestFile.toUncommittedIn(
        extra: LightDependencyWithVersion
    ): TestFile<TestModule.Exclusive> = TestFile.createUncommitted(
        location = extra.localBuildDir.resolve(name),
        module = extra.module,
        text = content
    )

    private fun BinaryCompatibilityTestModule.toLightDependencyWithVersion(
        version: Int
    ): LightDependencyWithVersion {
        val extra = LightDependencyWithVersion(name, version)
        versionFiles(version).forEach { extra.module.files += it.toUncommittedIn(extra) }
        return extra
    }

    private fun mkTestCase(
        extra: LightDependencyWithVersion,
        compilerArgs: TestCompilerArgs
    ): TestCase = TestCase(
        id = TestCaseId.Named(extra.name),
        kind = TestKind.STANDALONE,
        modules = setOf(extra.module),
        freeCompilerArgs = compilerArgs,
        nominalPackageName = PackageName.EMPTY,
        checks = TestRunChecks.Default(testRunSettings.get<Timeouts>().executionTimeout),
        extras = DEFAULT_EXTRAS
    ).apply {
        initialize(null, null)
    }

    private val buildDir: File
        get() = testRunSettings.get<SimpleTestDirectories>().testBuildDir
    private val staticCacheRequiredForEveryLibrary: Boolean
        get() = testRunSettings.get<CacheMode>().staticCacheRequiredForEveryLibrary

    companion object {
        private val COMPILER_ARGS_FOR_KLIB = TestCompilerArgs.EMPTY
        private val COMPILER_ARGS_FOR_STATIC_CACHE_AND_EXECUTABLE = TestCompilerArgs.EMPTY

        private val DEFAULT_EXTRAS = TestCase.WithTestRunnerExtras(TestRunnerType.DEFAULT)

        private const val BINARY_SOURCE_DIR_NAME = "sources"
        private const val BINARY_OUTPUT_DIR_NAME = "outputs"
    }
}

private fun File.resolveModuleWithVersion(moduleName: String, version: Int): File =
    resolve(moduleName).resolve("$version")
private fun File.resolveKlibFileWithVersion(moduleName: String, version: Int): File =
    resolveModuleWithVersion(moduleName, version).resolve("${moduleName}.klib")

private fun File.toKlib(): KLIB = KLIB(this)
private fun File.toStaticCacheArtifact() = KLIBStaticCache(
    cacheDir = parentFile.resolve(STATIC_CACHE_DIR_NAME).apply { mkdirs() },
    klib = KLIB(this)
)

private fun KLIB.toDependency() =
    ExistingDependency(this, TestCompilationDependencyType.Library)
private fun KLIBStaticCache.toDependency() =
    ExistingDependency(this, TestCompilationDependencyType.LibraryStaticCache)

private fun <T : TestCompilationArtifact> BasicCompilation<T>.trigger(): TestCompilationResult.Success<out T> =
    result.assertSuccess()

private fun <T> union(set: Set<T>, otherSet: Set<T>) = set.union(otherSet)

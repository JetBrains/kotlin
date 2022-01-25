/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest.support.compilation

import org.jetbrains.kotlin.konan.blackboxtest.support.PackageName
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCase
import org.jetbrains.kotlin.konan.blackboxtest.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.blackboxtest.support.TestModule
import org.jetbrains.kotlin.konan.blackboxtest.support.TestModule.Companion.allDependencies
import org.jetbrains.kotlin.konan.blackboxtest.support.TestModule.Companion.allFriends
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.*
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationDependencyType.*
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Binaries
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.CacheKind
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Settings
import org.jetbrains.kotlin.konan.blackboxtest.support.util.*
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File

internal class TestCompilationFactory {
    private val cachedKlibCompilations = ThreadSafeCache<KlibCacheKey, KlibCompilations>()
    private val cachedExecutableCompilations = ThreadSafeCache<ExecutableCacheKey, TestCompilation<Executable>>()

    private data class KlibCacheKey(val sourceModules: Set<TestModule>, val freeCompilerArgs: TestCompilerArgs)
    private data class ExecutableCacheKey(val sourceModules: Set<TestModule>)

    // A pair of compilations for a KLIB itself and for its static cache that are created together.
    private data class KlibCompilations(val klib: TestCompilation<KLIB>, val staticCache: TestCompilation<KLIBStaticCache>?)

    private data class SourceBasedCompilationDependencies(
        val klibDependencies: List<CompiledDependency<KLIB>>,
        val staticCacheDependencies: List<CompiledDependency<KLIBStaticCache>>
    ) {
        fun all(): Iterable<CompiledDependency<*>> = (klibDependencies.asSequence() + staticCacheDependencies).asIterable()

        fun staticCacheDependenciesWith(klib: CompiledDependency<KLIB>): Iterable<CompiledDependency<*>> =
            (staticCacheDependencies.asSequence() + klib).asIterable()
    }

    fun testCasesToExecutable(testCases: Collection<TestCase>, settings: Settings): TestCompilation<Executable> {
        val rootModules = testCases.flatMapToSet { testCase -> testCase.rootModules }
        val cacheKey = ExecutableCacheKey(rootModules)

        // Fast pass.
        cachedExecutableCompilations[cacheKey]?.let { return it }

        // Long pass.
        val freeCompilerArgs = rootModules.first().testCase.freeCompilerArgs // Should be identical inside the same test case group.
        val extras = testCases.first().extras // Should be identical inside the same test case group.
        val dependencies = collectDependencies(rootModules, freeCompilerArgs, settings).all()
        val executableArtifact = Executable(settings.artifactFileForExecutable(rootModules))

        return cachedExecutableCompilations.computeIfAbsent(cacheKey) {
            ExecutableCompilation(
                settings = settings,
                freeCompilerArgs = freeCompilerArgs,
                sourceModules = rootModules,
                extras = extras,
                dependencies = dependencies,
                expectedArtifact = executableArtifact
            )
        }
    }

    private fun moduleToKlib(sourceModule: TestModule, freeCompilerArgs: TestCompilerArgs, settings: Settings): KlibCompilations {
        val sourceModules = setOf(sourceModule)
        val cacheKey = KlibCacheKey(sourceModules, freeCompilerArgs)

        // Fast pass.
        cachedKlibCompilations[cacheKey]?.let { return it }

        // Long pass.
        val dependencies = collectDependencies(sourceModules, freeCompilerArgs, settings)
        val klibArtifact = KLIB(settings.artifactFileForKlib(sourceModule, freeCompilerArgs))

        val staticCacheArtifact: KLIBStaticCache? = if (settings.get<CacheKind>().staticCacheRequiredForEveryLibrary)
            KLIBStaticCache(cacheDir = klibArtifact.cacheDirForStaticCache(), klib = klibArtifact)
        else
            null // No artifact means no static cache should be compiled.

        return cachedKlibCompilations.computeIfAbsent(cacheKey) {
            val klibCompilation = LibraryCompilation(
                settings = settings,
                freeCompilerArgs = freeCompilerArgs,
                sourceModules = sourceModules,
                dependencies = dependencies.klibDependencies,
                expectedArtifact = klibArtifact
            )

            val staticCacheCompilation: StaticCacheCompilation? = if (staticCacheArtifact != null) {
                StaticCacheCompilation(
                    settings = settings,
                    freeCompilerArgs = freeCompilerArgs,
                    dependencies = dependencies.staticCacheDependenciesWith(klibCompilation.asKlibDependency(type = /* does not matter in fact*/ Library)),
                    expectedArtifact = staticCacheArtifact
                )
            } else
                null

            KlibCompilations(klibCompilation, staticCacheCompilation)
        }
    }

    private fun collectDependencies(
        sourceModules: Set<TestModule>,
        freeCompilerArgs: TestCompilerArgs,
        settings: Settings
    ): SourceBasedCompilationDependencies {
        val klibDependencies = mutableListOf<CompiledDependency<KLIB>>()
        val staticCacheDependencies = mutableListOf<CompiledDependency<KLIBStaticCache>>()

        fun <T : TestCompilationDependencyType<KLIB>> Set<TestModule>.collectDependencies(type: T) =
            forEach { dependencyModule: TestModule ->
                val klibCompilations = moduleToKlib(dependencyModule, freeCompilerArgs, settings)
                klibDependencies += klibCompilations.klib.asKlibDependency(type)
                staticCacheDependencies.addIfNotNull(klibCompilations.staticCache?.asStaticCacheDependency())
            }

        sourceModules.allDependencies().collectDependencies(Library)
        sourceModules.allFriends().collectDependencies(FriendLibrary)

        return SourceBasedCompilationDependencies(klibDependencies, staticCacheDependencies)
    }

    companion object {
        private fun Set<TestModule>.allDependencies() = if (size == 1) first().allDependencies else flatMapToSet { it.allDependencies }
        private fun Set<TestModule>.allFriends() = if (size == 1) first().allFriends else flatMapToSet { it.allFriends }

        private fun <T : TestCompilationDependencyType<KLIB>> TestCompilation<KLIB>.asKlibDependency(type: T): CompiledDependency<KLIB> =
            CompiledDependency(this, type)

        private fun TestCompilation<KLIBStaticCache>.asStaticCacheDependency(): CompiledDependency<KLIBStaticCache> =
            CompiledDependency(this, LibraryStaticCache)

        private fun Settings.artifactFileForExecutable(modules: Set<TestModule.Exclusive>) = when (modules.size) {
            1 -> artifactFileForExecutable(modules.first())
            else -> multiModuleArtifactFile(modules, get<KotlinNativeTargets>().testTarget.family.exeSuffix)
        }

        private fun Settings.artifactFileForExecutable(module: TestModule.Exclusive) =
            singleModuleArtifactFile(module, get<KotlinNativeTargets>().testTarget.family.exeSuffix)

        private fun Settings.artifactFileForKlib(module: TestModule, freeCompilerArgs: TestCompilerArgs) = when (module) {
            is TestModule.Exclusive -> singleModuleArtifactFile(module, "klib")
            is TestModule.Shared -> get<Binaries>().sharedBinariesDir.resolve("${module.name}-${prettyHash(freeCompilerArgs.hashCode())}.klib")
        }

        private fun KLIB.cacheDirForStaticCache(): File = klibFile.parentFile.resolve(STATIC_CACHE_DIR_NAME).apply { mkdirs() }

        private fun Settings.singleModuleArtifactFile(module: TestModule.Exclusive, extension: String): File {
            val artifactFileName = buildString {
                append(module.testCase.nominalPackageName.compressedPackageName).append('.')
                if (extension == "klib") append(module.name).append('.')
                append(extension)
            }
            return artifactDirForPackageName(module.testCase.nominalPackageName).resolve(artifactFileName)
        }

        private fun Settings.multiModuleArtifactFile(modules: Collection<TestModule>, extension: String): File {
            var filesCount = 0
            var hash = 0
            val uniquePackageNames = hashSetOf<PackageName>()

            modules.forEach { module ->
                module.files.forEach { file ->
                    filesCount++
                    hash = hash * 31 + file.hashCode()
                }

                if (module is TestModule.Exclusive)
                    uniquePackageNames += module.testCase.nominalPackageName
            }

            val commonPackageName = uniquePackageNames.findCommonPackageName()

            val artifactFileName = buildString {
                val prefix = filesCount.toString()
                repeat(4 - prefix.length) { append('0') }
                append(prefix).append('-')

                if (!commonPackageName.isEmpty())
                    append(commonPackageName.compressedPackageName).append('-')

                append(prettyHash(hash))

                append('.').append(extension)
            }

            return artifactDirForPackageName(commonPackageName).resolve(artifactFileName)
        }

        private fun Settings.artifactDirForPackageName(packageName: PackageName?): File {
            val baseDir = get<Binaries>().testBinariesDir
            val outputDir = if (packageName != null) baseDir.resolve(packageName.compressedPackageName) else baseDir

            outputDir.mkdirs()

            return outputDir
        }
    }
}

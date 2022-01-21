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
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.Executable
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationArtifact.KLIB
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationDependencyType.FriendLibrary
import org.jetbrains.kotlin.konan.blackboxtest.support.compilation.TestCompilationDependencyType.Library
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Binaries
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Settings
import org.jetbrains.kotlin.konan.blackboxtest.support.util.*
import java.io.File

internal class TestCompilationFactory {
    private val cachedKlibCompilations = ThreadSafeCache<KlibCacheKey, TestCompilation<KLIB>>()
    private val cachedExecutableCompilations = ThreadSafeCache<ExecutableCacheKey, TestCompilation<Executable>>()

    private data class KlibCacheKey(val sourceModules: Set<TestModule>, val freeCompilerArgs: TestCompilerArgs)
    private data class ExecutableCacheKey(val sourceModules: Set<TestModule>)

    fun testCasesToExecutable(testCases: Collection<TestCase>, settings: Settings): TestCompilation<Executable> {
        val rootModules = testCases.flatMapToSet { testCase -> testCase.rootModules }
        val cacheKey = ExecutableCacheKey(rootModules)

        // Fast pass.
        cachedExecutableCompilations[cacheKey]?.let { return it }

        // Long pass.
        val freeCompilerArgs = rootModules.first().testCase.freeCompilerArgs // Should be identical inside the same test case group.
        val extras = testCases.first().extras // Should be identical inside the same test case group.
        val dependencies = buildList {
            rootModules.flatMapToSet { it.allDependencies }.mapTo(this) { it.asKlibDependency(freeCompilerArgs, settings, Library) }
            rootModules.flatMapToSet { it.allFriends }.mapTo(this) { it.asKlibDependency(freeCompilerArgs, settings, FriendLibrary) }
        }
        val expectedArtifact = Executable(settings.artifactFileForExecutable(rootModules))

        return cachedExecutableCompilations.computeIfAbsent(cacheKey) {
            ExecutableCompilation(
                settings = settings,
                freeCompilerArgs = freeCompilerArgs,
                sourceModules = rootModules,
                extras = extras,
                dependencies = dependencies,
                expectedArtifact = expectedArtifact
            )
        }
    }

    private fun moduleToKlib(sourceModule: TestModule, freeCompilerArgs: TestCompilerArgs, settings: Settings): TestCompilation<KLIB> {
        val sourceModules = setOf(sourceModule)
        val cacheKey = KlibCacheKey(sourceModules, freeCompilerArgs)

        // Fast pass.
        cachedKlibCompilations[cacheKey]?.let { return it }

        // Long pass.
        val dependencies = buildList {
            sourceModule.allDependencies.mapTo(this) { it.asKlibDependency(freeCompilerArgs, settings, Library) }
            sourceModule.allFriends.mapTo(this) { it.asKlibDependency(freeCompilerArgs, settings, FriendLibrary) }
        }
        val expectedArtifact = KLIB(settings.artifactFileForKlib(sourceModule, freeCompilerArgs))

        return cachedKlibCompilations.computeIfAbsent(cacheKey) {
            LibraryCompilation(
                settings = settings,
                freeCompilerArgs = freeCompilerArgs,
                sourceModules = sourceModules,
                dependencies = dependencies,
                expectedArtifact = expectedArtifact
            )
        }
    }

    private fun <T : TestCompilationDependencyType<KLIB>> TestModule.asKlibDependency(
        freeCompilerArgs: TestCompilerArgs,
        settings: Settings,
        type: T
    ) = CompiledDependency(moduleToKlib(this, freeCompilerArgs, settings), type)

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

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
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Binaries
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.KotlinNativeTargets
import org.jetbrains.kotlin.konan.blackboxtest.support.settings.Settings
import org.jetbrains.kotlin.konan.blackboxtest.support.util.*
import java.io.File

internal class TestCompilationFactory {
    private val cachedCompilations = ThreadSafeCache<TestCompilationCacheKey, TestCompilation>()

    private sealed interface TestCompilationCacheKey {
        data class Klib(val sourceModules: Set<TestModule>, val freeCompilerArgs: TestCompilerArgs) : TestCompilationCacheKey
        data class Executable(val sourceModules: Set<TestModule>) : TestCompilationCacheKey
    }

    fun testCasesToExecutable(testCases: Collection<TestCase>, settings: Settings): TestCompilation {
        val rootModules = testCases.flatMapToSet { testCase -> testCase.rootModules }
        val cacheKey = TestCompilationCacheKey.Executable(rootModules)

        // Fast pass.
        cachedCompilations[cacheKey]?.let { return it }

        // Long pass.
        val freeCompilerArgs = rootModules.first().testCase.freeCompilerArgs // Should be identical inside the same test case group.
        val extras = testCases.first().extras // Should be identical inside the same test case group.
        val libraries = rootModules.flatMapToSet { it.allDependencies }.map { moduleToKlib(it, freeCompilerArgs, settings) }
        val friends = rootModules.flatMapToSet { it.allFriends }.map { moduleToKlib(it, freeCompilerArgs, settings) }

        return cachedCompilations.computeIfAbsent(cacheKey) {
            TestCompilation.createForExecutable(
                settings = settings,
                freeCompilerArgs = freeCompilerArgs,
                sourceModules = rootModules,
                extras = extras,
                dependencies = TestCompilationDependencies(libraries = libraries, friends = friends),
                expectedExecutableFile = settings.artifactFileForExecutable(rootModules),
            )
        }
    }

    private fun moduleToKlib(sourceModule: TestModule, freeCompilerArgs: TestCompilerArgs, settings: Settings): TestCompilation {
        val sourceModules = setOf(sourceModule)
        val cacheKey = TestCompilationCacheKey.Klib(sourceModules, freeCompilerArgs)

        // Fast pass.
        cachedCompilations[cacheKey]?.let { return it }

        // Long pass.
        val libraries = sourceModule.allDependencies.map { moduleToKlib(it, freeCompilerArgs, settings) }
        val friends = sourceModule.allFriends.map { moduleToKlib(it, freeCompilerArgs, settings) }

        return cachedCompilations.computeIfAbsent(cacheKey) {
            TestCompilation.createForKlib(
                settings = settings,
                freeCompilerArgs = freeCompilerArgs,
                sourceModules = sourceModules,
                dependencies = TestCompilationDependencies(libraries = libraries, friends = friends),
                expectedKlibFile = settings.artifactFileForKlib(sourceModule, freeCompilerArgs)
            )
        }
    }

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

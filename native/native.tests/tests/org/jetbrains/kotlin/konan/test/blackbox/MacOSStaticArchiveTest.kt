/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.BinaryLibraryCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.ExistingDependency
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.StaticCacheCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationDependencyType
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.BinaryLibraryKind
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CacheMode
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.configurables
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.toolsExecutor
import org.jetbrains.kotlin.konan.test.blackbox.support.util.compileWithClangToStaticLibrary
import org.jetbrains.kotlin.konan.test.blackbox.support.util.createUniversalBinary
import org.jetbrains.kotlin.konan.test.blackbox.support.util.universalBinaryArchs
import org.jetbrains.kotlin.native.executors.runProcess
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
class MacOSStaticArchiveTest : AbstractNativeSimpleTest() {

    @Test
    fun includedUniversalArchive___staticLibraryIndexesItsSymbols() = assertIncludedSymbolIsIndexed(
        outputKind = OutputKind.STATIC_LIBRARY,
    )

    @Test
    fun includedUniversalArchive___staticCacheIndexesItsSymbols() = assertIncludedSymbolIsIndexed(
        outputKind = OutputKind.STATIC_CACHE,
    )

    @Test
    fun includedUniversalArchiveWithoutTargetSlice___isIgnored() = assertIncludedSymbolIsIndexed(
        outputKind = OutputKind.STATIC_LIBRARY,
        includeTargetSlice = false,
    )

    // KT-89897
    @Test
    fun includedUniversalArchive___staticLibraryIsReproducible() = assertProducesSameArchives(
        outputKind = OutputKind.STATIC_LIBRARY,
    )

    // KT-89897
    @Test
    fun includedUniversalArchive___staticCacheIsReproducible() = assertProducesSameArchives(
        outputKind = OutputKind.STATIC_CACHE,
    )

    enum class OutputKind {
        STATIC_LIBRARY,
        STATIC_CACHE,
    }

    private val includedFunction = "includedFunction"

    private fun assertIncludedSymbolIsIndexed(outputKind: OutputKind, includeTargetSlice: Boolean = true) {
        val klibWithIncludedUniversalBinary = compileLibraryWithIncludedUniversalArchive(outputKind, includeTargetSlice)
        val archives = compileArchives(outputKind, klibWithIncludedUniversalBinary, outputDir = buildDir.resolve("output"))

        val indexedSymbols = archives.values.flatMap { archiveIndex(it) }
        assertEquals(includeTargetSlice, "_$includedFunction" in indexedSymbols, "Unexpected archive index: $indexedSymbols")
    }

    private fun assertProducesSameArchives(outputKind: OutputKind) {
        val klibWithIncludedUniversalBinary = compileLibraryWithIncludedUniversalArchive(outputKind)
        val firstArchives = compileArchives(outputKind, klibWithIncludedUniversalBinary, outputDir = buildDir.resolve("output1"))
        val secondArchives = compileArchives(outputKind, klibWithIncludedUniversalBinary, outputDir = buildDir.resolve("output2"))

        assertEquals(firstArchives.keys, secondArchives.keys)
        for (path in firstArchives.keys) {
            val firstArchive = firstArchives.getValue(path)
            val secondArchive = secondArchives.getValue(path)
            assertContentEquals(firstArchive.readBytes(), secondArchive.readBytes(), "$firstArchive and $secondArchive differ")
        }
    }

    private fun compileLibraryWithIncludedUniversalArchive(
        outputKind: OutputKind,
        includeTargetSlice: Boolean = true,
    ): TestCompilationArtifact.KLIB {
        Assumptions.assumeTrue(targets.hostTarget.family.isAppleFamily)
        if (outputKind == OutputKind.STATIC_CACHE) {
            Assumptions.assumeFalse(testRunSettings.get<CacheMode>() == CacheMode.WithoutCache)
        }

        return compileToLibraryIncludeBinaryOnly(createUniversalArchive(includeTargetSlice))
    }

    private fun compileArchives(outputKind: OutputKind, klib: TestCompilationArtifact.KLIB, outputDir: File): Map<String, File> {
        // Debug info refers to the output location; remap it like the distribution build does for its caches (see KonanCacheTask),
        // so that the outputs produced into different directories can be compared.
        val freeCompilerArgs = TestCompilerArgs("-Xdebug-prefix-map=${outputDir.canonicalPath}=out")
        outputDir.mkdirs()
        when (outputKind) {
            OutputKind.STATIC_LIBRARY -> BinaryLibraryCompilation(
                testRunSettings,
                freeCompilerArgs = freeCompilerArgs,
                sourceModules = emptyList(),
                dependencies = listOf(ExistingDependency(klib, TestCompilationDependencyType.IncludedLibrary)),
                expectedArtifact = TestCompilationArtifact.BinaryLibrary(outputDir.resolve("libstub.a")),
                kind = BinaryLibraryKind.STATIC,
            ).result.assertSuccess()
            OutputKind.STATIC_CACHE -> StaticCacheCompilation(
                testRunSettings,
                freeCompilerArgs = freeCompilerArgs,
                StaticCacheCompilation.Options.Regular,
                dependencies = listOf(klib.asLibraryDependency()),
                expectedArtifact = TestCompilationArtifact.KLIBStaticCacheImpl(outputDir, klib),
            ).result.assertSuccess()
        }
        return outputDir.walkTopDown()
            .filter { it.isFile && it.extension == "a" }
            .associateBy { it.relativeTo(outputDir).path }
            .also { assertTrue(it.isNotEmpty(), "No archives produced in $outputDir") }
    }

    private fun archiveIndex(archive: File): List<String> {
        val nm = "${testRunSettings.configurables.absoluteTargetToolchain}/bin/nm"
        // The archive index is printed first: "Archive map", then "<symbol> in <member>" lines up to an empty line.
        return toolsExecutor.runProcess(nm, "--print-armap", archive.canonicalPath).stdout
            .substringAfter("Archive map\n").substringBefore("\n\n")
            .lines().map { it.substringBefore(" in ") }
    }

    private fun createUniversalArchive(includeTargetSlice: Boolean): File {
        val source = buildDir.resolve("included.c")
        source.writeText("int $includedFunction(void) { return 42; }")

        // The first one is the target's own arch.
        val archs = if (includeTargetSlice) universalBinaryArchs else universalBinaryArchs.drop(1)
        return createUniversalBinary(archs, outputFile = buildDir.resolve("libincluded.a")) { arch ->
            compileWithClangToStaticLibrary(
                sourceFiles = listOf(source),
                outputFile = buildDir.resolve("libincluded_$arch.a"),
                additionalClangFlags = listOf("-arch", arch),
            ).assertSuccess().resultingArtifact.libraryFile
        }
    }
}

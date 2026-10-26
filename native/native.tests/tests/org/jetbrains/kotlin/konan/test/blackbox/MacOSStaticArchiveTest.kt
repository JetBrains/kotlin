/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.ClassLevelProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.EnforcedProperty
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.BinaryLibraryCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.StaticCacheCompilation
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.BinaryLibraryKind
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.CacheMode
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// KT-89897
@EnforcedProperty(ClassLevelProperty.COMPILER_OUTPUT_INTERCEPTOR, "NONE")
class MacOSStaticArchiveTest : AbstractNativeSimpleTest() {

    @Test
    fun staticLibraryIsReproducible() = assertProducesSameArchives(OutputKind.STATIC_LIBRARY)

    @Test
    fun staticCacheIsReproducible() = assertProducesSameArchives(OutputKind.STATIC_CACHE)

    enum class OutputKind {
        STATIC_LIBRARY,
        STATIC_CACHE,
    }

    private fun assertProducesSameArchives(outputKind: OutputKind) {
        Assumptions.assumeTrue(targets.hostTarget.family.isAppleFamily)
        if (outputKind == OutputKind.STATIC_CACHE) {
            Assumptions.assumeFalse(testRunSettings.get<CacheMode>() == CacheMode.WithoutCache)
        }

        val source = buildDir.resolve("lib.kt")
        source.writeText("fun libraryFunction() = 42")
        val klib = compileToLibrary(source)

        val firstArchives = compileArchives(outputKind, klib, outputDir = buildDir.resolve("output1"))
        val secondArchives = compileArchives(outputKind, klib, outputDir = buildDir.resolve("output2"))

        assertEquals(firstArchives.keys, secondArchives.keys)
        for (path in firstArchives.keys) {
            val firstArchive = firstArchives.getValue(path)
            val secondArchive = secondArchives.getValue(path)
            assertContentEquals(firstArchive.readBytes(), secondArchive.readBytes(), "$firstArchive and $secondArchive differ")
        }
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
                dependencies = listOf(klib.asIncludedLibraryDependency()),
                expectedArtifact = TestCompilationArtifact.BinaryLibrary(outputDir.resolve("liblib.a")),
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
}

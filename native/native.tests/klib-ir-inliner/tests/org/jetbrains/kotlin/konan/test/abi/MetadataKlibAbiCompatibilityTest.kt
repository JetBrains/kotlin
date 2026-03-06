/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.abi

import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.buildDir
import org.jetbrains.kotlin.konan.test.blackbox.compileToLibrary
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CompilationToolException
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.library.KotlinAbiVersion.Companion.CURRENT
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Test that metadata klibs can be consumed even though their ABI version is not compatible with compiler version.
 */
class MetadataKlibAbiCompatibilityTest : AbstractNativeSimpleTest() {
    @Test
    fun testMetadataKlibWithIncompatibleAbiVersion() {
        val dir1 = buildDir.resolve("dir1").apply { createDirectory() }
        val sourceFile1 = dir1.resolve("source1.kt").apply { writeText("fun foo() = Unit") }
        val library1 = compileLibraryWithIncompatibleAbiVersion(sourceFile1, dir1, listOf("-Xmetadata-klib"))

        val dir2 = buildDir.resolve("dir2").apply { createDirectory() }
        val sourceFile2 = dir2.resolve("source2.kt").apply { writeText("fun bar() = Unit") }
        compileToLibrary(sourceFile2, dir2, library1)
    }

    @Test
    fun testKlibWithIncompatibleAbiVersion() {
        val dir1 = buildDir.resolve("dir1").apply { createDirectory() }
        val sourceFile1 = dir1.resolve("source1.kt").apply { writeText("fun foo() = Unit") }
        val library1 = compileLibraryWithIncompatibleAbiVersion(sourceFile1, dir1)

        val dir2 = buildDir.resolve("dir2").apply { createDirectory() }
        val sourceFile2 = dir2.resolve("source2.kt").apply { writeText("fun bar() = Unit") }
        try {
            compileToLibrary(sourceFile2, dir2, library1)
            assert(false) // compilation should fail
        } catch (e: CompilationToolException) {
            assert(e.reason.contains("warning: KLIB resolver: Skipping"))
            assert(e.reason.contains("error: KLIB resolver: Could not find"))
        }
    }

    private fun compileLibraryWithIncompatibleAbiVersion(
        sourceDir: File,
        outputDir: File,
        extraArgs: List<String> = listOf(),
    ): TestCompilationArtifact.KLIB {
        val incompatibleAbiVersion = CURRENT.copy(minor = CURRENT.minor + 1)
        val compilerArgs = TestCompilerArgs(extraArgs + "-Xklib-abi-version=$incompatibleAbiVersion")
        return compileToLibrary(sourceDir, outputDir, compilerArgs, emptyList())
    }
}

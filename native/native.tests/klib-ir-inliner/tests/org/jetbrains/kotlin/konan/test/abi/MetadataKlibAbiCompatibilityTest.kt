/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.abi

import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.asLibraryDependency
import org.jetbrains.kotlin.konan.test.blackbox.buildDir
import org.jetbrains.kotlin.konan.test.blackbox.compileToLibrary
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.CompilationToolException
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationArtifact
import org.jetbrains.kotlin.library.KotlinAbiVersion
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
        val library1 = compileKlib(sourceFile1, dir1, extraArgs = listOf(INCOMPATIBLE_ABI_VERSION_ARG, "-Xmetadata-klib"))

        val dir2 = buildDir.resolve("dir2").apply { createDirectory() }
        val sourceFile2 = dir2.resolve("source2.kt").apply { writeText("fun bar() = foo()") }
        compileKlib(sourceFile2, dir2, extraArgs = listOf("-Xmetadata-klib"), dependency = library1)
    }

    @Test
    fun testKlibWithIncompatibleAbiVersion() {
        val dir1 = buildDir.resolve("dir1").apply { createDirectory() }
        val sourceFile1 = dir1.resolve("source1.kt").apply { writeText("fun foo() = Unit") }
        val library1 = compileKlib(sourceFile1, dir1, extraArgs = listOf(INCOMPATIBLE_ABI_VERSION_ARG))

        val dir2 = buildDir.resolve("dir2").apply { createDirectory() }
        val sourceFile2 = dir2.resolve("source2.kt").apply { writeText("fun bar() = foo()") }
        try {
            compileKlib(sourceFile2, dir2, dependency = library1)
            assert(false) // compilation should fail
        } catch (e: CompilationToolException) {
            assert(e.reason.contains("KLIB loader: Incompatible ABI version"))
        }
    }

    private fun compileKlib(
        sourceDir: File,
        outputDir: File,
        extraArgs: List<String> = emptyList(),
        dependency: TestCompilationArtifact.KLIB? = null,
    ): TestCompilationArtifact.KLIB = compileToLibrary(
        sourcesDir = sourceDir,
        outputDir = outputDir,
        freeCompilerArgs = TestCompilerArgs(extraArgs),
        dependencies = listOfNotNull(dependency?.asLibraryDependency())
    )

    companion object {
        private val INCOMPATIBLE_ABI_VERSION_ARG: String = run {
            val incompatibleAbiVersion = KotlinAbiVersion.CURRENT.copy(minor = KotlinAbiVersion.CURRENT.minor + 1)
            "-Xklib-abi-version=$incompatibleAbiVersion"
        }
    }
}

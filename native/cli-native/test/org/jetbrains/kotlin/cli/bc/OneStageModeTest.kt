/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.bc

import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.contentRoots
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.useFir
import org.jetbrains.kotlin.konan.config.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createFile

class OneStageModeTest {
    @TempDir
    private lateinit var tempDir: Path

    @Test
    fun `one-stage compilation needs a binary output and sources`() {
        // A binary built from sources: the sources have to become a KLIB first.
        assertTrue(isOneStageCompilation(arguments(produce = "program", sources = listOf("main.kt"))))
        assertTrue(isOneStageCompilation(arguments(produce = "framework", sources = listOf("main.kt"))))
        assertTrue(isOneStageCompilation(arguments(produce = "dynamic", sources = listOf("main.kt"))))

        // Producing a KLIB *is* what the first stage does, so there is nothing to split.
        assertFalse(isOneStageCompilation(arguments(produce = "library", sources = listOf("main.kt"))))

        // No sources. This is the shape the Kotlin Gradle plugin's link task uses -- the project's own code
        // arrives as an already-compiled KLIB -- so Gradle builds never take the one-stage path.
        assertFalse(isOneStageCompilation(arguments(produce = "framework", sources = emptyList())))

        // Compiling from bitcode bypasses the frontend, so again there are no sources to compile.
        assertFalse(
            isOneStageCompilation(
                arguments(produce = "program", sources = listOf("main.kt"), fromBitcode = "main.bc")
            )
        )
    }

    @Test
    fun `intermediate KLIB is appended after every CLI library bucket`() {
        val intermediateKlib = tempDir.resolve("intermediate.klib").createFile()
        val configuration = configuration {
            // As assembled by NativeConfigurationPhase: `-library`, then `-Xinclude`, then `-Xadd-cache`.
            konanLibraries = listOf("regular.klib", "included.klib", "toCache.klib")
            konanIncludedLibraries = listOf("included.klib")
        }

        adjustConfigurationForSecondStage(configuration, intermediateKlib)

        val intermediatePath = intermediateKlib.absolutePathString()
        assertEquals(
            listOf("regular.klib", "included.klib", "toCache.klib", intermediatePath),
            configuration.konanLibraries,
            "the intermediate KLIB must come last, so that it is last in LoadedNativeKlibs.all"
        )
        assertEquals(
            listOf("included.klib", intermediatePath),
            configuration.konanIncludedLibraries,
            "it must also be marked as included, so that its declarations are exported"
        )
    }

    @Test
    fun `the second stage compiles no sources of its own`() {
        val intermediateKlib = tempDir.resolve("intermediate.klib").createFile()
        val configuration = configuration {
            addKotlinSourceRoot("main.kt")
            useFir = true
        }
        assertTrue(configuration.contentRoots.isNotEmpty())

        adjustConfigurationForSecondStage(configuration, intermediateKlib)

        // The sources were already compiled by the first stage; leaving them in would compile them twice.
        assertTrue(configuration.contentRoots.isEmpty(), "source roots must be dropped for the second stage")
        assertFalse(configuration.useFir, "there is no frontend to run in the second stage")
    }

    @OptIn(CompilerConfiguration.Internals::class)
    private fun configuration(init: CompilerConfiguration.() -> Unit): CompilerConfiguration =
        CompilerConfiguration().apply(init)

    private fun arguments(
        produce: String,
        sources: List<String>,
        fromBitcode: String? = null,
    ): K2NativeCompilerArguments = K2NativeCompilerArguments().apply {
        this.produce = produce
        this.freeArgs = sources
        this.compileFromBitcode = fromBitcode
    }
}

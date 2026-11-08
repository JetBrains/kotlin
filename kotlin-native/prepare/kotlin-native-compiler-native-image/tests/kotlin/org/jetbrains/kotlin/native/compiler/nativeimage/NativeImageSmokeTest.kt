/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.compiler.nativeimage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Runs the compiler native image through the opt-in in the `kotlinc-native` launcher (`KONAN_NATIVE_IMAGE`)
 * the way an edit-compile loop does: compile KLIBs, link a debug executable with incremental caches,
 * change a source file, and recompile and re-link incrementally.
 */
class NativeImageSmokeTest {
    private val nativeImage = File(System.getProperty("kotlin.native.image") ?: error("Property kotlin.native.image is not specified"))
    private val nativeHome = File(System.getProperty("kotlin.native.home") ?: error("Property kotlin.native.home is not specified"))
    private val kotlincNative = nativeHome.resolve("bin/kotlinc-native")

    @TempDir
    lateinit var workDir: File

    private class ProcessResult(val exitCode: Int, val output: String)

    private fun run(command: List<String>, environment: Map<String, String> = emptyMap()): ProcessResult {
        val process = ProcessBuilder(command)
            .directory(workDir)
            .redirectErrorStream(true)
            .apply { environment().putAll(environment) }
            .start()
        val output = process.inputStream.bufferedReader().readText()
        assertTrue(process.waitFor(10, TimeUnit.MINUTES), "Timed out: $command")
        return ProcessResult(process.exitValue(), output)
    }

    private fun konanc(vararg args: String, nativeImagePath: String = nativeImage.absolutePath): ProcessResult =
        run(listOf(kotlincNative.absolutePath, *args), mapOf("KONAN_NATIVE_IMAGE" to nativeImagePath))

    private fun konancSuccessfully(vararg args: String) {
        val result = konanc(*args)
        assertEquals(0, result.exitCode, "Compilation failed: ${args.joinToString(" ")}\n${result.output}")
    }

    private fun writeSources(greeting: String, factor: Int) {
        workDir.resolve("lib/lib.kt").apply { parentFile.mkdirs() }.writeText(
            """
            package lib
            data class Item(val id: Int, val name: String)
            fun compute(n: Int): Int = (0 until n).map { it * $factor }.filter { it % 2 == 0 }.sum()
            """.trimIndent()
        )
        workDir.resolve("app/main.kt").apply { parentFile.mkdirs() }.writeText(
            """
            package app
            import lib.*
            fun main() {
                val items = List(3) { Item(it, "item${'$'}it") }
                println("$greeting " + items.joinToString { it.name } + " " + compute(10))
            }
            """.trimIndent()
        )
    }

    private fun buildAndRun(): String {
        konancSuccessfully("-p", "library", "-g", "lib", "-o", "lib.klib", "-module-name", "lib")
        konancSuccessfully("-p", "library", "-g", "app", "-l", "lib.klib", "-o", "app.klib", "-module-name", "app")
        workDir.resolve("ic").mkdirs()
        konancSuccessfully(
            "-p", "program", "-g", "-Xinclude=app.klib", "-l", "lib.klib", "-e", "app.main",
            "-Xenable-incremental-compilation", "-Xic-cache-dir=ic", "-o", "app",
        )
        val result = run(listOf(workDir.resolve("app.kexe").absolutePath))
        assertEquals(0, result.exitCode, result.output)
        return result.output.trim()
    }

    @Test
    fun `compile and incrementally recompile`() {
        writeSources(greeting = "Hello", factor = 1)
        assertEquals("Hello item0, item1, item2 20", buildAndRun())

        // Both an included and a library file change: their per-file caches have to be rebuilt by the image.
        writeSources(greeting = "Bye", factor = 3)
        assertEquals("Bye item0, item1, item2 60", buildAndRun())
    }

    @Test
    fun `arguments not exercised during metadata tracing are supported`() {
        writeSources(greeting = "Hello", factor = 1)
        val perfReport = workDir.resolve("perf.txt")
        konancSuccessfully("-p", "library", "lib", "-o", "lib.klib", "-Xdump-perf=${perfReport.absolutePath}", "-Xreport-all-warnings")
        assertTrue(perfReport.readText().contains("ANALYZE"), perfReport.readText())
    }

    @Test
    fun `misconfigured opt-in is reported`() {
        val result = konanc("-version", nativeImagePath = workDir.resolve("missing-image").absolutePath)
        assertNotEquals(0, result.exitCode, result.output)
        assertTrue(result.output.contains("KONAN_NATIVE_IMAGE"), result.output)
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class NativeImageSmokeTest {
    @TempDir
    lateinit var workingDir: File

    private val runner = NativeImageCompilerRunner(System.getProperty("java.home"))

    @Test
    fun testSmoke() {
        val source = File("testData/projects/smoke/Smoke.kt").absoluteFile
        val outDir = File(workingDir, "out").apply { mkdirs() }

        val [exitCode, output] = runner.run(
            workingDir = workingDir,
            arguments = listOf(source.absolutePath, "-d", outDir.absolutePath),
            classpath = emptyList(),
        )

        assertEquals(0, exitCode, "compilation failed:\n$output")
    }

    @Test
    fun `test non-bundled plugin fails with an error`() {
        val source = File("testData/projects/smoke/Smoke.kt").absoluteFile
        val outDir = File(workingDir, "out").apply { mkdirs() }
        val unknownPluginJar = File(workingDir, "kotlin-foo-bar-compiler-plugin-1.0.jar")

        val [exitCode, output] = runner.run(
            workingDir = workingDir,
            arguments = listOf(
                source.absolutePath,
                "-d", outDir.absolutePath,
                "-Xcompiler-plugin=${unknownPluginJar.absolutePath}",
            ),
            classpath = emptyList(),
        )

        assertNotEquals(0, exitCode, "compilation unexpectedly succeeded with a non-bundled plugin:\n$output")
        assertTrue(
            "cannot be loaded by the native-image compiler" in output,
            "expected a clear 'not bundled' diagnostic, got:\n$output",
        )
    }
}

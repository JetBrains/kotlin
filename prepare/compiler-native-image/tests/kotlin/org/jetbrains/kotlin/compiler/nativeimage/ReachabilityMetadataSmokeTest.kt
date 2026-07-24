/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ReachabilityMetadataSmokeTest {
    @TempDir
    lateinit var workingDir: File

    private val runner = ReachabilityMetadataGeneratingCompilerRunner(System.getProperty("java.home"))

    @Test
    fun testRegenerateReachabilityMetadata() {
        val source = File("testData/projects/smoke/Smoke.kt").absoluteFile
        val outDir = File(workingDir, "out").apply { mkdirs() }

        val [exitCode, output] = runner.run(
            workingDir = workingDir,
            arguments = listOf(source.absolutePath, "-d", outDir.absolutePath),
            classpath = emptyList(),
        )

        assertEquals(0, exitCode, "compilation failed:\n$output")
    }
}

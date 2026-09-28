/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

abstract class AbstractCompilerSmokeTest(private val runner: CompilerRunner) {
    @TempDir
    lateinit var workingDir: File

    @Test
    fun testSmoke() {
        val source = ForTestCompileRuntime.transformTestDataPath("testData/projects/smoke/Smoke.kt")
        val outDir = File(workingDir, "out").apply { mkdirs() }

        val result = runner.run(
            workingDir = workingDir,
            arguments = listOf(source.absolutePath, "-d", outDir.absolutePath),
        )

        assertEquals(0, result.exitCode, "compilation failed:\n${result.output}")
    }
}

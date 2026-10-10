/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Compiles a "Hello World" to WebAssembly with the Kotlin/Wasm compiler running
 * as a GraalVM web image and verifies that the compilation succeeds.
 */
class WebImageSmokeTest {
    private val runner = WebImageCompilerRunner()

    @TempDir
    lateinit var workingDir: File

    @Test
    fun testSmoke() {
        val source = ForTestCompileRuntime.transformTestDataPath("testData/projects/smokeWasm/SmokeWasm.kt")
        val outDir = File(workingDir, "out").apply { mkdirs() }

        val result = runner.run(
            workingDir = workingDir,
            arguments = listOf(
                source.absolutePath,
                "-libraries", ForTestCompileRuntime.stdlibWasmJsForTests().absolutePath,
                "-ir-output-dir", outDir.absolutePath,
                "-ir-output-name", "smoke",
                "-Xir-produce-js",
            ),
        )

        assertEquals(0, result.exitCode, "compilation failed:\n${result.output}")
        assertTrue(
            outDir.walkTopDown().any { it.extension == "wasm" },
            "no WebAssembly module was produced in ${outDir.absolutePath}:\n${result.output}",
        )
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.junit.jupiter.api.Assertions.assertEquals
import java.io.File

abstract class AbstractNativeImageScriptingTest : AbstractCompilerTest(NativeImageCompilerRunner()) {
    override fun buildCompilerArgs(
        testFile: File,
        outDir: File,
        directives: RegisteredDirectives,
        withFullJdk: Boolean,
    ): List<String> = super.buildCompilerArgs(testFile, outDir, directives, withFullJdk) + listOf("-script")

    override fun prepareSourceFile(source: String): File =
        File(workingDir, "script.kts").apply { writeText(source) }

    override fun checkCompilationResult(result: CompilerInvocationResult, outDir: File, source: String, withReflect: Boolean) {
        assertEquals(0, result.exitCode, "compilation failed:\n${result.output}")
        assertEquals("OK", result.stdout.trim(), "script output != 'OK'")
    }
}

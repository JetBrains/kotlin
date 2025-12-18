/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test

import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Files
import kotlin.io.path.pathString
import kotlin.io.path.writeText

// TODO super ugly, find better way to do this (https://jetbrains.slack.com/archives/C0288G57R/p1766139404407909)
open class TmpDirHelper(protected val tempDir: File) : AbstractFirWasmJsTest(
    pathToTestDir = tempDir.path,
    // TODO this is problematic, because it spams that dir once per test invocation. Change this before merging
    testGroupOutputDirPrefix = "codegen/wasmDirectives"
)

class WasmDirectivesWasmIrTest : TmpDirHelper(KtTestUtil.tmpDir("wasmDirectivesTest")) {
    @Test
    fun testWasmIrDirectivesAreExecuted() {

        val testCodeAndExpectation = mapOf(
            // Check that directives don't break a trivial test
            """
            // WASM_COUNT_INSTRUCTION_IN_SCOPE: instruction=if scope_function=box count=0
            fun box(): String = "OK"
            """.trimIndent() to true, // true means expect success

            // Use a directive that is guaranteed to fail if executed properly (expects 1000 ifs in a trivial function)
            """
            // WASM_COUNT_INSTRUCTION_IN_SCOPE: instruction=if scope_function=box count=1000
        
            fun box(): String {
                return "OK"
            }
            """.trimIndent() to false
        )

        // Create temporary directory; iterate tests; clean up
        for ((testCode, expectedSuccess) in testCodeAndExpectation) {
            val testFile = Files.createTempFile(tempDir.toPath(), "test", ".kt")
            testFile.writeText(testCode)

            if (expectedSuccess) {
                runTest(testFile.pathString)
            } else {
                assertThrows<AssertionError> { runTest(testFile.pathString) }
            }
        }


    }
}
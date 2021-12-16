/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.daemon

import org.jetbrains.kotlin.cli.common.ExitCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.nio.file.Files

@RunWith(Parameterized::class)
class CompilerTest(
    @Suppress("UNUSED_PARAMETER") name: String,
    private val daemonCompiler: DaemonCompiler
) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Array<Array<Any>> = arrayOf(
            arrayOf(BasicDaemonCompiler::class.java.name, BasicDaemonCompiler),
            arrayOf(IncrementalDaemonCompiler::class.java.name, IncrementalDaemonCompiler)
        )
    }

    @Test
    fun `files are compiled successfully`() {
        val sourceDir = Files.createTempDirectory("source")
        val inputFile = File(sourceDir.toFile(), "Input.kt").apply {
            writeText(
                """
            fun main() {
            }
        """.trimIndent()
            )
        }
        val inputFile2 = File(sourceDir.toFile(), "Input2.kt").apply {
            writeText(
                """
            fun main2() {
            }
        """.trimIndent()
            )
        }

        run {
            val outputDir = Files.createTempDirectory("output")
            assertEquals(
                ExitCode.OK, daemonCompiler.compile(
                    arrayOf(
                        "-d", outputDir.toAbsolutePath().toString(),
                        inputFile.absolutePath
                    ), DaemonCompilerSettings(null)
                )
            )
            assertTrue(File(outputDir.toFile(), "InputKt.class").exists())
        }

        // Verify multiple input files
        run {
            val outputDir = Files.createTempDirectory("output")
            assertEquals(
                ExitCode.OK, daemonCompiler.compile(
                    arrayOf(
                        "-d", outputDir.toAbsolutePath().toString(),
                        inputFile.absolutePath, inputFile2.absolutePath
                    ), DaemonCompilerSettings(null)
                )
            )
            assertTrue(File(outputDir.toFile(), "InputKt.class").exists())
            assertTrue(File(outputDir.toFile(), "Input2Kt.class").exists())
        }
    }

    @Test
    fun `compilation fails with syntax errors`() {
        val sourceDir = Files.createTempDirectory("source").toFile()
        val outputDir = Files.createTempDirectory("output")
        val inputFile = File(sourceDir, "Input.kt")
        inputFile.writeText(
            """
            Invalid code
        """.trimIndent()
        )

        assertEquals(
            ExitCode.COMPILATION_ERROR, daemonCompiler.compile(
                arrayOf(
                    "-d", outputDir.toAbsolutePath().toString(),
                    inputFile.absolutePath
                ), DaemonCompilerSettings(null)
            )
        )
    }
}
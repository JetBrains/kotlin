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
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.io.PipedReader
import java.io.PipedWriter
import java.io.PrintWriter
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class DaemonProtocolTest {

    @Test
    fun `verify daemon compiler commands`() {
        val compileCalls = mutableListOf<String>()
        val outputWriter = PipedWriter()
        val daemonOutput = ByteArrayOutputStream()
        val countDownLatch = CountDownLatch(2)
        val pipedReader = PipedReader(outputWriter)

        val output = PrintWriter(outputWriter)

        val testDaemon = object : DaemonCompiler {
            override fun compile(
                args: Array<String>,
                daemonCompilerSettings: DaemonCompilerSettings
            ): ExitCode {
                val pluginParameter = daemonCompilerSettings.composePluginPath
                    ?.let { "-plugin $it " } ?: ""
                val otherArgs = args.joinToString(" ")
                try {
                    compileCalls.add("kotlinc $pluginParameter$otherArgs")
                    return ExitCode.OK
                } finally {
                    countDownLatch.countDown()
                }
            }
        }

        thread {
            startInputLoop(
                testDaemon,
                DaemonCompilerSettings("pluginPath1"),
                pipedReader, OutputStreamWriter(daemonOutput)
            )
        }
        output.println(
            """
            -a 1
            -b
            Test.kt
            done
        """.trimIndent()
        )
        output.flush()

        countDownLatch.await()
        assertEquals(
            """
            kotlinc -version
            kotlinc -plugin pluginPath1 -a 1 -b Test.kt
        """.trimIndent(), compileCalls.joinToString("\n")
        )
    }
}
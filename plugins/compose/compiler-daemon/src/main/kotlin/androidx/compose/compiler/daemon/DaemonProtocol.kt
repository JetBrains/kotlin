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

import java.io.BufferedReader
import java.io.PrintWriter
import java.io.Reader
import java.io.Writer
import kotlin.system.exitProcess

/**
 * Starts the input loop listening for commands to be sent to the [daemonCompiler] using the given
 * [daemonSettings]. This method will listen to input commands via [inputStream] and sending the
 * output to [outputStream].
 */
fun startInputLoop(
    daemonCompiler: DaemonCompiler,
    daemonSettings: DaemonCompilerSettings,
    inputReader: Reader,
    outputWriter: Writer
) {
    val input = BufferedReader(inputReader)
    val output = PrintWriter(outputWriter)
    // Show the version and trigger the loading of all the compiler classes
    daemonCompiler.compile(arrayOf("-version"))
    while (true) {
        val commandLineBuilder = mutableListOf<String>()
        while (commandLineBuilder.lastOrNull() != "done") {
            output.print(">")
            val line = input.readLine()!!
            output.println(line)
            if (line == "quit") exitProcess(1)
            commandLineBuilder.add(line)
        }
        val exitCode = daemonCompiler.compile(
            commandLineBuilder.dropLast(1).toTypedArray(), daemonSettings
        )
        output.println("RESULT ${exitCode.code}")
        output.flush()
    }
}
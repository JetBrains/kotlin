/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.konan.exec

import java.lang.ProcessBuilder
import java.lang.ProcessBuilder.Redirect

fun executeCommand(command: List<String>) =
    executeCommand(*command.toTypedArray())

fun executeCommand(vararg command: String): Int {
    // TODO: need a verbose logger here.

    val builder = ProcessBuilder(command.asList())

    builder.redirectOutput(Redirect.INHERIT)
    builder.redirectInput(Redirect.INHERIT)
    builder.redirectError(Redirect.INHERIT)

    val process = builder.start()
    val exitCode = process.waitFor()
    return exitCode
}

fun runTool(command: List<String>) =
    runTool(*command.toTypedArray())

fun runTool(vararg command: String) {
    val code = executeCommand(*command)
    if (code != 0) error("The ${command[0]} command returned non-zero exit code: $code.")
}


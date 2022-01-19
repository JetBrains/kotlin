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

import java.io.File
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import java.io.PrintWriter
import java.lang.IllegalArgumentException
import java.net.URLDecoder
import java.nio.charset.Charset

private data class CliOptions(
    val disableEmbeddedPlugin: Boolean = false,
    val useIncrementalCompiler: Boolean = false,
    val studioVersion: String? = null
)

private fun parseArgumentWithOption(args: Array<String>, argumentName: String): String? {
    val argumentIndex = args.indexOf(argumentName)
    return if (argumentIndex != -1) args[argumentIndex + 1] else null
}

private fun parseCliOptions(args: Array<String>): CliOptions {
    return CliOptions(
        // Disables the use of the plugin embedded with the jar
        disableEmbeddedPlugin = args.contains("-disableEmbedded"),
        useIncrementalCompiler = args.contains("-incremental"),
        studioVersion = parseArgumentWithOption(args, "-studio")
    )
}

fun main(args: Array<String>) {
    setIdeaIoUseFallback()

    val cliOptions = parseCliOptions(args)
    println(cliOptions)

    val jarPath = if (cliOptions.disableEmbeddedPlugin)
        null
    else
        URLDecoder.decode(
            object {}.javaClass.protectionDomain.codeSource.location.path,
            Charsets.UTF_8
        )
    jarPath?.let {
        if (!File(it).exists()) {
            throw IllegalArgumentException("Compose plugin not found at $it")
        }
        println("Using embedded plugin with path $it")
    } ?: println("No embedded plugin")

    val compiler: DaemonCompiler = if (cliOptions.useIncrementalCompiler) {
        println("Using IncrementalDaemonCompiler")
        IncrementalDaemonCompiler
    } else {
        println("Using BasicDaemonCompiler")
        BasicDaemonCompiler
    }
    // Show the version and trigger the loading of all the compiler classes
    compiler.compile(arrayOf("-version"))
    val settings = DaemonCompilerSettings(jarPath)
    startInputLoop(compiler,
        settings,
        System.`in`.bufferedReader(Charset.defaultCharset()),
        PrintWriter(System.out)
    )
}
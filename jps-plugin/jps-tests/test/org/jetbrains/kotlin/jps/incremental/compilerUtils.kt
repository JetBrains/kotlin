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

package org.jetbrains.kotlin.jps.incremental

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.compilerRunner.*
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.jps.build.KotlinBuilder
import org.jetbrains.kotlin.utils.PathUtil
import java.io.*

fun createTestingCompilerEnvironment(
    messageCollector: MessageCollector,
    outputItemsCollector: OutputItemsCollectorImpl,
    services: Services
): JpsCompilerEnvironment {
    val paths = PathUtil.kotlinPathsForDistDirectory

    val wrappedMessageCollector = MessageCollectorToOutputItemsCollectorAdapter(messageCollector, outputItemsCollector)
    return JpsCompilerEnvironment(paths, services, KotlinBuilder.classesToLoadByParent, wrappedMessageCollector, outputItemsCollector)
}

fun runJSCompiler(args: K2JSCompilerArguments, env: JpsCompilerEnvironment): ExitCode? {
    val argsArray = ArgumentUtils.convertArgumentsToStringList(args).toTypedArray()

    val stream = ByteArrayOutputStream()
    val out = PrintStream(stream)
    val exitCode = CompilerRunnerUtil.invokeExecMethod(K2JSCompiler::class.java.name, argsArray, env, out)
    val reader = BufferedReader(StringReader(stream.toString()))
    CompilerOutputParser.parseCompilerMessagesFromReader(env.messageCollector, reader, env.outputItemsCollector)
    return exitCode as? ExitCode
}
/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.test

import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmCompilerFromEnvironment
import org.jetbrains.kotlin.scripting.compiler.plugin.toCompilerMessageSeverity
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.utils.tryConstructClassFromStringArgs
import org.junit.Assert
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.reflect.KClass
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.onSuccess
import kotlin.script.experimental.api.valueOr
import kotlin.script.experimental.api.with
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm

internal const val NUM_4_LINE = "num: 4"

internal const val FIB_SCRIPT_OUTPUT_TAIL =
    """
fib(1)=1
fib(0)=1
fib(2)=2
fib(1)=1
fib(3)=3
fib(1)=1
fib(0)=1
fib(2)=2
fib(4)=5
"""

internal fun captureOut(body: () -> Unit): String {
    val outStream = ByteArrayOutputStream()
    val prevOut = System.out
    System.setOut(PrintStream(outStream))
    try {
        body()
    } finally {
        System.out.flush()
        System.setOut(prevOut)
    }
    return outStream.toString()
}

internal fun String.linesSplitTrim() =
    split('\n', '\r').map(String::trim).filter(String::isNotBlank)

internal fun assertEqualsTrimmed(expected: String, actual: String) =
    Assert.assertEquals(expected.linesSplitTrim(), actual.linesSplitTrim())

// TODO: rewrite tests to avoid emulated old behavior
internal fun compileScript(
    script: SourceCode,
    environment: KotlinCoreEnvironment,
    parentClassLoader: ClassLoader?
): Pair<KClass<*>?, ExitCode> {
    val scriptCompiler = ScriptJvmCompilerFromEnvironment(environment)
    val messageCollector = environment.configuration[CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY]!!
    val scriptDefinition = ScriptDefinitionProvider.getInstance(environment.project)!!.findDefinition(script)!!

    val scriptCompilationConfiguration = scriptDefinition.compilationConfiguration.with {
        jvm {
            dependenciesFromCurrentContext(wholeClasspath = true)
        }
    }
    val compiledScript = scriptCompiler.compile(script, scriptCompilationConfiguration).onSuccess {
        runBlocking {
            it.getClass(scriptDefinition.evaluationConfiguration.with {
                jvm {
                    baseClassLoader(parentClassLoader)
                }
            })
        }
    }.valueOr {
        for (report in it.reports) {
            messageCollector.report(report.severity.toCompilerMessageSeverity(), report.render(withSeverity = false))
        }
        return null to ExitCode.COMPILATION_ERROR
    }
    return compiledScript to ExitCode.OK
}

// TODO: rewrite tests to avoid emulated old behavior
internal fun compileAndExecuteScript(
    script: SourceCode,
    environment: KotlinCoreEnvironment,
    parentClassLoader: ClassLoader?,
    scriptArgs: List<String>
): ExitCode {
    val (compiled, code) = compileScript(script, environment, parentClassLoader)

    if (compiled == null || code != ExitCode.OK) return code

    return if (tryConstructClassFromStringArgs(compiled.java, scriptArgs) != null) ExitCode.OK else ExitCode.INTERNAL_ERROR
}

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readLines
import kotlin.io.path.writeText
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.getScriptingClass
import kotlin.script.experimental.jvm.JvmGetScriptingClass
import kotlin.test.assertEquals

val pathToExplainingFile: Path by lazy {
    createTempDirectory("explainTemp").resolve("explain.txt")
}

@KotlinScript(
    displayName = "FakeExplanationScript",
    fileExtension = "kts",
    compilationConfiguration = KotlinExplainCompilationConfiguration::class,
    hostConfiguration = KotlinExplainHostConfiguration::class,
    evaluationConfiguration = KotlinExplainEvaluationConfiguration::class,
)
abstract class KotlinExplainScript(vararg args: String)

private class KotlinExplainEvaluationConfiguration : ScriptEvaluationConfiguration(
    {
        refineConfigurationBeforeEvaluate { (_, config, _) ->
            config.with {
                val map = mutableMapOf<String, Any?>()
                constructorArgs(map)
                scriptExecutionWrapper<Any?> {
                    try {
                        it()
                    } finally {
                        pathToExplainingFile
                            .writeText(map.entries.joinToString(separator = "\n") { entry -> "${entry.key} = ${entry.value}" })
                    }
                }
            }.asSuccess()
        }
    }
)

private class KotlinExplainCompilationConfiguration : ScriptCompilationConfiguration(
    {
        displayName("Kotlin Scratch")
        explainField($$$"$$explain")
    })

private class KotlinExplainHostConfiguration : ScriptingHostConfiguration(
    {
        getScriptingClass(JvmGetScriptingClass())
    })

private val additionalClasspath = System.getProperty("kotlin.test.script.classpath")
private val powerAssertJar = File("dist/kotlinc/lib/power-assert-compiler-plugin.jar").absolutePath

class ScriptingWithExplanationCompilerTest {
    companion object {
        const val TEST_DATA_DIR = "plugins/scripting/scripting-compiler/testData"
        const val TEST_SCRIPT_TO_EXPLAIN_1 = "$TEST_DATA_DIR/compiler/explain/simpleExplain.kts"
        const val TEST_SCRIPT_TO_EXPLAIN_2 = "$TEST_DATA_DIR/compiler/explain/explainWithNonLastExpr.kts"
        const val TEST_SCRIPT_TO_EXPLAIN_3 = "$TEST_DATA_DIR/compiler/explain/ifStatement.kts"
    }

    init {
        setIdeaIoUseFallback()
    }

    @Test
    fun scriptShouldFlushExplainInformationAfterEvaluation() {
        withTempDir { _ ->
            val (out, err, ret) = captureOutErrRet {
                runScriptWithExplain(TEST_SCRIPT_TO_EXPLAIN_1)
            }
            val lines = pathToExplainingFile.readLines()
            assertEquals(
                listOf(
                    "a(8, 9) = 1",
                    "b(18, 19) = 1",
                    "b(18, 23) = 6",
                    "result(38, 39) = 1",
                    "result(42, 43) = 6",
                    "result(38, 43) = 7",
                    "(45, 60) = true",
                    "(82, 83) = 1",
                    "(78, 83) = 4",
                    "(74, 83) = kotlin.Unit",
                    $$$"$$result(85, 87) = 42",
                ), lines
            )
        }
    }

    @Test
    fun testScriptExplainShouldCoverNonLastExpressions() {
        withTempDir { _ ->
            val (out, err, ret) = captureOutErrRet {
                runScriptWithExplain(TEST_SCRIPT_TO_EXPLAIN_2)
            }
            val lines = pathToExplainingFile.readLines()
            assertEquals(
                listOf(
                    "a(8, 9) = 7",
                    "(11, 16) = 42",
                    $$$"$$result(18, 24) = 6"
                ), lines
            )
        }
    }

    @Test
    fun testScriptExplainShouldCoverIfExpression() {
        withTempDir { _ ->
            val (out, err, ret) = captureOutErrRet {
                runScriptWithExplain(TEST_SCRIPT_TO_EXPLAIN_3)
            }
            val lines = pathToExplainingFile.readLines()
            assertEquals(
                listOf(
                    $$$"$$result(69, 74) = false"
                ), lines
            )
        }
    }
}

private fun runScriptWithExplain(scriptPath: String): ExitCode = CLICompiler.doMainNoExit(
    K2JVMCompiler(),
    arrayOf(
        "-P",
        "plugin:kotlin.scripting:disable-script-definitions-autoloading=true",
        "-P",
        "plugin:kotlin.scripting:disable-standard-script=true",
        "-P",
        "plugin:kotlin.scripting:enable-script-explanation=true",
        "-Xplugin=$powerAssertJar",
        "-P",
        "plugin:kotlin.scripting:script-templates=${KotlinExplainScript::class.java.name}",
        K2JVMCompilerArguments::classpath.cliArgument, additionalClasspath,
        "-script",
        scriptPath,
    )
)
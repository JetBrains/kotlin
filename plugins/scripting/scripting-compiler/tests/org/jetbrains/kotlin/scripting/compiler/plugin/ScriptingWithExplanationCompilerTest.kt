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
import org.jetbrains.kotlin.scripting.definitions.getEnvironment
import org.jetbrains.kotlin.test.util.JUnit4Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.getScriptingClass
import kotlin.script.experimental.jvm.JvmGetScriptingClass
import kotlin.test.assertEquals

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
                val explainFilePath = get(hostConfiguration)!!.get(ScriptingHostConfiguration.getEnvironment)!!.invoke()!!.get("explainFile") as String
                val map = mutableMapOf<String, Any?>()
                constructorArgs(map)
                scriptExecutionWrapper<Any?> {
                    try {
                        it()
                    } finally {
                        File(explainFilePath)
                            .writeText(map.entries.joinToString(separator = "\n") { entry -> "${entry.key} = ${entry.value}" })
                    }
                }
            }.asSuccess()
        }
    }
)

private class KotlinExplainCompilationConfiguration() : ScriptCompilationConfiguration(
    {
        displayName("Kotlin Scratch")
        explainField("\$\$explain")
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
    }

    init {
        setIdeaIoUseFallback()
    }

    @Test
    fun scriptShouldFlushExplainInformationAfterEvaluation() {
        explainAndCheck(
            "${TEST_DATA_DIR}/compiler/explain/simpleExplain.kts",
            expectedExplanations = listOf(
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
                "\$\$result(85, 87) = 42",
            )
        )
    }

    @Test
    fun testScriptExplainShouldCoverNonLastExpressions() {
        explainAndCheck(
            "${TEST_DATA_DIR}/compiler/explain/explainWithNonLastExpr.kts",
            expectedExplanations = listOf(
                "a(8, 9) = 7", "(11, 16) = 42", "\$\$result(18, 24) = 6"
            )
        )
    }

    @Test
    fun testScriptExplainShouldCoverBodyOfTheNonExhaustiveIf() {
        explainAndCheck(
            "${TEST_DATA_DIR}/compiler/explain/explainWithNonExhaustiveIf.kts",
            expectedExplanations = listOf(
                "(17, 19) = 42",
                "(24, 37) = kotlin.Unit",
                "(42, 44) = 44",
                "(11, 46) = kotlin.Unit",
                "\$\$result(48, 50) = 43"
            ),
            expectedOut = listOf("43", "43")
        )
    }

    @Test
    fun testScriptExplainShouldSkipBodyOfTheDeadNonExhaustiveIf() {
        explainAndCheck(
            "${TEST_DATA_DIR}/compiler/explain/explainWithDeadNonExhaustiveIf.kts",
            // Unexpected results - then body should be skipped in the explanation and in the output
            expectedExplanations = listOf(
                "(18, 20) = 42",
                "(25, 38) = kotlin.Unit",
                "(12, 40) = kotlin.Unit",
                "\$\$result(42, 44) = 43"
            ),
            expectedOut = listOf("43", "43")
        )
    }

    @Test
    fun testScriptExplainShouldSkipBodyOfTheDeadNonExhaustiveIf2() {
        // Unexpected results - second then body should be skipped in the explanation and in the output
        explainAndCheck(
            "${TEST_DATA_DIR}/compiler/explain/explainWithDeadNonExhaustiveIf2.kts",
            expectedExplanations = listOf(
                "(63, 65) = 44", "(70, 83) = kotlin.Unit", "(57, 85) = kotlin.Unit", "\$\$result(87, 89) = 46"
            ),
            expectedOut = listOf("45", "46")
        )
    }
}

private fun explainAndCheck(scriptPath: String, expectedExplanations: List<String>, expectedExitCode: ExitCode = ExitCode.OK, expectedOut: List<String>? = null) {
    withTempFile { explainFile ->
        val (out, err, ret) = captureOutErrRet {
            runScriptWithExplain(scriptPath, explainFile.absolutePath)
        }
        assertEquals(expectedExitCode, ret) { "Expected exit code $expectedExitCode, actual $ret\n$err" }
        if (expectedOut != null) {
            assertEquals(expectedOut, out.trim().lines())
        }
        val lines = explainFile.readLines()
        assertEquals(expectedExplanations, lines)
    }
}

private fun runScriptWithExplain(scriptPath: String, explainFilePath: String): ExitCode = CLICompiler.doMainNoExit(
    K2JVMCompiler(),
    arrayOf(
        "-P",
        "plugin:kotlin.scripting:disable-script-definitions-autoloading=true",
        "-P",
        "plugin:kotlin.scripting:disable-standard-script=true",
        "-P",
        "plugin:kotlin.scripting:enable-script-explanation=true",
        "-P",
        "plugin:kotlin.scripting:script-resolver-environment=explainFile=\"$explainFilePath\"",
        "-Xplugin=$powerAssertJar",
        "-P",
        "plugin:kotlin.scripting:script-templates=${KotlinExplainScript::class.java.name}",
        K2JVMCompilerArguments::classpath.cliArgument, additionalClasspath,
        "-script",
        scriptPath,
    )
)

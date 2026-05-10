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
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.scripting.definitions.getEnvironment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.getScriptingClass
import kotlin.script.experimental.jvm.JvmGetScriptingClass

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
        explainField($$$"$$explain")
    })

private class KotlinExplainHostConfiguration : ScriptingHostConfiguration(
    {
        getScriptingClass(JvmGetScriptingClass())
    })

private val additionalClasspath = System.getProperty("kotlin.test.script.classpath")
private val powerAssertJar = ForTestCompileRuntime.getFileFromProperty("kotlin.power.assert.compiler.plugin.jar").absolutePath

class ScriptingWithExplanationCompilerTest {
    companion object {
        const val TEST_DATA_DIR = "plugins/scripting/scripting-compiler/testData/compiler/explain/"
    }

    init {
        setIdeaIoUseFallback()
    }

    @Test
    fun scriptShouldFlushExplainInformationAfterEvaluation() {
        runScriptAndValidateExplain("$TEST_DATA_DIR/simpleExplain.kts")
    }

    @Test
    fun testUnaryOperator() {
        runScriptAndValidateExplain("$TEST_DATA_DIR/unaryOperator.kts")
    }

    @Test
    fun testScriptExplainShouldCoverNonLastExpressions() {
        runScriptAndValidateExplain("$TEST_DATA_DIR/explainWithNonLastExpr.kts")
    }

    @Test
    fun testScriptExplainShouldCoverBodyOfTheExhaustiveIf() {
        runScriptAndValidateExplain("$TEST_DATA_DIR/explainWithExhaustiveIf.kts")
    }

    @Test
    fun testScriptExplainShouldCoverBodyOfTheNonExhaustiveIf() {
        runScriptAndValidateExplain("$TEST_DATA_DIR/explainWithNonExhaustiveIf.kts")
    }

    @Test
    fun testScriptExplainShouldSkipBodyOfTheDeadNonExhaustiveIf() {
        runScriptAndValidateExplain("$TEST_DATA_DIR/explainWithDeadNonExhaustiveIf.kts")
    }

    @Test
    fun testScriptExplainShouldSkipBodyOfTheDeadNonExhaustiveIf2() {
        // Unexpected results - second then body should be skipped in the explanation and in the output
        runScriptAndValidateExplain("$TEST_DATA_DIR/explainWithDeadNonExhaustiveIf2.kts")
    }

    @Test
    fun testScriptExplainShouldHandleLoops() {
        runScriptAndValidateExplain("$TEST_DATA_DIR/explainWithLoops.kts")
    }

    @Test
    fun testScriptExplainWithReducedList() {
        runScriptAndValidateExplain("$TEST_DATA_DIR/explainWithReducedList.kts")
    }

    @Test // KT-85102
    fun testScriptExplainWithForLoop() {
        runScriptAndValidateExplain("$TEST_DATA_DIR/forLoop.kts")
    }

    @Test // KT-85103
    fun testScriptExplainDestructuringDeclarations() {
        runScriptAndValidateExplain("$TEST_DATA_DIR/destructuringDecls.kts", expectedExitCode = ExitCode.OK)
    }

    @Test // KT-85105
    fun testScriptExplainWithObjectLiteral() {
        runScriptAndValidateExplain("$TEST_DATA_DIR/objectLiteral.kts", expectedExitCode = ExitCode.OK)
    }
}

private val hexAddressRegex = Regex("@[0-9a-fA-F]+")

private val updateTestData = System.getProperty("kotlin.test.update.test.data") == "true"

private fun runScriptAndValidateExplain(
    scriptPath: String,
    expectedExitCode: ExitCode = ExitCode.OK,
) {
    val scriptFile = ForTestCompileRuntime.transformTestDataPath(scriptPath)
    val baseName = scriptFile.nameWithoutExtension
    val dir = scriptFile.parentFile
    val explainExpectedFile = dir.resolve("$baseName.explain")

    withTempFile { tempExplainFile ->
        val (out, err, ret) = captureOutErrRet {
            runScriptWithExplain(scriptFile.path, tempExplainFile.absolutePath)
        }
        assertEquals(expectedExitCode, ret) { "Expected exit code $expectedExitCode, actual $ret\n$err" }

        val actualExplainLines = tempExplainFile.readLines().map { it.replace(hexAddressRegex, "@") }

        if (updateTestData) {
            explainExpectedFile.writeText(actualExplainLines.joinToString("\n"))
        } else {
            assertEquals(explainExpectedFile.readLines(), actualExplainLines)
        }
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

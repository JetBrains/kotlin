/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin

import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.readLines
import kotlin.io.path.writeText
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.getScriptingClass
import kotlin.script.experimental.jvm.JvmGetScriptingClass
import kotlin.test.Test
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

private class KotlinExplainCompilationConfiguration() : ScriptCompilationConfiguration(
    {
        displayName("Kotlin Scratch")
        explainField("\$\$explain")
    })

private class KotlinExplainHostConfiguration : ScriptingHostConfiguration(
    {
        getScriptingClass(JvmGetScriptingClass())
    })


class ScriptingWithExplanationCompilerTest {
    companion object {
        const val TEST_DATA_DIR = "plugins/scripting/scripting-compiler/testData"
        const val TEST_SCRIPT_TO_EXPLAIN = "$TEST_DATA_DIR/compiler/explain/simpleExplain.kts"
    }

    init {
        setIdeaIoUseFallback()
    }

    @Test
    fun scriptShouldFlushExplainInformationAfterEvaluation() {
        val additionalClasspath = System.getProperty("kotlin.test.script.classpath")
        val powerAssertJar = File("dist/kotlinc/lib/power-assert-compiler-plugin.jar").absolutePath
        withTempDir { _ ->
            captureOutErrRet {
                CLICompiler.doMainNoExit(
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
                        TEST_SCRIPT_TO_EXPLAIN,
                    )
                )
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
                ), lines
            )
        }
    }
}
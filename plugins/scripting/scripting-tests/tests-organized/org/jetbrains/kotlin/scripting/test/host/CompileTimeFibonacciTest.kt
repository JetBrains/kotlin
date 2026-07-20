/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test.host

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.CoreEnvironmentDeprecation
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.compiler.plugin.getCompilerExtensions
import org.jetbrains.kotlin.config.useFir
import org.jetbrains.kotlin.script.loadScriptingPlugin
import org.jetbrains.kotlin.scripting.compiler.plugin.TestDisposable
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmCompilerFromEnvironment
import org.jetbrains.kotlin.scripting.compiler.plugin.updateWithBaseCompilerArguments
import org.jetbrains.kotlin.scripting.compiler.test.CompileTimeFibonacci
import org.jetbrains.kotlin.scripting.compiler.test.captureOut
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinitionProvider
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.testFramework.RunAll
import org.junit.jupiter.api.Disabled
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.dependenciesFromCurrentContext
import kotlin.script.experimental.jvm.jvm
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals


private const val testDataPath = "plugins/scripting/scripting-tests/testData/host/compiler/compileTimeFibonacci"

class CompileTimeFibonacciTest {
    private val testRootDisposable: Disposable = TestDisposable("${CompileTimeFibonacciTest::class.simpleName}.testRootDisposable")

    @AfterTest
    fun tearDown() {
        RunAll(
            { Disposer.dispose(testRootDisposable) },
        )
    }

    @Test
    fun testFibonacciWithSupportedNumbersImplementsTheCorrectConstants() {
        val outputLines = runScript("supported.fib.kts")
            .valueOr { failure ->
                val message = failure.reports.joinToString("\n") { it.message }
                kotlin.test.fail("supported.fib.kts was expected to succeed:\n\n${message}")
            }
            .lines()
            .filter { it.isNotBlank() }

        assertEquals(4, outputLines.count())
        assertEquals("fib(1)=1", outputLines[0])
        assertEquals("fib(2)=1", outputLines[1])
        assertEquals("fib(3)=2", outputLines[2])
        assertEquals("fib(4)=3", outputLines[3])
    }

    // This tests if the annotations delivered with the correct location
    // and that scripts can return error messages at the location of the annotation
    @Test
    fun testFibonacciWithUnsupportedNumbersEmitsErrorAtLocation() {
        when (val result = runScript("unsupported.fib.kts")) {
            is ResultWithDiagnostics.Success ->
                kotlin.test.fail("supported.fib.kts was expected to fail with a compiler error from refinement")

            is ResultWithDiagnostics.Failure -> {
                val error = result.reports.first()

                val expectedFile =
                    ForTestCompileRuntime.transformTestDataPath("plugins/scripting/scripting-tests/testData/host/compiler/compileTimeFibonacci/unsupported.fib.kts")
                val expectedErrorMessage = """
                    ($expectedFile:3:1) Fibonacci of non-positive numbers like 0 are not supported
                """.trimIndent()
                assertEquals(expectedErrorMessage, error.message)
                // TODO: the location is not in the diagnostics because the `MessageCollector` defined in KotlinTestUtils,
                //  throws the reports as `AssertionException`s. Evaluate using a different compiler configuration.
//                assertEquals(3, error.location?.start?.line)
//                assertEquals(1, error.location?.start?.col)
//                assertEquals(3, error.location?.end?.line)
//                assertEquals(14, error.location?.end?.col)
            }
        }
    }

    private fun runScript(scriptPath: String): ResultWithDiagnostics<String> {
        val source = ForTestCompileRuntime.transformTestDataPath(testDataPath + File.separator + scriptPath).toScriptSource()
        return compileScript(source)
            .onSuccess { compiled ->
                captureOut {
                    val evaluator = BasicJvmScriptEvaluator()
                    runBlocking {
                        evaluator(compiled)
                    }
                }.asSuccess()
            }
    }

    private fun compileScript(
        script: SourceCode
    ): ResultWithDiagnostics<CompiledScript> {
        val configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.NO_KOTLIN_REFLECT, TestJdkKind.FULL_JDK).apply {
            useFir = true
            updateWithBaseCompilerArguments()
            val hostConfiguration = ScriptingHostConfiguration(defaultJvmScriptingHostConfiguration)
            add(
                ScriptingConfigurationKeys.SCRIPT_DEFINITIONS,
                ScriptDefinition.FromTemplate(hostConfiguration, CompileTimeFibonacci::class, ScriptDefinition::class)
            )
            loadScriptingPlugin(this, testRootDisposable)
        }

        @OptIn(CoreEnvironmentDeprecation::class)
        val environment = KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES)
        val scriptCompiler = ScriptJvmCompilerFromEnvironment(environment)
        val scriptDefinition = environment.configuration.getCompilerExtensions(ScriptDefinitionProvider).first().findDefinition(script)!!

        val scriptCompilationConfiguration = scriptDefinition.compilationConfiguration.with {
            jvm {
                dependenciesFromCurrentContext(wholeClasspath = true)
            }
        }

        return scriptCompiler.compile(script, scriptCompilationConfiguration)
    }
}

// Test Script with Compile Time Fibonacci Computation

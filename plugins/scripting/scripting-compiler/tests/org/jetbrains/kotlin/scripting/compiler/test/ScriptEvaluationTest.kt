import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.fir.extensions.FirScriptResolutionHacksComponent
import org.jetbrains.kotlin.scripting.compiler.plugin.SCRIPT_TEST_BASE_COMPILER_ARGUMENTS_PROPERTY
import org.jetbrains.kotlin.scripting.compiler.plugin.configureFirSession
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SCRIPT_BASE_COMPILER_ARGUMENTS_PROPERTY
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmCompilerIsolated
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmK2CompilerIsolated
import org.jetbrains.kotlin.scripting.compiler.test.assertEqualsTrimmed
import org.jetbrains.kotlin.scripting.compiler.test.dependenciesResolver
import org.jetbrains.kotlin.test.util.JUnit4Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.util.renderError
import kotlin.test.assertEquals
import kotlin.test.junit5.JUnit5Asserter.fail

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


class ScriptEvaluationTest {

    private val isK2 = System.getProperty(SCRIPT_BASE_COMPILER_ARGUMENTS_PROPERTY)?.contains("-language-version 1.9") != true &&
            System.getProperty(SCRIPT_TEST_BASE_COMPILER_ARGUMENTS_PROPERTY)?.contains("-language-version 1.9") != true

    @Test
    fun testExceptionWithCause() {
        checkEvaluateAsError(
            """
                try {
                    throw Exception("Error!")
                } catch (e: Exception) {
                    throw Exception("Oh no", e)
                }
            """.trimIndent().toScriptSource("exceptionWithCause.kts"),
            """
                java.lang.Exception: Oh no
	                    at ExceptionWithCause.<init>(exceptionWithCause.kts:4)
                Caused by: java.lang.Exception: Error!
	                    at ExceptionWithCause.<init>(exceptionWithCause.kts:2)
            """.trimIndent()
        )
    }

    // KT-19423
    @Test
    fun testClassCapturingScriptInstance() {
        val res = checkEvaluate(
            """
                val used = "abc"
                class User {
                    val property = used
                }

                User().property
            """.trimIndent().toScriptSource()
        )
        assertEquals("abc", (res.returnValue as ResultValue.Value).value)
    }

    @Test
    fun testObjectCapturingScriptInstance() {
        val res = checkCompile(
            """
                val used = "abc"
                object User {
                    val property = used
                }

                User.property
            """.trimIndent().toScriptSource()
        )
        assertTrue(res is ResultWithDiagnostics.Failure)
        if (!res.reports.any { it.message == "Object User captures the script class instance. Try to use class or anonymous object instead" }) {
            fail("expecting error about object capturing script instance, got:\n  ${res.reports.joinToString("\n  ") { it.message }}")
        }
    }

    private fun checkEvaluateAsError(script: SourceCode, expectedOutput: String): EvaluationResult {
        val res = checkEvaluate(script)
        assert(res.returnValue is ResultValue.Error)
        ByteArrayOutputStream().use { os ->
            val ps = PrintStream(os)
            (res.returnValue as ResultValue.Error).renderError(ps)
            ps.flush()
            assertEqualsTrimmed(expectedOutput, os.toString())
        }
        return res
    }

    private fun checkCompile(
        script: SourceCode,
        compilationConfiguration: ScriptCompilationConfiguration = ScriptCompilationConfiguration(),
        hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration,
    ): ResultWithDiagnostics<CompiledScript> {
        val compiler = if (isK2) ScriptJvmK2CompilerIsolated(hostConfiguration) else ScriptJvmCompilerIsolated(hostConfiguration)
        return compiler.compile(script, compilationConfiguration)
    }

    private fun checkEvaluate(
        script: SourceCode,
        compilationConfiguration: ScriptCompilationConfiguration = ScriptCompilationConfiguration(),
        evaluationConfiguration: ScriptEvaluationConfiguration = ScriptEvaluationConfiguration()
    ): EvaluationResult {
        val compiled = checkCompile(script, compilationConfiguration).valueOrThrow()
        val evaluator = BasicJvmScriptEvaluator()
        val res = runBlocking {
            evaluator.invoke(compiled, evaluationConfiguration).valueOrThrow()
        }
        return res
    }
}


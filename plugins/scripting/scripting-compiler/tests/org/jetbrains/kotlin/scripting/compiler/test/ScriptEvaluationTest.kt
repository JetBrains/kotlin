import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ScriptJvmCompilerIsolated
import org.jetbrains.kotlin.scripting.compiler.test.assertEqualsTrimmed
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.BasicJvmScriptEvaluator
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration
import kotlin.script.experimental.jvm.util.renderError

/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


class ScriptEvaluationTest : TestCase() {

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

    private fun checkCompile(script: SourceCode): ResultWithDiagnostics<CompiledScript> {
        val compilationConfiguration = ScriptCompilationConfiguration()
        val compiler = ScriptJvmCompilerIsolated(defaultJvmScriptingHostConfiguration)
        return compiler.compile(script, compilationConfiguration)
    }

    private fun checkEvaluate(script: SourceCode): EvaluationResult {
        val compiled = checkCompile(script).valueOrThrow()
        val evaluationConfiguration = ScriptEvaluationConfiguration()
        val evaluator = BasicJvmScriptEvaluator()
        val res = runBlocking {
            evaluator.invoke(compiled, evaluationConfiguration).valueOrThrow()
        }
        return res
    }
}

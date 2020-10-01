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
        checkEvaluate(
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

    private fun checkEvaluate(script: SourceCode, expectedOutput: String) {
        val compilationConfiguration = ScriptCompilationConfiguration()
        val compiler = ScriptJvmCompilerIsolated(defaultJvmScriptingHostConfiguration)
        val compiled = compiler.compile(script, compilationConfiguration).valueOrThrow()
        val evaluationConfiguration = ScriptEvaluationConfiguration()
        val evaluator = BasicJvmScriptEvaluator()
        val res = runBlocking {
            evaluator.invoke(compiled, evaluationConfiguration).valueOrThrow()
        }
        assert(res.returnValue is ResultValue.Error)
        ByteArrayOutputStream().use { os ->
            val ps = PrintStream(os)
            (res.returnValue as ResultValue.Error).renderError(ps)
            ps.flush()
            assertEqualsTrimmed(expectedOutput, os.toString())
        }
    }
}

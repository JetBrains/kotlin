/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services

import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.scripting.ide_services.test_util.*
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.reflect.full.isSubclassOf
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvm.updateClasspath
import kotlin.script.experimental.jvm.util.isError
import kotlin.script.experimental.jvm.util.isIncomplete
import kotlin.script.experimental.jvm.util.scriptCompilationClasspathFromContext

// Adapted form GenericReplTest

// Artificial split into several testsuites, to speed up parallel testing
class JvmIdeServicesTest : TestCase() {
    fun testReplBasics() {
        JvmTestRepl()
            .use { repl ->
                val res1 = repl.compile(
                    SourceCodeTestImpl(
                        0,
                        "val x ="
                    )
                )
                assertTrue("Unexpected check results: $res1", res1.isIncomplete())

                assertEvalResult(
                    repl,
                    "val l1 = listOf(1 + 2)\nl1.first()",
                    3
                )

                assertEvalUnit(
                    repl,
                    "val x = 5"
                )

                assertEvalResult(
                    repl,
                    "x + 2",
                    7
                )
            }
    }

    fun testReplErrors() {
        JvmTestRepl()
            .use { repl ->
                repl.compileAndEval(repl.nextCodeLine("val x = 10"))

                val res = repl.compileAndEval(repl.nextCodeLine("java.util.fish"))
                assertTrue("Expected compile error", res.first.isError())

                val result = repl.compileAndEval(repl.nextCodeLine("x"))
                assertEquals(res.second.toString(), 10, (result.second?.result as ResultValue.Value?)?.value)
            }
    }

    fun testReplErrorsWithLocations() {
        JvmTestRepl()
            .use { repl ->
                val (compileResult, evalResult) = repl.compileAndEval(
                    repl.nextCodeLine(
                        """
                            val foobar = 78
                            val foobaz = "dsdsda"
                            val ddd = ppp
                            val ooo = foobar
                        """.trimIndent()
                    )
                )

                if (compileResult.isError() && evalResult == null) {
                    val errors = compileResult.getErrors()
                    val loc = errors.location
                    if (loc == null) {
                        fail("Location shouldn't be null")
                    } else {
                        assertEquals(3, loc.line)
                        assertEquals(11, loc.column)
                        assertEquals(3, loc.lineEnd)
                        assertEquals(14, loc.columnEnd)
                    }
                } else {
                    fail("Result should be an error")
                }
            }
    }

    fun testReplErrorsAndWarningsWithLocations() {
        JvmTestRepl()
            .use { repl ->
                val (compileResult, evalResult) = repl.compileAndEval(
                    repl.nextCodeLine(
                        """
                        fun f() {
                            val x = 3
                            val y = ooo
                            return y
                        }
                    """.trimIndent()
                    )
                )
                if (compileResult.isError() && evalResult == null) {
                    val errors = compileResult.getErrors()
                    val loc = errors.location
                    if (loc == null) {
                        fail("Location shouldn't be null")
                    } else {
                        assertEquals(3, loc.line)
                        assertEquals(13, loc.column)
                        assertEquals(3, loc.lineEnd)
                        assertEquals(16, loc.columnEnd)
                    }
                } else {
                    fail("Result should be an error")
                }
            }
    }

    fun testReplSyntaxErrorsChecked() {
        JvmTestRepl()
            .use { repl ->
                val res = repl.compileAndEval(repl.nextCodeLine("data class Q(val x: Int, val: String)"))
                assertTrue("Expected compile error", res.first.isError())
            }
    }

    private fun checkContains(actual: Sequence<SourceCodeCompletionVariant>, expected: Set<String>) {
        val variants = actual.map { it.displayText }.toHashSet()
        for (displayText in expected) {
            if (!variants.contains(displayText)) {
                fail("Element $displayText should be in $variants")
            }
        }
    }

    private fun checkDoesntContain(actual: Sequence<SourceCodeCompletionVariant>, expected: Set<String>) {
        val variants = actual.map { it.displayText }.toHashSet()
        for (displayText in expected) {
            if (variants.contains(displayText)) {
                fail("Element $displayText should NOT be in $variants")
            }
        }
    }

    fun testCompletion() = JvmTestRepl().use { repl ->
        repl.compileAndEval(
            repl.nextCodeLine(
                """
                    class AClass(val prop_x: Int) {
                        fun filter(xxx: (AClass).(AClass) -> Boolean): AClass {
                            return if(this.xxx(this)) 
                                this 
                            else 
                                this
                        }
                    }
                    val AClass.prop_y: Int
                        get() = prop_x * prop_x
                        
                    val df = AClass(10)
                    val pro = "some string"
                """.trimIndent()
            )
        )

        val codeLine1 = repl.nextCodeLine(
            """
                df.filter{pr}
            """.trimIndent()
        )
        val completionList1 = repl.complete(codeLine1, 12)
        checkContains(completionList1.valueOrThrow(), setOf("prop_x", "prop_y", "pro", "println(Double)"))
    }

    fun testPackageCompletion() = JvmTestRepl().use { repl ->
        val codeLine1 = repl.nextCodeLine(
            """
                import java.
                val xval = 3
            """.trimIndent()
        )
        val completionList1 = repl.complete(codeLine1, 12)
        checkContains(completionList1.valueOrThrow(), setOf("lang", "math"))
        checkDoesntContain(completionList1.valueOrThrow(), setOf("xval"))
    }

    fun testFileCompletion() = JvmTestRepl().use { repl ->
        val codeLine1 = repl.nextCodeLine(
            """
                val fname = "
            """.trimIndent()
        )
        val completionList1 = repl.complete(codeLine1, 13)
        val files = File(".").listFiles()?.map { it.name }
        assertFalse("There should be files in current dir", files.isNullOrEmpty())
        checkContains(completionList1.valueOrThrow(), files!!.toSet())
    }

    fun testReplCodeFormat() {
        JvmTestRepl()
            .use { repl ->
                val codeLine0 =
                    SourceCodeTestImpl(0, "val l1 = 1\r\nl1\r\n")
                val res = repl.compile(codeLine0)

                assertTrue("Unexpected compile result: $res", res is ResultWithDiagnostics.Success<*>)
            }
    }

    fun testRepPackage() {
        JvmTestRepl()
            .use { repl ->
                assertEvalResult(
                    repl,
                    "package mypackage\n\nval x = 1\nx+2",
                    3
                )

                assertEvalResult(
                    repl,
                    "x+4",
                    5
                )
            }
    }

    fun testReplResultFieldWithFunction() {
        JvmTestRepl()
            .use { repl ->
                assertEvalResultIs<Function0<Int>>(
                    repl,
                    "{ 1 + 2 }"
                )
                assertEvalResultIs<Function0<Int>>(
                    repl,
                    "res0"
                )
                assertEvalResult(
                    repl,
                    "res0()",
                    3
                )
            }
    }

    fun testReplResultField() {
        JvmTestRepl()
            .use { repl ->
                assertEvalResult(
                    repl,
                    "5 * 4",
                    20
                )
                assertEvalResult(
                    repl,
                    "res0 + 3",
                    23
                )
            }
    }

    fun testDependency() {
        val resolver = ScriptDependenciesResolver()

        val conf = ScriptCompilationConfiguration {
            jvm {
                updateClasspath(scriptCompilationClasspathFromContext("test", classLoader = DependsOn::class.java.classLoader))
            }
            compilerOptions("-Xallow-unstable-dependencies")
            defaultImports(DependsOn::class)
            refineConfiguration {
                onAnnotations(DependsOn::class, handler = { configureMavenDepsOnAnnotations(it, resolver) })
            }
        }

        JvmTestRepl(conf)
            .use { repl ->
                val outputJarName = "kt35651.jar"
                val (exitCode, outputJarPath) = compileFile("stringTo.kt", outputJarName)
                assertEquals(ExitCode.OK, exitCode)

                assertCompileFails(
                    repl, """
                        import example.dependency.*
                    """.trimIndent()
                )

                assertEvalUnit(
                    repl, """
                        @file:DependsOn("$outputJarPath")
                        import example.dependency.*
                        
                        val x = listOf<String>()
                    """.trimIndent()
                )

                // This snippet is needed to be evaluated to ensure that importing scopes were created
                // (but default ones were not)
                assertEvalUnit(
                    repl, """
                        import kotlin.math.*
                        
                        val y = listOf<String>()
                    """.trimIndent()
                )

                assertEvalResult(repl, """ "a" to "a" """, "aa")
            }
    }

    fun testAnonymousObjectReflection() {
        JvmTestRepl()
            .use { repl ->
                assertEvalResult(repl, "42", 42)
                assertEvalUnit(repl, "val sim = object : ArrayList<String>() {}")

                val compiledSnippet = checkCompile(repl, "sim")
                val evalResult = repl.eval(compiledSnippet!!)

                val a = (evalResult.valueOrThrow().get().result as ResultValue.Value).value!!
                assertTrue(a::class.isSubclassOf(List::class))
            }
    }

    @OptIn(ExperimentalPathApi::class)
    companion object {
        private const val MODULE_PATH = "plugins/scripting/scripting-ide-services-test"
        private val outputJarDir = createTempDirectory("temp-ide-services")

        private data class CliCompilationResult(val exitCode: ExitCode, val outputJarPath: String)

        private fun compileFile(inputKtFileName: String, outputJarName: String): CliCompilationResult {
            val jarPath = outputJarDir.resolve(outputJarName).toAbsolutePath().invariantSeparatorsPathString

            val compilerArgs = arrayOf(
                "$MODULE_PATH/testData/$inputKtFileName",
                "-kotlin-home", "dist/kotlinc",
                "-d", jarPath
            )

            val exitCode = K2JVMCompiler().exec(
                MessageCollector.NONE,
                Services.EMPTY,
                K2JVMCompilerArguments().apply {
                    K2JVMCompiler().parseArguments(compilerArgs, this)
                }
            )

            return CliCompilationResult(exitCode, jarPath)
        }
    }
}

class LegacyReplTestLong1 : TestCase() {
    fun test256Evals() {
        JvmTestRepl()
            .use { repl ->
                repl.compileAndEval(
                    SourceCodeTestImpl(
                        0,
                        "val x0 = 0"
                    )
                )

                val evals = 256
                for (i in 1..evals) {
                    repl.compileAndEval(
                        SourceCodeTestImpl(
                            i,
                            "val x$i = x${i - 1} + 1"
                        )
                    )
                }

                val (_, evaluated) = repl.compileAndEval(
                    SourceCodeTestImpl(
                        evals + 1,
                        "x$evals"
                    )
                )
                assertEquals(evaluated.toString(), evals, (evaluated?.result as ResultValue.Value?)?.value)
            }
    }
}

class LegacyReplTestLong2 : TestCase() {
    fun testReplSlowdownKt22740() {
        JvmTestRepl()
            .use { repl ->
                repl.compileAndEval(
                    SourceCodeTestImpl(
                        0,
                        "class Test<T>(val x: T) { fun <R> map(f: (T) -> R): R = f(x) }".trimIndent()
                    )
                )

                // We expect that analysis time is not exponential
                for (i in 1..60) {
                    repl.compileAndEval(
                        SourceCodeTestImpl(
                            i,
                            "fun <T> Test<T>.map(f: (T) -> Double): List<Double> = listOf(f(this.x))"
                        )
                    )
                }
            }
    }
}

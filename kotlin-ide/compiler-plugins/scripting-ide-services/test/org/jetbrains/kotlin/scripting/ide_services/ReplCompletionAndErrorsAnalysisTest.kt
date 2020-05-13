/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services

import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.scripting.ide_services.compiler.KJvmReplCompilerWithIdeServices
import org.jetbrains.kotlin.scripting.ide_services.compiler.completion
import org.jetbrains.kotlin.scripting.ide_services.compiler.filterOutShadowedDescriptors
import org.jetbrains.kotlin.scripting.ide_services.compiler.nameFilter
import org.jetbrains.kotlin.scripting.ide_services.test_util.SourceCodeTestImpl
import org.jetbrains.kotlin.scripting.ide_services.test_util.simpleScriptCompilationConfiguration
import org.jetbrains.kotlin.scripting.ide_services.test_util.toList
import org.junit.Assert
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.script.experimental.api.*

class ReplCompletionAndErrorsAnalysisTest : TestCase() {
    @Test
    fun testTrivial() = test {
        run {
            doCompile
            code = """
                data class AClass(val memx: Int, val memy: String)
                data class BClass(val memz: String, val mema: AClass)
                val foobar = 42
                var foobaz = "string"
                val v = BClass("KKK", AClass(5, "25"))
            """.trimIndent()
        }

        run {
            code = "foo"
            cursor = 3
            expect {
                addCompletion("foobar", "foobar", "Int", "property")
                addCompletion("foobaz", "foobaz", "String", "property")
            }
        }

        run {
            code = "v.mema."
            cursor = 7
            expect {
                completions.mode = ComparisonType.INCLUDES
                addCompletion("memx", "memx", "Int", "property")
                addCompletion("memy", "memy", "String", "property")
            }
        }

        run {
            code = "listO"
            cursor = 5
            expect {
                addCompletion("listOf(", "listOf(T)", "List<T>", "method")
                addCompletion("listOf()", "listOf()", "List<T>", "method")
                addCompletion("listOf(", "listOf(vararg T)", "List<T>", "method")
                addCompletion("listOfNotNull(", "listOfNotNull(T?)", "List<T>", "method")
                addCompletion("listOfNotNull(", "listOfNotNull(vararg T?)", "List<T>", "method")
            }
        }
    }

    @Test
    fun testNoVariantsAfterFinalExpressions() = test {
        fun testNoVariants(testCode: String, testCursor: Int? = null) = run {
            doComplete
            code = testCode
            cursor = testCursor ?: testCode.length
            expect {
                completions.mode = ComparisonType.EQUALS
            }
        }

        testNoVariants("val x1 = 42")
        testNoVariants("val x2 = 42.42")
        testNoVariants("val x3 = 'v'")
        testNoVariants("val x4 = \"str42\"")

        testNoVariants("val x5 = 40 + 41 * 42")
        testNoVariants("val x6 = 40 + 41 * 42", 16) // after 41
        testNoVariants("val x7 = 40 + 41 * 42", 11) // after 40
        testNoVariants("6 * (2 + 5)")

        testNoVariants("\"aBc\".capitalize()")
        testNoVariants(
            """
                "abc" + "def"
            """.trimIndent()
        )
    }

    @Test
    fun testPackagesImport() = test {
        run {
            cursor = 17
            code = "import java.lang."
            expect {
                completions.mode = ComparisonType.INCLUDES
                addCompletion("Process", "Process", " (java.lang)", "class")
            }
        }
    }

    @Test
    fun testExtensionMethods() = test {
        run {
            doCompile
            code = """
                class AClass(val c_prop_x: Int) {
                    fun filter(xxx: (AClass).() -> Boolean): AClass {
                        return this
                    }
                }
                val AClass.c_prop_y: Int
                    get() = c_prop_x * c_prop_x
                
                fun AClass.c_meth_z(v: Int) = v * c_prop_y
                val df = AClass(10)
                val c_zzz = "some string"
            """.trimIndent()
        }

        run {
            code = "df.filter{ c_ }"
            cursor = 13
            expect {
                addCompletion("c_prop_x", "c_prop_x", "Int", "property")
                addCompletion("c_zzz", "c_zzz", "String", "property")
                addCompletion("c_prop_y", "c_prop_y", "Int", "property")
                addCompletion("c_meth_z(", "c_meth_z(Int)", "Int", "method")
            }
        }

        run {
            code = "df.fil"
            cursor = 6
            expect {
                addCompletion("filter { ", "filter(Line_1_simplescript.AClass.() -> ...", "Line_1_simplescript.AClass", "method")
            }
        }
    }

    @Test
    fun testBacktickedFields() = test {
        run {
            doCompile
            code = """
                class AClass(val `c_prop   x y z`: Int)
                val df = AClass(33)
            """.trimIndent()
        }

        run {
            code = "df.c_"
            cursor = 5
            expect {
                addCompletion("`c_prop   x y z`", "`c_prop   x y z`", "Int", "property")
            }
        }
    }

    @Test
    fun testListErrors() = test {
        run {
            doCompile
            code = """
                data class AClass(val memx: Int, val memy: String)
                data class BClass(val memz: String, val mema: AClass)
                val foobar = 42
                var foobaz = "string"
                val v = BClass("KKK", AClass(5, "25"))
            """.trimIndent()
            expect {
                errors.mode = ComparisonType.EQUALS
            }
        }

        run {
            code = """
                val a = AClass("42", 3.14)
                val b: Int = "str"
                val c = foob
            """.trimIndent()
            expect {
                addError(1, 16, "Type mismatch: inferred type is String but Int was expected", "ERROR")
                addError(1, 22, "The floating-point literal does not conform to the expected type String", "ERROR")
                addError(2, 14, "Type mismatch: inferred type is String but Int was expected", "ERROR")
                addError(3, 9, "Unresolved reference: foob", "ERROR")
            }
        }
    }

    @Test
    fun testCompletionDuplication() = test {
        for (i in 1..6) {
            run {
                if (i == 5) doCompile
                if (i % 2 == 1) doErrorCheck

                val value = "a".repeat(i)
                code = "val ddddd = \"$value\""
                cursor = 13 + i
            }
        }

        run {
            code = "dd"
            cursor = 2
            expect {
                addCompletion("ddddd", "ddddd", "String", "property")
            }
        }
    }

    @Test
    fun testAnalyze() = test {
        run {
            code = """
                val foo = 42
                foo
            """.trimIndent()
            expect {
                errors.mode = ComparisonType.EQUALS
                resultType = "Int"
            }
        }
    }

    @Test
    fun testNameFilter() = test {
        run {
            code = """
                val xxxyyy = 42
                val yyyxxx = 43
            """.trimIndent()
            doCompile
        }

        run {
            code = "xxx"
            cursor = 3
            expect {
                addCompletion("xxxyyy", "xxxyyy", "Int", "property")
            }
        }

        run {
            code = "xxx"
            cursor = 3
            compilationConfiguration = ScriptCompilationConfiguration {
                completion {
                    nameFilter { name, namePart -> name.endsWith(namePart) }
                }
            }
            expect {
                addCompletion("yyyxxx", "yyyxxx", "Int", "property")
            }
        }
    }

    @Test
    fun testShadowedDescriptors() = test {
        for (i in 1..2)
            run {
                code = """
                    val xxxyyy = 42
                """.trimIndent()
                doCompile
            }

        for ((flag, expectedSize) in listOf(true to 1, false to 2))
            run {
                code = "xxx"
                cursor = 3
                compilationConfiguration = ScriptCompilationConfiguration {
                    completion {
                        filterOutShadowedDescriptors(flag)
                    }
                }
                expect {
                    completions.size = expectedSize
                }
            }
    }

    class TestConf {
        private val runs = mutableListOf<Run>()

        fun run(setup: (Run).() -> Unit) {
            val r = Run()
            r.setup()
            runs.add(r)
        }

        fun collect() = runs.map { it.collect() }

        class Run {
            private var _doCompile = false
            val doCompile: Unit
                get() {
                    _doCompile = true
                }

            private var _doComplete = false
            val doComplete: Unit
                get() {
                    _doComplete = true
                }

            private var _doErrorCheck = false
            val doErrorCheck: Unit
                get() {
                    _doErrorCheck = true
                }

            var cursor: Int? = null
            var compilationConfiguration: ScriptCompilationConfiguration? = null
            var code: String = ""
            private var _expected: Expected = Expected(this)

            fun expect(setup: (Expected).() -> Unit) {
                _expected = Expected(this)
                _expected.setup()
            }

            fun collect(): Pair<RunRequest, ExpectedResult> {
                return RunRequest(cursor, code, _doCompile, _doComplete, _doErrorCheck, compilationConfiguration) to _expected.collect()
            }

            class Expected(private val run: Run) {
                val completions = ExpectedList<SourceCodeCompletionVariant>(run::doComplete)
                fun addCompletion(text: String, displayText: String, tail: String, icon: String) {
                    completions.add(SourceCodeCompletionVariant(text, displayText, tail, icon))
                }

                val errors = ExpectedList<ScriptDiagnostic>(run::doErrorCheck)
                fun addError(startLine: Int, startCol: Int, message: String, severity: String) {
                    errors.add(
                        ScriptDiagnostic(
                            ScriptDiagnostic.unspecifiedError,
                            message,
                            ScriptDiagnostic.Severity.valueOf(severity),
                            location = SourceCode.Location(
                                SourceCode.Position(startLine, startCol)
                            )
                        )
                    )
                }

                var resultType: String? by ExpectedNullableVar(run::doErrorCheck)

                fun collect(): ExpectedResult {
                    return ExpectedResult(completions, errors, resultType)
                }
            }

        }
    }

    private fun test(setup: (TestConf).() -> Unit) {
        val test = TestConf()
        test.setup()
        runBlocking { checkEvaluateInRepl(simpleScriptCompilationConfiguration, test.collect()) }
    }

    enum class ComparisonType {
        COMPARE_SIZE, INCLUDES, EQUALS, DONT_CHECK
    }

    data class RunRequest(
        val cursor: Int?,
        val code: String,
        val doCompile: Boolean,
        val doComplete: Boolean,
        val doErrorCheck: Boolean,
        val compilationConfiguration: ScriptCompilationConfiguration?
    )

    interface ExpectedOptions {
        val mode: ComparisonType
        val size: Int
    }

    class ExpectedList<T>(private val runProperty: KProperty0<Unit>) : ExpectedOptions {
        val list = mutableListOf<T>()

        override var mode = ComparisonType.DONT_CHECK
        override var size = 0
            set(value) {
                if (mode == ComparisonType.DONT_CHECK)
                    mode = ComparisonType.COMPARE_SIZE
                runProperty.get()
                field = value
            }

        fun add(elem: T) {
            if (mode == ComparisonType.DONT_CHECK)
                mode = ComparisonType.EQUALS
            runProperty.get()
            list.add(elem)
        }
    }

    class ExpectedNullableVar<T>(private val runProperty: KProperty0<Unit>) {
        private var variable: T? = null

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T? = variable
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
            runProperty.get()
            variable = value
        }
    }

    data class ExpectedResult(
        val completions: ExpectedList<SourceCodeCompletionVariant>,
        val errors: ExpectedList<ScriptDiagnostic>,
        val resultType: String?,
    )

    data class ActualResult(
        val completions: List<SourceCodeCompletionVariant>,
        val errors: List<ScriptDiagnostic>,
        val resultType: String?,
    )

    private val currentLineCounter = AtomicInteger()

    private fun nextCodeLine(code: String): SourceCode =
        SourceCodeTestImpl(
            currentLineCounter.getAndIncrement(),
            code
        )

    private suspend fun evaluateInRepl(
        compilationConfiguration: ScriptCompilationConfiguration,
        snippets: List<RunRequest>
    ): List<ResultWithDiagnostics<ActualResult>> {
        val compiler = KJvmReplCompilerWithIdeServices()
        return snippets.map { runRequest ->
            with(runRequest) {
                val newCompilationConfiguration = this.compilationConfiguration?.let {
                    ScriptCompilationConfiguration(compilationConfiguration, it)
                } ?: compilationConfiguration

                val pos = SourceCode.Position(0, 0, cursor)
                val codeLine = nextCodeLine(code)
                val completionRes = if (doComplete && cursor != null) {
                    val res = compiler.complete(codeLine, pos, newCompilationConfiguration)
                    res.toList().filter { it.tail != "keyword" }
                } else {
                    emptyList()
                }

                val analysisResult = if (doErrorCheck) {
                    val codeLineForErrorCheck = nextCodeLine(code)
                    compiler.analyze(codeLineForErrorCheck, SourceCode.Position(0, 0), newCompilationConfiguration).valueOrNull()
                } else {
                    null
                } ?: ReplAnalyzerResult()

                val errorsSequence = analysisResult[ReplAnalyzerResult.analysisDiagnostics]!!
                val resultType = analysisResult[ReplAnalyzerResult.renderedResultType]

                if (doCompile) {
                    val codeLineForCompilation = nextCodeLine(code)
                    compiler.compile(codeLineForCompilation, newCompilationConfiguration)
                }

                ActualResult(completionRes, errorsSequence.toList(), resultType).asSuccess()
            }
        }
    }

    private fun <T> checkLists(index: Int, checkName: String, expected: List<T>, actual: List<T>, options: ExpectedOptions) {
        when (options.mode) {
            ComparisonType.EQUALS -> Assert.assertEquals(
                "#$index ($checkName): Expected $expected, got $actual",
                expected,
                actual
            )
            ComparisonType.INCLUDES -> Assert.assertTrue(
                "#$index ($checkName): Expected $actual to include $expected",
                actual.containsAll(expected)
            )
            ComparisonType.COMPARE_SIZE -> Assert.assertEquals(
                "#$index ($checkName): Expected list size to be equal to ${options.size}, but was ${actual.size}",
                options.size,
                actual.size
            )
            ComparisonType.DONT_CHECK -> {
            }
        }
    }

    private suspend fun checkEvaluateInRepl(
        compilationConfiguration: ScriptCompilationConfiguration,
        testData: List<Pair<RunRequest, ExpectedResult>>
    ) {
        val (snippets, expected) = testData.unzip()
        val expectedIter = expected.iterator()
        evaluateInRepl(compilationConfiguration, snippets).forEachIndexed { index, res ->
            when (res) {
                is ResultWithDiagnostics.Failure -> Assert.fail("#$index: Expected result, got $res")
                is ResultWithDiagnostics.Success -> {
                    val (expectedCompletions, expectedErrors, expectedResultType) = expectedIter.next()
                    val (completionsRes, errorsRes, resultType) = res.value

                    checkLists(index, "completions", expectedCompletions.list, completionsRes, expectedCompletions)
                    val expectedErrorsWithPath = expectedErrors.list.map {
                        it.copy(sourcePath = errorsRes.firstOrNull()?.sourcePath)
                    }
                    checkLists(index, "errors", expectedErrorsWithPath, errorsRes, expectedErrors)
                    assertEquals("Analysis result types are different", expectedResultType, resultType)
                }
            }
        }
    }

}


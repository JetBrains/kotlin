/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_services.test_util

import junit.framework.TestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.scripting.ide_services.compiler.KJvmReplCompilerWithIdeServices
import org.junit.Assert
import java.io.Writer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0
import kotlin.script.experimental.api.*
import kotlin.system.measureTimeMillis

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

        var loggingInfo: CSVLoggingInfo? = null

        fun expect(setup: (Expected).() -> Unit) {
            _expected = Expected(this)
            _expected.setup()
        }

        fun collect(): Pair<RunRequest, ExpectedResult> {
            return RunRequest(
                cursor,
                code,
                _doCompile,
                _doComplete,
                _doErrorCheck,
                compilationConfiguration,
                loggingInfo
            ) to _expected.collect()
        }

        class Expected(private val run: Run) {
            val completions = ExpectedList<SourceCodeCompletionVariant>(run::doComplete)
            fun addCompletion(text: String, displayText: String, tail: String, icon: String) {
                completions.add(SourceCodeCompletionVariant(text, displayText, tail, icon))
            }

            val errors = ExpectedList<ScriptDiagnostic>(run::doErrorCheck)
            fun addError(startLine: Int, startCol: Int, endLine: Int, endCol: Int, message: String, severity: String) {
                errors.add(
                    ScriptDiagnostic(
                        ScriptDiagnostic.unspecifiedError,
                        message,
                        ScriptDiagnostic.Severity.valueOf(severity),
                        location = SourceCode.Location(
                            SourceCode.Position(startLine, startCol),
                            SourceCode.Position(endLine, endCol)
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

fun test(setup: (TestConf).() -> Unit) {
    val test = TestConf()
    test.setup()
    runBlocking { checkEvaluateInRepl(simpleScriptCompilationConfiguration, test.collect()) }
}

enum class ComparisonType {
    COMPARE_SIZE, INCLUDES, EQUALS, DONT_CHECK
}

data class CSVLoggingInfoItem(
    val writer: Writer,
    val xValue: Int,
    val prefix: String = "",
) {
    fun writeValue(value: Any) {
        writer.write("$prefix$xValue;$value\n")
        writer.flush()
    }
}

data class CSVLoggingInfo(
    val compile: CSVLoggingInfoItem? = null,
    val complete: CSVLoggingInfoItem? = null,
    val analyze: CSVLoggingInfoItem? = null,
)

data class RunRequest(
    val cursor: Int?,
    val code: String,
    val doCompile: Boolean,
    val doComplete: Boolean,
    val doErrorCheck: Boolean,
    val compilationConfiguration: ScriptCompilationConfiguration?,
    val loggingInfo: CSVLoggingInfo?,
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

private fun nextCodeLine(code: String, lineCounter: AtomicInteger): SourceCode =
    SourceCodeTestImpl(
        lineCounter.getAndIncrement(),
        code
    )

private suspend fun evaluateInRepl(
    compilationConfiguration: ScriptCompilationConfiguration,
    snippets: List<RunRequest>,
    lineCounter: AtomicInteger
): List<ResultWithDiagnostics<ActualResult>> {
    val compiler = KJvmReplCompilerWithIdeServices()
    return snippets.map { runRequest ->
        with(runRequest) {
            val newCompilationConfiguration = this.compilationConfiguration?.let {
                ScriptCompilationConfiguration(compilationConfiguration, it)
            } ?: compilationConfiguration

            val pos = SourceCode.Position(0, 0, cursor)
            val codeLine = nextCodeLine(code, lineCounter)
            val completionRes = if (doComplete && cursor != null) {
                var res: ResultWithDiagnostics<ReplCompletionResult>?
                val timeMillis = measureTimeMillis { res = compiler.complete(codeLine, pos, newCompilationConfiguration) }

                loggingInfo?.complete?.writeValue(timeMillis)

                res!!.toList().filter { it.tail != "keyword" }
            } else {
                emptyList()
            }

            val analysisResult = if (doErrorCheck) {
                val codeLineForErrorCheck = nextCodeLine(code, lineCounter)

                var res: ReplAnalyzerResult?
                val timeMillis = measureTimeMillis {
                    res = compiler.analyze(codeLineForErrorCheck, SourceCode.Position(0, 0), newCompilationConfiguration).valueOrNull()
                }

                loggingInfo?.analyze?.writeValue(timeMillis)

                res
            } else {
                null
            } ?: ReplAnalyzerResult()

            val errorsSequence = analysisResult[ReplAnalyzerResult.analysisDiagnostics]!!
            val resultType = analysisResult[ReplAnalyzerResult.renderedResultType]

            if (doCompile) {
                val codeLineForCompilation = nextCodeLine(code, lineCounter)

                val timeMillis = measureTimeMillis { compiler.compile(codeLineForCompilation, newCompilationConfiguration) }

                loggingInfo?.compile?.writeValue(timeMillis)
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
    evaluateInRepl(compilationConfiguration, snippets, AtomicInteger()).forEachIndexed { index, res ->
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
                TestCase.assertEquals("Analysis result types are different", expectedResultType, resultType)
            }
        }
    }
}

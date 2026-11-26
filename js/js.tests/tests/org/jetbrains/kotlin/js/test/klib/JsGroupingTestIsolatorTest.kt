/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.klib

import org.jetbrains.kotlin.js.test.runners.AbstractJsCodegenBoxTest
import org.jetbrains.kotlin.test.model.GroupingTestIsolator.BatchToken
import org.jetbrains.kotlin.test.services.moduleStructure
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class JsGroupingTestIsolatorTest : AbstractJsCodegenBoxTest() {
    private enum class Batch {
        OF_PLAIN_TEST,
        OF_ITS_SETTINGS,
        ISOLATED,
    }

    private class Case(val name: String, val expected: Batch, val source: String)

    private val cases = listOf(
        Case("ignoredOnAnotherBackend", Batch.OF_PLAIN_TEST, "// IGNORE_BACKEND: WASM\n$BOX"),
        Case("declarations", Batch.OF_PLAIN_TEST, "package foo\n\nclass A {\n    fun ok() = \"OK\"\n}\n\nfun box() = A().ok()\n"),

        Case("withStdlib", Batch.OF_ITS_SETTINGS, "// WITH_STDLIB\n$BOX"),
        Case("fullRuntime", Batch.OF_ITS_SETTINGS, "// KJS_WITH_FULL_RUNTIME\n$BOX"),
        Case("languageFeature", Batch.OF_ITS_SETTINGS, "// LANGUAGE: +ContextParameters\n$BOX"),
        Case("optIn", Batch.OF_ITS_SETTINGS, "// OPT_IN: kotlin.ExperimentalStdlibApi\n$BOX"),
        Case("constAndLet", Batch.OF_ITS_SETTINGS, "// USE_CONST_AND_LET_FOR_VARIABLES\n$BOX"),
        Case("onlyIrDce", Batch.OF_ITS_SETTINGS, "// ONLY_IR_DCE\n$BOX"),

        Case("ignoredOnJs", Batch.ISOLATED, "// IGNORE_BACKEND: JS_IR\n$BOX"),
        Case("ignoredOnAnyBackend", Batch.ISOLATED, "// IGNORE_BACKEND_K2: ANY\n$BOX"),
        Case("callMain", Batch.ISOLATED, "// CALL_MAIN\n$BOX"),
        Case("splitPerModule", Batch.ISOLATED, "// SPLIT_PER_MODULE\n$BOX"),
        Case("keep", Batch.ISOLATED, "// KEEP: box\n$BOX"),
        Case("irDump", Batch.ISOLATED, "// DUMP_IR\n$BOX"),
        Case("firDump", Batch.ISOLATED, "// FIR_DUMP\n$BOX"),
        Case("jsAstCheck", Batch.ISOLATED, "// CHECK_NOT_CALLED: foo\n$BOX"),
        Case("recompiledFile", Batch.ISOLATED, "// FILE: main.kt\n// RECOMPILE\n$BOX"),
        Case("jsFile", Batch.ISOLATED, "// FILE: main.kt\n$BOX// FILE: lib.js\nfunction foo() {}\n"),
        Case(
            "severalModules", Batch.ISOLATED,
            "// MODULE: lib\n// FILE: lib.kt\nfun ok() = \"OK\"\n\n// MODULE: main(lib)\n// FILE: main.kt\nfun box() = ok()\n",
        ),
        Case("externalDeclaration", Batch.ISOLATED, "external fun foo(): String\n$BOX"),
        Case("jsCode", Batch.ISOLATED, "fun foo(): String = js(\"'OK'\")\n$BOX"),
        Case("jsAnnotation", Batch.ISOLATED, "@JsName(\"bar\")\nfun foo() = \"OK\"\n$BOX"),
        Case("dynamicType", Batch.ISOLATED, "fun foo(x: dynamic) = \"OK\"\n$BOX"),
        Case("mainFunction", Batch.ISOLATED, "fun main() {}\n$BOX"),
        Case("associatedObject", Batch.ISOLATED, "annotation class Key(val kClass: kotlin.reflect.KClass<*>) // AssociatedObjectKey\n$BOX"),
        Case("keptPackage", Batch.ISOLATED, "package helpers\n\n$BOX"),
        Case("ownPackageInString", Batch.ISOLATED, "package foo\n\nclass A\n\nfun box() = if (\"foo.A\" != \"\") \"OK\" else \"FAIL\"\n"),
        Case("jsClass", Batch.ISOLATED, "class A\n\nfun box() = if (A::class.js != null) \"OK\" else \"FAIL\"\n"),
        Case("exceptionClass", Batch.ISOLATED, "class MyException : Exception()\n$BOX"),
        Case(
            "suspendFunctionType", Batch.ISOLATED,
            "@Suppress(\"IMPLEMENTING_SUSPEND_FUNCTION_INTERFACE\")\nclass A : suspend () -> Unit {\n" +
                    "    override suspend fun invoke() {}\n}\n$BOX",
        ),
    )

    @Test
    fun batchTokens() {
        withTestDataDirectory { directory ->
            val tokenOfPlainTest = computeBatchToken(directory, "plain", BOX)
            assertNotEquals<BatchToken>(BatchToken.Isolated, tokenOfPlainTest, "A plain test must not be isolated")
            val mismatches = cases.mapNotNull { case ->
                val token = computeBatchToken(directory, case.name, case.source)
                val actual = when (token) {
                    BatchToken.Isolated -> Batch.ISOLATED
                    tokenOfPlainTest -> Batch.OF_PLAIN_TEST
                    else -> Batch.OF_ITS_SETTINGS
                }
                "${case.name}: expected ${case.expected}, but was $actual ($token)".takeIf { actual != case.expected }
            }
            assertTrue(mismatches.isEmpty(), mismatches.joinToString("\n"))
        }
    }

    @Test
    fun testsAskingForFullRuntimeByDifferentDirectivesShareBatch() {
        withTestDataDirectory { directory ->
            assertEquals(
                computeBatchToken(directory, "withStdlib", "// WITH_STDLIB\n$BOX"),
                computeBatchToken(directory, "fullRuntime", "// KJS_WITH_FULL_RUNTIME\n$BOX"),
            )
        }
    }

    @Test
    fun testsWithDifferentSettingsDoNotShareBatch() {
        withTestDataDirectory { directory ->
            assertNotEquals(
                computeBatchToken(directory, "withStdlib", "// WITH_STDLIB\n$BOX"),
                computeBatchToken(directory, "constAndLet", "// USE_CONST_AND_LET_FOR_VARIABLES\n$BOX"),
            )
        }
    }

    private fun withTestDataDirectory(action: (File) -> Unit) {
        val directory = createTempDirectory("JsGroupingTestIsolatorTest").toFile()
        try {
            action(directory)
        } finally {
            directory.deleteRecursively()
        }
    }

    private fun computeBatchToken(directory: File, name: String, source: String): BatchToken {
        val testDataFile = directory.resolve("$name.kt").apply { writeText(source) }
        initTestRunnerAndCreateModuleStructure(testDataFile.path)
        try {
            val configuration = nonGroupingRunner.testConfiguration
            val isolator = configuration.groupingTestIsolators.filterIsInstance<JsGroupingTestIsolator>().single()
            return isolator.computeBatchToken(nonGroupingRunner.testServices.moduleStructure)
        } finally {
            nonGroupingRunner.finalizeAndDispose()
            groupingStageRunner.finalizeAndDispose()
        }
    }

    private companion object {
        const val BOX = "fun box() = \"OK\"\n"
    }
}

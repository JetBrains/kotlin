/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("JUnitTestCaseWithNoTests")

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.FirPipeline
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class ClassicKT39548Test : KT39548TestBase()

@FirPipeline
@Tag("frontend-fir")
class FirClassicKT39548Test : KT39548TestBase()

abstract class KT39548TestBase : AbstractNativeSimpleTest() {
    @Test
    fun test() {
        Assumptions.assumeTrue(targets.testTarget.family == Family.MINGW)

        val longName = StringBuilder().apply {
            repeat(10_000_000) {
                append('a')
            }
        }

        val text = """
            import kotlin.test.*

            fun $longName(): Int = 42
            fun <T> same(value: T): T = value
            val globalInt1: Int = same(1)
            val globalStringA: String = same("a")
            @ThreadLocal val threadLocalInt2: Int = same(2)
            @ThreadLocal val threadLocalStringB: String = same("b")

            fun main() {
                // Ensure function don't get DCEd:
                val resultOfFunctionWithLongName = $longName()
                assertEquals(42, resultOfFunctionWithLongName)

                // Check that top-level initializers did run as expected:
                assertEquals(1, globalInt1)
                assertEquals("a", globalStringA)
                assertEquals(2, threadLocalInt2)
                assertEquals("b", threadLocalStringB)
            }
        """.trimIndent()

        val file = buildDir.resolve("main.kt")
        file.parentFile.mkdirs()
        file.writeText(text)

        val testCase = generateTestCaseWithSingleFile(
            sourceFile = file,
            testKind = TestKind.STANDALONE_NO_TR,
            extras = TestCase.NoTestRunnerExtras()
        )

        val compilationResult = compileToExecutableInOneStage(testCase).assertSuccess()

        runExecutableAndVerify(testCase, TestExecutable.fromCompilationResult(testCase, compilationResult))
    }
}
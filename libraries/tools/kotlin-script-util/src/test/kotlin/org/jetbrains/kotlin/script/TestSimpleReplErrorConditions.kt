/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.script

import org.jetbrains.kotlin.cli.common.repl.NO_ACTION
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.junit.JUnitAsserter


class TestErrorConditions {
    @Test
    fun testBasicCompilerErrors() {
        SimplifiedRepl().use { repl ->
            try {
                repl.compileAndEval("java.util.Xyz()")
            }
            catch (ex: ReplCompilerException) {
                assertTrue("unresolved reference: Xyz" in ex.message!!)
            }
        }
    }

    @Test
    fun testBasicRuntimeErrors() {
        SimplifiedRepl().use { repl ->
            try {
                repl.compileAndEval("val x: String? = null; x!!")
            }
            catch (ex: ReplEvalRuntimeException) {
                assertTrue("NullPointerException" in ex.message!!)
            }
        }
    }

    @Test
    fun testResumeAfterCompilerError() {
        SimplifiedRepl().use { repl ->
            repl.compileAndEval("val x = 10")
            try {
                repl.compileAndEval("java.util.fish")
                JUnitAsserter.fail("Expected compile error")
            }
            catch (ex: ReplCompilerException) {
                NO_ACTION()
            }

            val result = repl.compileAndEval("x")
            assertEquals(10, result.resultValue)
        }
    }

    @Test
    fun testResumeAfterRuntimeError() {
        SimplifiedRepl().use { repl ->
            try {
                repl.compileAndEval("val y = 100")
                repl.compileAndEval("val x: String? = null")
                try {
                    repl.compileAndEval("x!!")
                    JUnitAsserter.fail("Expected runtime error")
                }
                catch (ex: ReplEvalRuntimeException) {
                    NO_ACTION()
                }

                val result = repl.compileAndEval("\"\$x \$y\"")
                assertEquals("null 100", result.resultValue)
            }
            catch (ex: ReplEvalRuntimeException) {
                assertTrue("NullPointerException" in ex.message!!)
            }
        }
    }
}
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

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.script

import org.jetbrains.kotlin.cli.common.repl.ReplRepeatingMode
import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.junit.Test
import kotlin.script.templates.standard.ScriptTemplateWithBindings
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.junit.JUnitAsserter


class TestRepeatableEval {
    @Test
    fun testRepeatableLastNotAllowed() {
        SimplifiedRepl(repeatingMode = ReplRepeatingMode.NONE).use { repl ->
            val line1 = repl.compile(repl.nextCodeLine("""val x = 1"""))
            val line2 = repl.compile(repl.nextCodeLine("""val y = 2"""))
            val line3 = repl.compile(repl.nextCodeLine("""x+y"""))

            repl.eval(line1)
            repl.eval(line2)
            repl.eval(line3)
            try {
                repl.eval(line3)
                JUnitAsserter.fail("Expecting history mismatch error")
            }
            catch (ex: ReplCompilerException) {
                assertTrue("History Mismatch" in ex.message!!)
            }
        }
    }

    @Test
    fun testRepeatableAnyNotAllowedInModeNONE() {
        SimplifiedRepl(repeatingMode = ReplRepeatingMode.NONE).use { repl ->
            val line1 = repl.compile(repl.nextCodeLine("""val x = 1"""))
            val line2 = repl.compile(repl.nextCodeLine("""val y = 2"""))
            val line3 = repl.compile(repl.nextCodeLine("""x+y"""))

            repl.eval(line1)
            repl.eval(line2)
            repl.eval(line3)

            try {
                repl.eval(line2)
                JUnitAsserter.fail("Expecting history mismatch error")
            }
            catch (ex: ReplCompilerException) {
                assertTrue("History Mismatch" in ex.message!!)
            }
        }
    }

    @Test
    fun testRepeatableAnyNotAllowedInModeMOSTRECENT() {
        SimplifiedRepl(repeatingMode = ReplRepeatingMode.REPEAT_ONLY_MOST_RECENT).use { repl ->
            val line1 = repl.compile(repl.nextCodeLine("""val x = 1"""))
            val line2 = repl.compile(repl.nextCodeLine("""val y = 2"""))
            val line3 = repl.compile(repl.nextCodeLine("""x+y"""))

            repl.eval(line1)
            repl.eval(line2)
            repl.eval(line3)

            try {
                repl.eval(line2)
                JUnitAsserter.fail("Expecting history mismatch error")
            }
            catch (ex: ReplCompilerException) {
                assertTrue("History Mismatch" in ex.message!!)
            }
        }
    }

    @Test
    fun testRepeatableExecutionsMOSTRECENT() {
        SimplifiedRepl(repeatingMode = ReplRepeatingMode.REPEAT_ONLY_MOST_RECENT).use { repl ->
            val line1 = repl.compile(repl.nextCodeLine("""val x = 1"""))
            val line2 = repl.compile(repl.nextCodeLine("""val y = 2"""))
            val line3 = repl.compile(repl.nextCodeLine("""x+y"""))

            repl.eval(line1)
            repl.eval(line1)
            repl.eval(line1)

            repl.eval(line2)
            repl.eval(line2)
            repl.eval(line2)
            repl.eval(line2)

            val result = repl.eval(line3)
            assertEquals(3, result.resultValue)
        }
    }

    @Test
    fun testRepeatableExecutionsREPEATANYPREVIOUS() {
        SimplifiedRepl(repeatingMode = ReplRepeatingMode.REPEAT_ANY_PREVIOUS).use { repl ->
            val line1 = repl.compile(repl.nextCodeLine("""val x = 1"""))
            val line2 = repl.compile(repl.nextCodeLine("""val y = 2"""))
            val line3 = repl.compile(repl.nextCodeLine("""x+y"""))

            repl.eval(line1)

            repl.eval(line2)

            repl.eval(line1)

            repl.eval(line2)

            val resultFirstTime = repl.eval(line3)
            assertEquals(3, resultFirstTime.resultValue)

            repl.eval(line2)

            val resultSecondTime = repl.eval(line3)
            assertEquals(3, resultSecondTime.resultValue)

            repl.eval(line1)
            repl.eval(line1)
            repl.eval(line1)
            repl.eval(line1)
        }
    }

    @Test
    fun testRepeatableChangesValues() {
        val scriptArgs = ScriptArgsWithTypes(arrayOf(emptyMap<String, Any?>()), arrayOf(Map::class))
        SimplifiedRepl(repeatingMode = ReplRepeatingMode.REPEAT_ANY_PREVIOUS,
                       scriptDefinition = KotlinScriptDefinitionEx(ScriptTemplateWithBindings::class, scriptArgs)).use { repl ->
            val line1 = repl.compile(repl.nextCodeLine("""val something = bindings.get("x") as Int"""))
            val line2 = repl.compile(repl.nextCodeLine("""val somethingElse = something + (bindings.get("y") as Int)"""))
            val line3 = repl.compile(repl.nextCodeLine("""somethingElse + 10"""))

            val firstArgs = ScriptArgsWithTypes(arrayOf(mapOf<String, Any?>("x" to 100, "y" to 50)), arrayOf(Map::class))
            repl.eval(line1, firstArgs)
            repl.eval(line2, firstArgs)
            val result1 = repl.eval(line3)
            assertEquals(160, result1.resultValue)

            // same thing twice, same results
            repl.eval(line1, firstArgs)
            repl.eval(line2, firstArgs)
            val result2 = repl.eval(line3)
            assertEquals(160, result2.resultValue)

            val secondArgs = ScriptArgsWithTypes(arrayOf(mapOf<String, Any?>("x" to 200, "y" to 70)), arrayOf(Map::class))

            // eval line1 with different args affects it and would only affect line2
            // but not line3 until line2 is re-eval'd
            repl.eval(line1, secondArgs)
            val result3 = repl.eval(line3)
            assertEquals(160, result3.resultValue)

            // but if we do line2 again, the line3 will change...
            repl.eval(line2, secondArgs)
            val result4 = repl.eval(line3)
            assertEquals(280, result4.resultValue)
        }
    }
}
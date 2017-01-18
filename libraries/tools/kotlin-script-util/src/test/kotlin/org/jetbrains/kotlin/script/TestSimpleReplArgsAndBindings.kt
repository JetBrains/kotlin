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

import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.junit.Test
import kotlin.script.templates.standard.ScriptTemplateWithArgs
import kotlin.script.templates.standard.ScriptTemplateWithBindings
import kotlin.test.assertEquals


class TestSimpleReplArgsAndBindings {
    @Test
    fun testUsingArgsForScript() {
        val scriptArgs = ScriptArgsWithTypes(arrayOf(arrayOf("100")), arrayOf(Array<String>::class))
        SimplifiedRepl(scriptDefinition = KotlinScriptDefinitionEx(ScriptTemplateWithArgs::class, scriptArgs)).use { repl ->
            val result = repl.compileAndEval("args[0].toInt()")
            assertEquals(100, result.resultValue)
        }
    }

    @Test
    fun testOverridingArgsOnEachEval() {
        val scriptArgs = ScriptArgsWithTypes(arrayOf(arrayOf("100")), arrayOf(Array<String>::class))
        SimplifiedRepl(scriptDefinition = KotlinScriptDefinitionEx(ScriptTemplateWithArgs::class, scriptArgs)).use { repl ->
            repl.compileAndEval("val y = args[0].toInt()")
            repl.compileAndEval("val x = args[0].toInt()", ScriptArgsWithTypes(arrayOf(arrayOf("200")), arrayOf(Array<String>::class)))
            val result = repl.compileAndEval("x + y + args[0].toInt() + args[1].toInt()",
                                             ScriptArgsWithTypes(arrayOf(arrayOf("1", "2")), arrayOf(Array<String>::class)))
            assertEquals(303, result.resultValue)

            // TODO: decide if retaining last passed args to evaluator should be done (with the same stack that tracks current classloader,
            //       or if not passing in args goes back to the default or none.

            // test that we drop back to original args
            val result2 = repl.compileAndEval("args[0].toInt()")
            assertEquals(100, result2.resultValue)
        }
    }

    @Test
    fun testScriptDefinitionWithMapBindings() {
        val scriptArgs = ScriptArgsWithTypes(arrayOf(mapOf<String, Any?>("x" to 100, "y" to 50)), arrayOf(Map::class))
        SimplifiedRepl(scriptDefinition = KotlinScriptDefinitionEx(ScriptTemplateWithBindings::class, scriptArgs)).use { repl ->
            repl.compileAndEval("""val y = bindings.get("y") as Int""")
            repl.compileAndEval("""val x = bindings.get("x") as Int""")
            val result = repl.compileAndEval("""x + y + (bindings.get("z") as String).toInt()""",
                                             ScriptArgsWithTypes(arrayOf(mapOf<String, Any?>("z" to "3")), arrayOf(Map::class)))
            assertEquals(153, result.resultValue)
        }
    }
}
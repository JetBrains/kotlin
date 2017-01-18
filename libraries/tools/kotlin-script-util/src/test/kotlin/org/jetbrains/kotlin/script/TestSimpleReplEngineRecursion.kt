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
import org.jetbrains.kotlin.script.util.findClassJars
import org.jetbrains.kotlin.script.util.findKotlinCompilerJars
import org.junit.Test
import kotlin.test.assertEquals


class TestSimpleReplEngineRecursion {
    @Test
    fun testRecursingScriptsDifferentEngines() {
        val extraClasspath = findClassJars(SimplifiedRepl::class) +
                             findKotlinCompilerJars(false)

        SimplifiedRepl(additionalClasspath = extraClasspath).use { repl ->
            val outerEval = repl.compileAndEval("""
                 import org.jetbrains.kotlin.script.SimplifiedRepl
                 import org.jetbrains.kotlin.script.util.*

                 val extraClasspath =  findClassJars(SimplifiedRepl::class) +
                                       findKotlinCompilerJars(false)
                 val result = SimplifiedRepl(additionalClasspath = extraClasspath).use { repl ->
                    val innerEval = repl.compileAndEval("println(\"inner world\"); 100")
                    innerEval.resultValue
                 }
                 result
            """)
            assertEquals(100, outerEval.resultValue)
        }
    }

    @Test
    fun testRecursingScriptsSameEngines() {
        val extraClasspath = findClassJars(SimplifiedRepl::class) +
                             findKotlinCompilerJars(false)
        SimplifiedRepl(scriptDefinition = KotlinScriptDefinitionEx(TestRecursiveScriptContext::class, null),
                       additionalClasspath = extraClasspath,
                       sharedHostClassLoader = Thread.currentThread().contextClassLoader).apply {
            fallbackArgs = ScriptArgsWithTypes(arrayOf(this, mapOf<String, Any?>("x" to 100, "y" to 50)),
                                               arrayOf(SimplifiedRepl::class, Map::class))
        }.use { repl ->
            val outerEval = repl.compileAndEval("""
                 val x = bindings.get("x") as Int
                 val y = bindings.get("y") as Int
                 kotlinScript.compileAndEval("println(\"inner world\"); ${"$"}x+${"$"}y").resultValue
            """)
            assertEquals(150, outerEval.resultValue)
        }
    }
}

abstract class TestRecursiveScriptContext(val kotlinScript: SimplifiedRepl, val bindings: Map<String, Any?>)

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

package org.jetbrains.kotlin.script.resolver


import org.jetbrains.kotlin.cli.common.repl.ScriptArgsWithTypes
import org.jetbrains.kotlin.script.KotlinScriptDefinitionEx
import org.jetbrains.kotlin.script.ReplCompilerException
import org.jetbrains.kotlin.script.SimplifiedRepl
import org.jetbrains.kotlin.script.resolver.AnnotationTriggeredScriptDefinition
import org.junit.Test
import java.time.Instant
import kotlin.reflect.KClass
import kotlin.script.templates.standard.ScriptTemplateWithArgs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.junit.JUnitAsserter

class TestSimpleReplResolversAndImports {
    private val EMPTY_SCRIPT_ARGS: Array<out Any?> = arrayOf(emptyArray<String>())
    private val EMPTY_SCRIPT_ARGS_TYPES: Array<out KClass<out Any>> = arrayOf(Array<String>::class)

    @Test
    fun testWithoutDefaultImportsFails() {
        SimplifiedRepl(scriptDefinition = KotlinScriptDefinitionEx(ScriptTemplateWithArgs::class,
                                                                   ScriptArgsWithTypes(EMPTY_SCRIPT_ARGS, EMPTY_SCRIPT_ARGS_TYPES))).use { repl ->
            try {
                repl.compileAndEval("""val now = Instant.now()""")
                JUnitAsserter.fail("Expected compile error")
            }
            catch (ex: ReplCompilerException) {
                assertTrue("unresolved reference: Instant" in ex.message!!)
            }
        }
    }

    @Test
    fun testWithDefaultImports() {
        SimplifiedRepl(scriptDefinition = KotlinScriptDefinitionEx(ScriptTemplateWithArgs::class,
                                                                   ScriptArgsWithTypes(EMPTY_SCRIPT_ARGS, EMPTY_SCRIPT_ARGS_TYPES),
                                                                   defaultImports = listOf("java.time.*"))).use { repl ->
            val now = Instant.now()
            repl.compileAndEval("""val now = Instant.now()""")
            val result = repl.compileAndEval("""now""").resultValue as Instant
            assertTrue(result >= now)
        }
    }

    fun makeConfigurableEngine(defaultImports: List<String> = emptyList()): SimplifiedRepl =
            SimplifiedRepl(scriptDefinition = AnnotationTriggeredScriptDefinition(
                    "varargTemplateWithMavenResolving",
                    ScriptTemplateWithArgs::class,
                    ScriptArgsWithTypes(EMPTY_SCRIPT_ARGS, EMPTY_SCRIPT_ARGS_TYPES),
                    defaultImports = defaultImports))

    @Test
    fun testConfigurableResolversEmpty() {
        makeConfigurableEngine().use { repl ->
            repl.compileAndEval("""val x = 10 + 100""")
            assertEquals(110, repl.compileAndEval("""x""").resultValue)
        }
    }

    @Test
    fun testConfigurableResolversFailsWithoutCorrectImport() {
        makeConfigurableEngine().use { repl ->
            try {
                repl.compileAndEval("""val now = Instant.now()""")
                JUnitAsserter.fail("Expected compile error")
            }
            catch (ex: ReplCompilerException) {
                assertTrue("unresolved reference: Instant" in ex.message!!)
            }
        }
    }

    @Test
    fun testConfigurableResolversWithDefaultImports() {
        makeConfigurableEngine(defaultImports = listOf("java.time.*")).use { repl ->
            val now = Instant.now()
            repl.compileAndEval("""val now = Instant.now()""")
            val result = repl.compileAndEval("""now""").resultValue as Instant
            assertTrue(result >= now)
        }
    }
}
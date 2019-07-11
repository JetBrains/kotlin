/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jsr223

import org.jetbrains.kotlin.cli.common.repl.KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY
import org.jetbrains.kotlin.cli.common.repl.KOTLIN_SCRIPT_STATE_BINDINGS_KEY
import javax.script.Bindings
import javax.script.ScriptEngine
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.ScriptEvaluationConfiguration
import kotlin.script.experimental.api.refineConfiguration
import kotlin.script.experimental.api.refineConfigurationBeforeEvaluate
import kotlin.script.experimental.jvmhost.jsr223.configureProvidedPropertiesFromJsr223Context
import kotlin.script.experimental.jvmhost.jsr223.importAllBindings
import kotlin.script.experimental.jvmhost.jsr223.jsr223
import kotlin.script.templates.standard.ScriptTemplateWithBindings

@Suppress("unused")
@KotlinScript(
    compilationConfiguration = KotlinJsr223DefaultScriptCompilationConfiguration::class,
    evaluationConfiguration = KotlinJsr223DefaultScriptEvaluationConfiguration::class
)
abstract class KotlinJsr223DefaultScript(val jsr223Bindings: Bindings) : ScriptTemplateWithBindings(jsr223Bindings) {

    private val myEngine: ScriptEngine? get() = bindings[KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY]?.let { it as? ScriptEngine }

    private inline fun <T> withMyEngine(body: (ScriptEngine) -> T): T =
        myEngine?.let(body) ?: throw IllegalStateException("Script engine for `eval` call is not found")

    fun eval(script: String, newBindings: Bindings): Any? =
        withMyEngine {
            val savedState =
                newBindings[KOTLIN_SCRIPT_STATE_BINDINGS_KEY]?.takeIf { it === this.jsr223Bindings[KOTLIN_SCRIPT_STATE_BINDINGS_KEY] }
                    ?.apply {
                        newBindings[KOTLIN_SCRIPT_STATE_BINDINGS_KEY] = null
                    }
            val res = it.eval(script, newBindings)
            savedState?.apply {
                newBindings[KOTLIN_SCRIPT_STATE_BINDINGS_KEY] = savedState
            }
            res
        }

    fun eval(script: String): Any? =
        withMyEngine {
            val savedState = jsr223Bindings.remove(KOTLIN_SCRIPT_STATE_BINDINGS_KEY)
            val res = it.eval(script, jsr223Bindings)
            savedState?.apply {
                jsr223Bindings[KOTLIN_SCRIPT_STATE_BINDINGS_KEY] = savedState
            }
            res
        }

    fun createBindings(): Bindings = withMyEngine { it.createBindings() }
}

object KotlinJsr223DefaultScriptCompilationConfiguration : ScriptCompilationConfiguration(
    {
        refineConfiguration {
            beforeCompiling(::configureProvidedPropertiesFromJsr223Context)
        }
        jsr223 {
            importAllBindings(true)
        }
    }
)

object KotlinJsr223DefaultScriptEvaluationConfiguration : ScriptEvaluationConfiguration(
    {
        refineConfigurationBeforeEvaluate(::configureProvidedPropertiesFromJsr223Context)
    }
)

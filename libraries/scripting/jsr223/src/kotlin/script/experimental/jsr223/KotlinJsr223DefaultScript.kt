/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jsr223

import org.jetbrains.kotlin.cli.common.repl.KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY
import org.jetbrains.kotlin.cli.common.repl.KOTLIN_SCRIPT_STATE_BINDINGS_KEY
import javax.script.Bindings
import javax.script.ScriptContext
import javax.script.ScriptEngine
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvmhost.jsr223.getScriptContext
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
            beforeCompiling { context ->
                val jsr223context = context.compilationConfiguration[ScriptCompilationConfiguration.jsr223.getScriptContext]?.invoke()
                if (jsr223context != null && context.compilationConfiguration[ScriptCompilationConfiguration.jsr223.importAllBindings] == true) {
                    val updatedProperties =
                        context.compilationConfiguration[ScriptCompilationConfiguration.providedProperties]?.toMutableMap() ?: hashMapOf()
                    val allBindings = (jsr223context.getBindings(ScriptContext.GLOBAL_SCOPE).toMutableMap() ?: hashMapOf()).apply {
                        val engineBindings = jsr223context.getBindings(ScriptContext.ENGINE_SCOPE)
                        if (engineBindings != null)
                            putAll(engineBindings)
                    }
                    for ((k, v) in allBindings) {
                        // only adding bindings that are not already defined
                        if (!updatedProperties.containsKey(k)) {
                            // TODO: add only valid names
                            // TODO: find out how it's implemented in other jsr223 engines for typed languages, since this approach prevent certain usage scenarios, e.g. assigning back value of a "sibling" type
                            updatedProperties[k] = KotlinType(v::class)
                        }
                    }
                    ScriptCompilationConfiguration(context.compilationConfiguration) {
                        providedProperties(updatedProperties)
                    }.asSuccess()
                } else context.compilationConfiguration.asSuccess()
            }
        }
        jsr223 {
            importAllBindings(true)
        }
    }
)

object KotlinJsr223DefaultScriptEvaluationConfiguration : ScriptEvaluationConfiguration(
    {
        refineConfigurationBeforeEvaluate { context ->
            val jsr223context = context.evaluationConfiguration[ScriptEvaluationConfiguration.jsr223.getScriptContext]?.invoke()
            val knownProperties = context.compiledScript.compilationConfiguration[ScriptCompilationConfiguration.providedProperties]
            if (jsr223context != null && knownProperties != null && knownProperties.isNotEmpty()) {
                val updatedProperties =
                    context.evaluationConfiguration[ScriptEvaluationConfiguration.providedProperties]?.toMutableMap() ?: hashMapOf()
                val engineBindings = jsr223context.getBindings(ScriptContext.ENGINE_SCOPE)
                val globalBindings = jsr223context.getBindings(ScriptContext.GLOBAL_SCOPE)
                for (prop in knownProperties) {
                    val v = when {
                        engineBindings?.containsKey(prop.key) == true -> engineBindings[prop.key]
                        globalBindings?.containsKey(prop.key) == true -> globalBindings[prop.key]
                        else -> return@refineConfigurationBeforeEvaluate ResultWithDiagnostics.Failure("Property ${prop.key} is not found in the bindings".asErrorDiagnostics())
                    }
                    updatedProperties[prop.key] = v
                }
                ScriptEvaluationConfiguration(context.evaluationConfiguration) {
                    providedProperties(updatedProperties)
                }.asSuccess()
            } else context.evaluationConfiguration.asSuccess()
        }
    }
)

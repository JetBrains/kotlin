/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223

import javax.script.ScriptContext
import kotlin.script.experimental.api.*

fun configureProvidedPropertiesFromJsr223Context(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    val jsr223context = context.compilationConfiguration[ScriptCompilationConfiguration.jsr223.getScriptContext]?.invoke()
    return if (jsr223context != null && context.compilationConfiguration[ScriptCompilationConfiguration.jsr223.importAllBindings] == true) {
        val updatedProperties =
            context.compilationConfiguration[ScriptCompilationConfiguration.providedProperties]?.toMutableMap() ?: hashMapOf()
        val allBindings = (jsr223context.getBindings(ScriptContext.GLOBAL_SCOPE)?.toMutableMap() ?: hashMapOf()).apply {
            val engineBindings = jsr223context.getBindings(ScriptContext.ENGINE_SCOPE)
            if (engineBindings != null)
                putAll(engineBindings)
        }
        for ((k, v) in allBindings) {
            // only adding bindings that are not already defined and also skip local classes
            if (!updatedProperties.containsKey(k) && v::class.qualifiedName != null) {
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

fun configureProvidedPropertiesFromJsr223Context(context: ScriptEvaluationConfigurationRefinementContext): ResultWithDiagnostics<ScriptEvaluationConfiguration> {
    val jsr223context = context.evaluationConfiguration[ScriptEvaluationConfiguration.jsr223.getScriptContext]?.invoke()
    val knownProperties = context.compiledScript.compilationConfiguration[ScriptCompilationConfiguration.providedProperties]
    return if (jsr223context != null && knownProperties != null && knownProperties.isNotEmpty()) {
        val updatedProperties =
            context.evaluationConfiguration[ScriptEvaluationConfiguration.providedProperties]?.toMutableMap() ?: hashMapOf()
        val engineBindings = jsr223context.getBindings(ScriptContext.ENGINE_SCOPE)
        val globalBindings = jsr223context.getBindings(ScriptContext.GLOBAL_SCOPE)
        for (prop in knownProperties) {
            val v = when {
                engineBindings?.containsKey(prop.key) == true -> engineBindings[prop.key]
                globalBindings?.containsKey(prop.key) == true -> globalBindings[prop.key]
                else -> return ResultWithDiagnostics.Failure("Property ${prop.key} is not found in the bindings".asErrorDiagnostics())
            }
            updatedProperties[prop.key] = v
        }
        ScriptEvaluationConfiguration(context.evaluationConfiguration) {
            providedProperties(updatedProperties)
        }.asSuccess()
    } else context.evaluationConfiguration.asSuccess()
}



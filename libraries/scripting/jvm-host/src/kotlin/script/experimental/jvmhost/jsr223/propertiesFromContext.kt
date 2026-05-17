/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223

import org.jetbrains.kotlin.name.Name
import javax.script.ScriptContext
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.impl._isSyntheticSnippet
import kotlin.script.experimental.util.PropertiesCollection

private val ScriptCompilationConfigurationKeys.exposedBindings by PropertiesCollection.key<Map<String, KotlinType>>() // external variables
private val ScriptCompilationConfigurationKeys.rootBindingsConfigured by PropertiesCollection.key(false) // bindings variable

private const val SYNTHETIC_SNIPPET_PREFIX = "\$\$synthetic_jsr223_"

fun configureExposedJsr223Context(context: ScriptConfigurationRefinementContext): ResultWithDiagnostics<ScriptCompilationConfiguration> {
    if (context.compilationConfiguration[ScriptCompilationConfiguration.jsr223.getScriptContext]?.invoke() == null)
        return context.compilationConfiguration.asSuccess()

    return ScriptCompilationConfiguration(context.compilationConfiguration) {
        implicitReceivers(ScriptContext::class)
    }.asSuccess()
}

fun generateBindingSnippetIfNeeded(context: ScriptConfigurationRefinementContext):
        ResultWithDiagnostics<Pair<ScriptCompilationConfiguration, SourceCode?>>
{
    val jsr223context =
        context.compilationConfiguration[ScriptCompilationConfiguration.jsr223.getScriptContext]?.invoke()
            ?: return (context.compilationConfiguration to null).asSuccess()

    var bindingsSnippet = ""

    if (context.compilationConfiguration[ScriptCompilationConfiguration.rootBindingsConfigured] != true) {
        bindingsSnippet = """
                val bindings: javax.script.Bindings = getBindings(javax.script.ScriptContext.ENGINE_SCOPE)
                
            """
    }

    val knownBindings =
        context.compilationConfiguration[ScriptCompilationConfiguration.exposedBindings] ?: hashMapOf()
    val newBindings = hashMapOf<String, KotlinType>()

    if (
        context.compilationConfiguration[ScriptCompilationConfiguration.jsr223.importAllBindings] == true &&
        context.compilationConfiguration[ScriptCompilationConfiguration.repl._isSyntheticSnippet] != true
    ) {
        val allBindings = (jsr223context.getBindings(ScriptContext.GLOBAL_SCOPE)?.toMutableMap() ?: hashMapOf()).apply {
            val engineBindings = jsr223context.getBindings(ScriptContext.ENGINE_SCOPE)
            if (engineBindings != null)
                putAll(engineBindings)
        }
        for ((k, v) in allBindings) {
            // only adding bindings that are not already defined and also skip local classes
            if (!knownBindings.containsKey(k) && (v == null || v::class.qualifiedName != null) && Name.isValidIdentifier(k)) {
                // TODO: find out how it's implemented in other jsr223 engines for typed languages, since this approach prevent certain usage scenarios, e.g. assigning back value of a "sibling" type
                newBindings[k] = if (v == null) KotlinType(Any::class, isNullable = true) else KotlinType(v::class)
            }
        }

        newBindings.forEach { (name, type) ->
            bindingsSnippet +=
                """
                    @Suppress("UNCHECKED_CAST")
                    var $name: ${type.typeName}
                        get() = bindings["$name"] as ${type.typeName}
                        set(value) { bindings["$name"] = value }
                        
                """.trimIndent()
        }
    }
    val source = bindingsSnippet.takeIf { it.isNotBlank() }?.toScriptSource(SYNTHETIC_SNIPPET_PREFIX + context.script.name)
    return (
            context.compilationConfiguration.with {
                rootBindingsConfigured(true)
                exposedBindings(knownBindings + newBindings)
            } to source).asSuccess()
}

fun configureExposedJsr223Context(context: ScriptEvaluationConfigurationRefinementContext): ResultWithDiagnostics<ScriptEvaluationConfiguration> {
    val jsr223context = context.evaluationConfiguration[ScriptEvaluationConfiguration.jsr223.getScriptContext]?.invoke()
        ?: return context.evaluationConfiguration.asSuccess() // likely an error

    return context.evaluationConfiguration.with {
        implicitReceivers(jsr223context)
    }.asSuccess()
}



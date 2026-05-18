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

// Engine-internal binding keys that must not be exposed as snippet properties.
private val ENGINE_INTERNAL_BINDING_KEYS = setOf(
    "kotlin.script.state",
    "kotlin.script.engine",
)

/**
 * Returns a valid Kotlin identifier for a JSR-223 binding name, or null if the name cannot be exposed.
 * All-whitespace names are converted to underscores; all other names must pass Name.isValidIdentifier.
 */
private fun encodeBindingNameToKotlinIdentifier(name: String): String? {
    if (name.isEmpty()) return null
    if (name.all { it == ' ' }) return "_".repeat(name.length)
    return if (Name.isValidIdentifier(name)) name else null
}

/**
 * Returns true if [qualifiedName] is a dot-separated chain of identifiers that the Kotlin parser
 * will accept as a type reference. Filters out synthetic / anonymous class names produced for
 * indy lambdas (e.g. `Foo$$Lambda$1`, `MyKt$f$lambda$1`, names containing `/` or `<`) which
 * have non-null `KClass.qualifiedName` on some JDKs but cannot be embedded into source.
 */
private fun isParseableKotlinQualifiedName(qualifiedName: String): Boolean {
    if (qualifiedName.isEmpty()) return false
    return qualifiedName.split('.').all { Name.isValidIdentifier(it) }
}

/** Escapes a string for embedding inside a Kotlin regular string literal ("..."). */
private fun escapeForKotlinStringLiteral(s: String): String = buildString {
    for (c in s) {
        when (c) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '$' -> append("\\u0024")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
    }
}

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

    // `val bindings` is declared in every synthetic snippet so that each eval's synthetic snippet
    // captures the ScriptContext active at that eval's evaluation time. This ensures that property
    // getters (e.g. `var z: Int`) in subsequent synthetic snippets resolve `bindings` from their own
    // class rather than from synthetic-snippet-0, avoiding stale-context bugs when eval is called
    // with a custom Bindings argument.
    bindingsSnippet += "val bindings: javax.script.Bindings = getBindings(javax.script.ScriptContext.ENGINE_SCOPE)\n\n"

    if (context.compilationConfiguration[ScriptCompilationConfiguration.rootBindingsConfigured] != true) {
        // Declare eval() helpers only once (in the first synthetic snippet). They reference
        // snippet-0's `bindings` which holds the default-context ENGINE_SCOPE — correct for
        // eval-in-eval because the default state is what needs to be saved/restored.
        // Avoid @InlineOnly stdlib operators: use explicit null checks and .put() instead of [] = .
        bindingsSnippet += """
fun eval(script: String): Any? {
    @Suppress("UNCHECKED_CAST")
    val __engine = bindings["kotlin.script.engine"] as? javax.script.ScriptEngine
        ?: throw IllegalStateException("Script engine for `eval` call is not found")
    val savedState = bindings.remove("kotlin.script.state")
    val result = __engine.eval(script, bindings)
    if (savedState != null) bindings.put("kotlin.script.state", savedState)
    return result
}

fun eval(script: String, newBindings: javax.script.Bindings): Any? {
    @Suppress("UNCHECKED_CAST")
    val __engine = bindings["kotlin.script.engine"] as? javax.script.ScriptEngine
        ?: throw IllegalStateException("Script engine for `eval` call is not found")
    val sameState = newBindings["kotlin.script.state"]
    val savedState: Any? = if (sameState != null && sameState === bindings["kotlin.script.state"]) {
        newBindings.remove("kotlin.script.state")
        sameState
    } else null
    val result = __engine.eval(script, newBindings)
    if (savedState != null) newBindings.put("kotlin.script.state", savedState)
    return result
}

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
            if (knownBindings.containsKey(k)) continue
            if (k in ENGINE_INTERNAL_BINDING_KEYS) continue
            if (encodeBindingNameToKotlinIdentifier(k) == null) continue
            val qn = v?.let { it::class.qualifiedName }
            if (v != null && (qn == null || !isParseableKotlinQualifiedName(qn))) {
                // Skip values whose type cannot be embedded as a Kotlin type reference in source
                // (lambdas under -Xlambdas=indy, local/anonymous classes, etc.). Such bindings remain
                // accessible via `bindings["..."]` from user code, just not as auto-generated properties.
                continue
            }
            // TODO: find out how it's implemented in other jsr223 engines for typed languages, since this approach prevent certain usage scenarios, e.g. assigning back value of a "sibling" type
            newBindings[k] = if (v == null) KotlinType(Any::class, isNullable = true) else KotlinType(v::class)
        }

        newBindings.forEach { (name, type) ->
            val encodedName = encodeBindingNameToKotlinIdentifier(name)!!
            val safeKey = escapeForKotlinStringLiteral(name)
            bindingsSnippet +=
                """
                    @Suppress("UNCHECKED_CAST")
                    var $encodedName: ${type.typeName}
                        get() = bindings["$safeKey"] as ${type.typeName}
                        set(value) { bindings.put("$safeKey", value) }

                """.trimIndent() + "\n"
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

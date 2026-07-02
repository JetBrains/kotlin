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
private fun encodeBindingNameToKotlinIdentifier(name: String): String? =
    when {
        name.isEmpty() -> null
        name.all { it == ' ' } -> "`" + "_".repeat(name.length) + "`"
        Name.isValidIdentifier(name) -> name
        name.contains("`") -> null
        else -> "`$name`"
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

    // Add `ScriptContext` as an implicit receiver, but only once. This refinement runs `beforeCompiling`
    // on every snippet, and the engine threads (and mutates) a single `ScriptCompilationConfiguration`
    // across evals; a freshly-created nested-eval REPL state is even seeded from that threaded config
    // (see `KotlinJsr223ScriptEngineImpl.createState` and the generated `eval(...)` helper that resets
    // the engine state before re-entering). Appending unconditionally would let the receiver list grow
    // across evals, so a snippet's `$$eval` would take N `ScriptContext` parameters while the evaluator
    // always passes exactly one — surfacing as `IllegalArgumentException: wrong number of arguments` in
    // the eval-in-eval scenario (`KotlinJsr223ScriptEngineIT.testSimpleEvalInEval`). Adding it
    // idempotently keeps the count at one in every (including nested) state.
    val alreadyPresent =
        context.compilationConfiguration[ScriptCompilationConfiguration.implicitReceivers]
            ?.contains(KotlinType(ScriptContext::class)) == true
    if (alreadyPresent) return context.compilationConfiguration.asSuccess()

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
    // The set of bindings exposed as typed properties after this snippet. Starts from the
    // previously-known set and, when `importAllBindings` is on, is recomputed below and written back
    // into `exposedBindings` so the next snippet can diff against it (add / retype / remove).
    var updatedBindings: Map<String, KotlinType> = knownBindings

    if (
        context.compilationConfiguration[ScriptCompilationConfiguration.jsr223.importAllBindings] == true &&
        context.compilationConfiguration[ScriptCompilationConfiguration.repl._isSyntheticSnippet] != true
    ) {
        val allBindings = (jsr223context.getBindings(ScriptContext.GLOBAL_SCOPE)?.toMutableMap() ?: hashMapOf()).apply {
            val engineBindings = jsr223context.getBindings(ScriptContext.ENGINE_SCOPE)
            if (engineBindings != null)
                putAll(engineBindings)
        }
        // Current bindings that can be exposed as typed properties, with the type inferred from the
        // present runtime value. Names that aren't usable identifiers, or whose value type can't be
        // embedded as a Kotlin type reference (lambdas under -Xlambdas=indy, local/anonymous classes,
        // ...), are left out — they stay reachable via `bindings["..."]`, just not as typed properties.
        val currentBindings = LinkedHashMap<String, KotlinType>()
        for ([k, v] in allBindings) {
            if (k in ENGINE_INTERNAL_BINDING_KEYS) continue
            if (encodeBindingNameToKotlinIdentifier(k) == null) continue
            val qn = v?.let { it::class.qualifiedName }
            if (v != null && (qn == null || !isParseableKotlinQualifiedName(qn))) continue
            // TODO: find out how it's implemented in other jsr223 engines for typed languages, since this approach prevent certain usage scenarios, e.g. assigning back value of a "sibling" type
            currentBindings[k] = if (v == null) KotlinType(Any::class, isNullable = true) else KotlinType(v::class)
        }

        // Q10d — (re)emit a typed accessor for each binding that is new or whose type changed since it
        // was last exposed (KotlinType compares by type name + nullability). A retyped binding gets a
        // fresh accessor that shadows the stale one in subsequent snippets, so e.g. rebinding an Int as
        // a String stops the old `var x: Int` (whose `as Int` getter would then fail to compile / throw
        // a ClassCastException against the new value) from resolving.
        for ([name, type] in currentBindings) {
            if (knownBindings[name] == type) continue
            val encodedName = encodeBindingNameToKotlinIdentifier(name)!!
            val safeKey = escapeForKotlinStringLiteral(name)
            // Render the source-level type with its nullability marker: KotlinType.typeName strips the
            // trailing `?`, so a `null`-valued binding (typed Any?) would otherwise emit `var x: kotlin.Any`
            // with a `as kotlin.Any` getter cast that NPEs on the null value, bypassing the user's own
            // null-safety (see plugins/scripting/.ai/target/90-open-questions.md Q17).
            val renderedType = if (type.isNullable) "${type.typeName}?" else type.typeName
            bindingsSnippet +=
                """
                    @Suppress("UNCHECKED_CAST")
                    var $encodedName: $renderedType
                        get() = bindings["$safeKey"] as $renderedType
                        set(value) { bindings.put("$safeKey", value) }

                """.trimIndent() + "\n"
        }

        // Q10c — a binding that was exposed as a typed property before but is no longer present
        // (removed from the bindings, or absent in the current eval's context) gets a shadowing
        // accessor that keeps the previously declared type — so existing user code still type-checks —
        // but throws a clear diagnostic at access time instead of the cryptic `null cannot be cast to
        // non-null type ...` NPE from the stale getter. Re-adding the binding later emits a fresh typed
        // accessor (it is new relative to the recomputed set) which shadows this one again.
        for (removedName in knownBindings.keys - currentBindings.keys) {
            val encodedName = encodeBindingNameToKotlinIdentifier(removedName) ?: continue
            val safeKey = escapeForKotlinStringLiteral(removedName)
            val prevType = knownBindings.getValue(removedName)
            val renderedType = if (prevType.isNullable) "${prevType.typeName}?" else prevType.typeName
            bindingsSnippet +=
                """
                    @Suppress("UNCHECKED_CAST")
                    var $encodedName: $renderedType
                        get() = throw java.util.NoSuchElementException("JSR-223 binding \"$safeKey\" is no longer available")
                        set(value) { bindings.put("$safeKey", value) }

                """.trimIndent() + "\n"
        }

        updatedBindings = currentBindings
    }
    val source = bindingsSnippet.takeIf { it.isNotBlank() }?.toScriptSource(SYNTHETIC_SNIPPET_PREFIX + context.script.name)
    return (
            context.compilationConfiguration.with {
                rootBindingsConfigured(true)
                exposedBindings(updatedBindings)
            } to source).asSuccess()
}

fun configureExposedJsr223Context(context: ScriptEvaluationConfigurationRefinementContext): ResultWithDiagnostics<ScriptEvaluationConfiguration> {
    val jsr223context = context.evaluationConfiguration[ScriptEvaluationConfiguration.jsr223.getScriptContext]?.invoke()
        ?: return context.evaluationConfiguration.asSuccess() // likely an error

    return context.evaluationConfiguration.with {
        implicitReceivers(jsr223context)
    }.asSuccess()
}

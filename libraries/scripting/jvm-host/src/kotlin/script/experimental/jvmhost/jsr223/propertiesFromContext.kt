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
import kotlin.script.templates.standard.ScriptTemplateWithBindings

private val ScriptCompilationConfigurationKeys.exposedBindings by PropertiesCollection.key<Map<String, KotlinType>>() // external variables
private val ScriptCompilationConfigurationKeys.rootBindingsConfigured by PropertiesCollection.key(false) // bindings variable

private const val SYNTHETIC_SNIPPET_PREFIX = "\$\$synthetic_jsr223_"

// Engine-internal binding keys that must not be exposed as snippet properties.
private val ENGINE_INTERNAL_BINDING_KEYS = setOf(
    "kotlin.script.state",
    "kotlin.script.engine",
)

/**
 * Characters that cannot appear in a Kotlin declaration name under *any* quoting: the JVM member-name
 * characters `FirJvmNamesChecker`/`JvmSimpleNameBacktickChecker` reject outright (`. ; [ ] / < > : \`),
 * plus the backtick itself (can't nest inside a backtick-quoted identifier) and raw line breaks (can't
 * appear inside a source-level identifier at all). A name containing any of these must go through
 * [encodeBindingNameToMarkerIdentifier]; every other name — including one with spaces, `$`, non-ASCII
 * characters, or the JVM-"dangerous" `? * " | %` — is JVM- and Kotlin-legal as a backtick-quoted
 * identifier (see [encodeBindingNameToKotlinIdentifier]).
 */
private val NEEDS_MARKER_ENCODING_CHARS: Set<Char> =
    setOf('.', ';', '[', ']', '/', '<', '>', ':', '\\', '`', '\n', '\r')

private fun Char.isAsciiIdentifierChar(): Boolean =
    this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this == '_'

/**
 * Encodes a binding name that cannot be a plain/backtick-quoted identifier into a plain Kotlin
 * identifier by replacing every problematic character with a `__u<hex>__` marker carrying its Unicode
 * code point (e.g. `a.b` -> `a__u002e__b`, `c:d` -> `c__u003a__d`, `☺` -> `__u263a__`) — the same,
 * single, uniform rule for every character, deliberately close to (but not literally) the familiar
 * `\uXXXX` escape convention from Kotlin/Java/JS string literals: an actual backslash can't be used
 * here because `\` is itself one of the [NEEDS_MARKER_ENCODING_CHARS] (a hard JVM-invalid character),
 * so reusing it would just reintroduce the very character this scheme exists to eliminate. ASCII
 * letters/digits/underscores are kept verbatim, so the result is a valid identifier that needs no
 * backtick-quoting.
 *
 * Only injectivity is required (distinct raw names must map to distinct identifiers) — the generated
 * getter/setter reaches the value through the raw binding key, not by decoding this identifier — which
 * holds for all names except the pathological case of a raw name spelled exactly like an emitted
 * marker (e.g. a binding literally named `a__u002e__b`); such names are out of scope for this prototype.
 */
private fun encodeBindingNameToMarkerIdentifier(name: String): String {
    val sb = StringBuilder(name.length + 8)
    for (ch in name) {
        if (ch.isAsciiIdentifierChar()) {
            sb.append(ch)
        } else {
            sb.append("__u").append(ch.code.toString(16).padStart(4, '0')).append("__")
        }
    }
    // A leading digit is not a legal identifier start; markers begin with `_`, so this only fires when
    // the name starts with a kept digit (e.g. `1.2` -> `1__u002e__2`).
    if (sb.isNotEmpty() && sb[0] in '0'..'9') sb.insert(0, '_')
    return sb.toString()
}

/**
 * Returns a Kotlin identifier that references a JSR-223 binding [name] from snippet source, or null if
 * the name is empty. Plain identifiers are used verbatim; any other name is either backtick-quoted
 * (verbatim) or, if it contains a character from [NEEDS_MARKER_ENCODING_CHARS], reversibly encoded via
 * [encodeBindingNameToMarkerIdentifier].
 *
 * A backtick-quoted name is *not* safe to declare with a hardcoded `get()`/`set()` accessor block in
 * this feature's generated snippet: every snippet also declares `val bindings = getBindings(...)`, an
 * implicit-context-receiver call, and a backtick-quoted property whose accessors are hand-written
 * makes the K2 REPL/script-snippet compiler fail with a spurious "Property getter or setter expected"
 * parse error as soon as both are present in the same live REPL session (reproduced only through the
 * real incremental JSR-223/REPL pipeline — an equivalent one-shot `.kts` compile of the same source
 * does not reproduce it, so the trigger is specific to REPL-snippet statement-sequence parsing, not to
 * backtick names or implicit receivers in general). [generateBindingSnippetIfNeeded] sidesteps this by
 * declaring every backtick-quoted property with a delegate (`by ...`) instead of accessor bodies — a
 * delegate expression is consumed by `parsePropertyDelegateOrAssignment()`, so the accessor-parsing
 * code path that misfires is never reached for these properties. Plain (non-backtick) identifiers keep
 * using ordinary `get()`/`set()`, since they were never part of the failure.
 */
private fun encodeBindingNameToKotlinIdentifier(name: String): String? =
    when {
        name.isEmpty() -> null
        // A plain Kotlin identifier: only ASCII letters/digits/underscores, not starting with a digit,
        // and not a reserved all-underscore name. Safe to emit verbatim.
        name.all { it.isAsciiIdentifierChar() } && name[0] !in '0'..'9' && name.any { it != '_' } -> name
        // Contains a character that can't survive even backtick-quoting — reversibly encode into a
        // marker identifier instead.
        name.any { it in NEEDS_MARKER_ENCODING_CHARS } -> encodeBindingNameToMarkerIdentifier(name)
        // Everything else (leading-digit / all-underscore ASCII names, spaces, `$`, non-ASCII
        // characters, the JVM-"dangerous" `? * " | %`, ...) is safe to emit as a backtick-quoted
        // identifier, declared with a delegate — see the doc comment above.
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

    // The implicit receivers exposed to every JSR-223 snippet: `ScriptContext` (the JSR-223-specific
    // scopes/attributes API) and `ScriptTemplateWithBindings` (the K1-era `bindings`-only shape), added
    // side by side. `ScriptTemplateWithBindings` only exposes a `bindings` property, and `ScriptContext`
    // exposes JSR-223-specific methods (`getBindings`, `getAttribute`, `getWriter`, ...), so there's no
    // member-name collision between the two receivers under normal use.
    //
    // This list is deliberately computed *here*, inside the function and after the `getScriptContext`
    // guard above, rather than as a top-level property: a top-level `listOf(ScriptContext::class, ...)`
    // would be evaluated eagerly by this file's static initializer as soon as this function is first
    // invoked — including for the early-return (non-JSR-223) branch above — forcing `javax.script.*`
    // classes to load even for plain, non-JSR-223 script compilation (e.g. `MainKtsScriptDefinition`
    // wires this same callback unconditionally). That previously surfaced as a spurious
    // `NoClassDefFoundError: javax/script/ScriptContext` when compiling ordinary `.main.kts` scripts
    // that never touch JSR-223 at all.
    val requiredImplicitReceivers = listOf(ScriptContext::class, ScriptTemplateWithBindings::class)

    // Add the required implicit receivers, but only once each. This refinement runs `beforeCompiling`
    // on every snippet, and the engine threads (and mutates) a single `ScriptCompilationConfiguration`
    // across evals; a freshly-created nested-eval REPL state is even seeded from that threaded config
    // (see `KotlinJsr223ScriptEngineImpl.createState` and the generated `eval(...)` helper that resets
    // the engine state before re-entering). Appending unconditionally would let the receiver list grow
    // across evals, so a snippet's `$$eval` would take N receiver parameters of each kind while the
    // evaluator always passes exactly one of each — surfacing as `IllegalArgumentException: wrong
    // number of arguments` in the eval-in-eval scenario (`KotlinJsr223ScriptEngineIT.testSimpleEvalInEval`).
    // Adding them idempotently keeps the count at one (per receiver type) in every (including nested) state.
    val existingReceivers = context.compilationConfiguration[ScriptCompilationConfiguration.implicitReceivers].orEmpty()
    val missingReceivers = requiredImplicitReceivers.filter { KotlinType(it) !in existingReceivers }
    if (missingReceivers.isEmpty()) return context.compilationConfiguration.asSuccess()

    return ScriptCompilationConfiguration(context.compilationConfiguration) {
        implicitReceivers(*missingReceivers.toTypedArray())
    }.asSuccess()
}

/**
 * Renders the `var $encodedName: $renderedType ...` declaration for one exposed (or [removed]) binding.
 * A backtick-quoted [encodedName] is declared with a [__Jsr223BindingDelegate] (`by ...`) instead of a
 * hardcoded `get()`/`set()` — see the doc comment on [encodeBindingNameToKotlinIdentifier] for why.
 */
private fun renderBindingProperty(encodedName: String, renderedType: String, safeKey: String, removed: Boolean): String =
    if (encodedName.startsWith("`")) {
        """
            var $encodedName: $renderedType by __Jsr223BindingDelegate<$renderedType>(bindings, "$safeKey"${if (removed) ", removed = true" else ""})

        """.trimIndent() + "\n"
    } else if (!removed) {
        """
            @Suppress("UNCHECKED_CAST")
            var $encodedName: $renderedType
                get() = bindings["$safeKey"] as $renderedType
                set(value) { bindings.put("$safeKey", value) }

        """.trimIndent() + "\n"
    } else {
        """
            @Suppress("UNCHECKED_CAST")
            var $encodedName: $renderedType
                get() = throw java.util.NoSuchElementException("JSR-223 binding \"$safeKey\" is no longer available")
                set(value) { bindings.put("$safeKey", value) }

        """.trimIndent() + "\n"
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

// A property delegate used (only) for backtick-quoted binding properties — see the doc comment on
// [encodeBindingNameToKotlinIdentifier] for why these can't be declared with a hardcoded get()/set().
// [removed] renders the same "no longer available" diagnostic that a removed binding's shadowing
// accessor used to throw from its getter.
class __Jsr223BindingDelegate<T>(private val bindings: javax.script.Bindings, private val key: String, private val removed: Boolean = false) {
    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T {
        if (removed) throw java.util.NoSuchElementException("JSR-223 binding \"${'$'}key\" is no longer available")
        return bindings[key] as T
    }
    operator fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: T) { bindings.put(key, value) }
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

        // (re)emit a typed accessor for each binding that is new or whose type changed since it
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
            bindingsSnippet += renderBindingProperty(encodedName, renderedType, safeKey, removed = false)
        }

        // a binding that was exposed as a typed property before but is no longer present
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
            bindingsSnippet += renderBindingProperty(encodedName, renderedType, safeKey, removed = true)
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

// A trivial concrete subclass, since `ScriptTemplateWithBindings` itself is abstract. It wraps the
// same (live, mutable) `Bindings` map instance already backing `ScriptContext`'s `ENGINE_SCOPE`, so
// there's no separate synchronization needed between the two receivers' views of the data.
private class Jsr223ScriptTemplateWithBindings(bindings: Map<String, Any?>) : ScriptTemplateWithBindings(bindings)

fun configureExposedJsr223Context(context: ScriptEvaluationConfigurationRefinementContext): ResultWithDiagnostics<ScriptEvaluationConfiguration> {
    val jsr223context = context.evaluationConfiguration[ScriptEvaluationConfiguration.jsr223.getScriptContext]?.invoke()
        ?: return context.evaluationConfiguration.asSuccess() // likely an error

    // Order matches the order the corresponding types were added in `configureExposedJsr223Context`
    // (compile-time overload above): `ScriptContext` first, then `ScriptTemplateWithBindings`.
    val engineBindings = jsr223context.getBindings(ScriptContext.ENGINE_SCOPE) ?: emptyMap<String, Any?>()
    return context.evaluationConfiguration.with {
        implicitReceivers(jsr223context, Jsr223ScriptTemplateWithBindings(engineBindings))
    }.asSuccess()
}

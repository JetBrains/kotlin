/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.jsr223

import javax.script.ScriptContext
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.impl._isSyntheticSnippet
import kotlin.script.experimental.jvm.jsr223.base.KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY
import kotlin.script.experimental.jvm.jsr223.base.KOTLIN_SCRIPT_STATE_BINDINGS_KEY
import kotlin.script.experimental.util.PropertiesCollection
import kotlin.script.templates.standard.ScriptTemplateWithBindings

private val ScriptCompilationConfigurationKeys.exposedBindings by PropertiesCollection.key<Map<String, KotlinType>>()
private val ScriptCompilationConfigurationKeys.rootBindingsConfigured by PropertiesCollection.key(false)

private const val SYNTHETIC_SNIPPET_PREFIX = "\$\$synthetic_jsr223_"

// Engine-internal binding keys that must not be exposed as snippet properties.
private val ENGINE_INTERNAL_BINDING_KEYS = setOf(
    KOTLIN_SCRIPT_STATE_BINDINGS_KEY,
    KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY,
)

/**
 * Characters that can't appear in a Kotlin identifier under any quoting. These are the JVM
 * member-name characters rejected outright (`. ; [ ] / < > : \`), plus the backtick itself
 * (can't nest inside a backtick-quoted name) and raw line breaks. A name containing one of these
 * must go through [encodeBindingNameToMarkerIdentifier]. Every other name is legal as a
 * backtick-quoted identifier (see [encodeBindingNameToKotlinIdentifier]).
 */
private val NEEDS_MARKER_ENCODING_CHARS: Set<Char> =
    setOf('.', ';', '[', ']', '/', '<', '>', ':', '\\', '`', '\n', '\r')

private fun Char.isAsciiIdentifierChar(): Boolean =
    this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this == '_'

/**
 * Encodes a binding [name] that can't be a plain or backtick-quoted identifier into a plain Kotlin
 * identifier by replacing every problematic character with a `__u<hex>__` marker for its Unicode
 * code point (e.g. `a.b` -> `a__u002e__b`). Only injectivity is required: the generated accessor
 * reaches the value through the raw binding key, not by decoding this identifier. A raw name
 * spelled exactly like an emitted marker is therefore unsupported.
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
    // A leading digit is not a legal identifier start. Markers begin with `_`, so this only fires
    // when the name itself starts with a kept digit (e.g. `1.2` -> `1__u002e__2`).
    if (sb.isNotEmpty() && sb[0] in '0'..'9') sb.insert(0, '_')
    return sb.toString()
}

/**
 * Returns a Kotlin identifier that references a JSR-223 binding [name] from snippet source, or null
 * if [name] is empty. Plain identifiers are used verbatim. Any other name is backtick-quoted, or
 * reversibly encoded via [encodeBindingNameToMarkerIdentifier] if it contains a character from
 * [NEEDS_MARKER_ENCODING_CHARS].
 *
 * A backtick-quoted name must not be declared with a hardcoded `get()`/`set()` accessor. Every
 * generated snippet also declares `val bindings = getBindings(...)`, which is an
 * implicit-context-receiver call. Having both a backtick-quoted property with hand-written
 * accessors and that call in the same live REPL session makes the K2 REPL/script-snippet parser
 * fail with a spurious "Property getter or setter expected" error.
 * [generateBindingSnippetIfNeeded] sidesteps this by declaring every backtick-quoted property with
 * a delegate (`by ...`) instead, which parses through a different path.
 */
private fun encodeBindingNameToKotlinIdentifier(name: String): String? =
    when {
        name.isEmpty() -> null
        // Plain identifier: ASCII letters/digits/underscores, not digit-leading, not all-underscore.
        name.all { it.isAsciiIdentifierChar() } && name[0] !in '0'..'9' && name.any { it != '_' } -> name
        name.any { it in NEEDS_MARKER_ENCODING_CHARS } -> encodeBindingNameToMarkerIdentifier(name)
        // Everything else is safe as a backtick-quoted identifier, declared via a delegate (see above).
        else -> "`$name`"
    }

/**
 * Returns true if [name] is a valid JVM unqualified member name (JVM spec 4.2.2). Duplicated from
 * `org.jetbrains.kotlin.name.Name.isValidIdentifier` so this module doesn't depend on the compiler.
 */
private fun isValidJvmUnqualifiedName(name: String): Boolean =
    name.isNotEmpty() && !name.startsWith("<") && name.none { it == '.' || it == ';' || it == '[' || it == '/' }

/**
 * Returns true if [qualifiedName] is a dot-separated identifier chain the Kotlin parser accepts as a
 * type reference. Filters out synthetic/anonymous names (e.g. indy-lambda classes containing `/` or
 * `<`) that have a non-null `KClass.qualifiedName` on some JDKs but can't be embedded into source.
 */
private fun isParseableKotlinQualifiedName(qualifiedName: String): Boolean {
    if (qualifiedName.isEmpty()) return false
    return qualifiedName.split('.').all { isValidJvmUnqualifiedName(it) }
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

    // Implicit receivers exposed to every JSR-223 snippet: ScriptContext (the JSR-223 scopes and
    // attributes API) and ScriptTemplateWithBindings (the K1-era bindings-only shape). Their members
    // do not collide under normal use.
    //
    // Computed here rather than as a top-level property. A top-level list literal would be evaluated
    // by this file's static initializer on first call, forcing javax.script.* classes to load even
    // for non-JSR-223 compilations (MainKtsScriptDefinition wires this callback unconditionally).
    // That previously caused a spurious NoClassDefFoundError on plain .main.kts scripts.
    val requiredImplicitReceivers = listOf(ScriptContext::class, ScriptTemplateWithBindings::class)

    // Add each receiver only once. The engine threads a single, mutated ScriptCompilationConfiguration
    // across evals, including nested eval-in-eval. Appending unconditionally would grow the receiver
    // list per eval while the evaluator always passes exactly one of each, which surfaces as
    // `IllegalArgumentException: wrong number of arguments`
    // (KotlinJsr223ScriptEngineIT.testSimpleEvalInEval).
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
 * hardcoded `get()`/`set()`. See the doc comment on [encodeBindingNameToKotlinIdentifier] for why.
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

    // Declared in every synthetic snippet so each snippet's property accessors resolve `bindings`
    // from their own class (the ScriptContext active at that eval), not from synthetic-snippet-0.
    // That avoids stale-context bugs when eval is called with a custom Bindings argument.
    bindingsSnippet += "val bindings: javax.script.Bindings = getBindings(javax.script.ScriptContext.ENGINE_SCOPE)\n\n"

    if (context.compilationConfiguration[ScriptCompilationConfiguration.rootBindingsConfigured] != true) {
        // Declared only once, in the first synthetic snippet. The helpers reference snippet-0's
        // `bindings` (the default ENGINE_SCOPE), which is what eval-in-eval needs to save and restore.
        // Uses .put() and explicit null checks instead of [] = to avoid @InlineOnly stdlib operators.
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

// A property delegate used only for backtick-quoted binding properties. See the doc comment on
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
    // Recomputed below when `importAllBindings` is on, then written back into `exposedBindings` so
    // the next snippet can diff against it (add, retype, or remove).
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
        // Bindings whose name is a usable identifier and whose value type can be embedded as a Kotlin
        // type reference are exposed as typed properties. The rest, including indy lambdas and
        // local/anonymous classes, stay reachable only via `bindings["..."]`.
        val currentBindings = LinkedHashMap<String, KotlinType>()
        for ([k, v] in allBindings) {
            if (k in ENGINE_INTERNAL_BINDING_KEYS) continue
            if (encodeBindingNameToKotlinIdentifier(k) == null) continue
            val qn = v?.let { it::class.qualifiedName }
            if (v != null && (qn == null || !isParseableKotlinQualifiedName(qn))) continue
            // TODO: find out how this is implemented in other JSR-223 engines for typed languages, since
            //  this approach prevents certain usage scenarios, e.g. assigning back a value of a "sibling" type.
            currentBindings[k] = if (v == null) KotlinType(Any::class, isNullable = true) else KotlinType(v::class)
        }

        // Emit a fresh accessor for each binding that is new or retyped since it was last exposed
        // (compared by type name and nullability). The fresh accessor shadows the stale one in
        // subsequent snippets.
        for ([name, type] in currentBindings) {
            if (knownBindings[name] == type) continue
            val encodedName = encodeBindingNameToKotlinIdentifier(name)!!
            val safeKey = escapeForKotlinStringLiteral(name)
            // KotlinType.typeName strips the trailing `?`, so nullability must be appended explicitly.
            // Otherwise a null-valued binding would emit a non-null getter cast that NPEs on that value.
            val renderedType = if (type.isNullable) "${type.typeName}?" else type.typeName
            bindingsSnippet += renderBindingProperty(encodedName, renderedType, safeKey, removed = false)
        }

        // A binding that was exposed before but is no longer present gets a shadowing accessor that
        // keeps the old type, so existing user code still type-checks, but throws a clear diagnostic
        // instead of the stale getter's NPE. Re-adding the binding later emits a fresh accessor that
        // shadows this one.
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

// Concrete subclass of the abstract ScriptTemplateWithBindings. It wraps the same live, mutable
// Bindings map that already backs ScriptContext's ENGINE_SCOPE, so both receivers see the same data
// without separate synchronization.
private class Jsr223ScriptTemplateWithBindings(bindings: Map<String, Any?>) : ScriptTemplateWithBindings(bindings)

fun configureExposedJsr223Context(context: ScriptEvaluationConfigurationRefinementContext): ResultWithDiagnostics<ScriptEvaluationConfiguration> {
    val jsr223context = context.evaluationConfiguration[ScriptEvaluationConfiguration.jsr223.getScriptContext]?.invoke()
        ?: return context.evaluationConfiguration.asSuccess() // likely an error

    // Order matches the compile-time overload of configureExposedJsr223Context above:
    // ScriptContext first, then ScriptTemplateWithBindings.
    val engineBindings = jsr223context.getBindings(ScriptContext.ENGINE_SCOPE) ?: emptyMap<String, Any?>()
    return context.evaluationConfiguration.with {
        implicitReceivers(jsr223context, Jsr223ScriptTemplateWithBindings(engineBindings))
    }.asSuccess()
}

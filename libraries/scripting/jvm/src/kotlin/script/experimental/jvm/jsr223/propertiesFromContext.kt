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

private val ENGINE_INTERNAL_BINDING_KEYS = setOf(
    KOTLIN_SCRIPT_STATE_BINDINGS_KEY,
    KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY,
)

/**
 * Characters that make a name unusable as an identifier even backtick-quoted, so it has to go
 * through [encodeBindingNameToMarkerIdentifier]: the JVM member-name characters, the backtick
 * itself and raw line breaks.
 */
private val NEEDS_MARKER_ENCODING_CHARS: Set<Char> =
    setOf('.', ';', '[', ']', '/', '<', '>', ':', '\\', '`', '\n', '\r')

private fun Char.isAsciiIdentifierChar(): Boolean =
    this in 'A'..'Z' || this in 'a'..'z' || this in '0'..'9' || this == '_'

/**
 * Encodes a binding [name] into a plain Kotlin identifier, replacing every problematic character with
 * a `__u<hex>__` marker for its code point (e.g. `a.b` -> `a__u002e__b`). Only injectivity is
 * required, since the value is reached through the raw binding key rather than by decoding the
 * identifier back, so a raw name spelled exactly like an emitted marker is unsupported.
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
    if (sb.isNotEmpty() && sb[0] in '0'..'9') sb.insert(0, '_')
    return sb.toString()
}

/**
 * Returns a Kotlin identifier that references a JSR-223 binding [name] from snippet source, or null
 * if [name] is empty.
 *
 * A backtick-quoted result must not be declared with a hardcoded `get()`/`set()`: combined with the
 * implicit-context-receiver `getBindings(...)` call that every generated snippet contains, it makes
 * the K2 snippet parser fail with a spurious "Property getter or setter expected". Hence the
 * delegated declarations in [renderBindingProperty].
 */
private fun encodeBindingNameToKotlinIdentifier(name: String): String? =
    when {
        name.isEmpty() -> null
        name.all { it.isAsciiIdentifierChar() } && name[0] !in '0'..'9' && name.any { it != '_' } -> name
        name.any { it in NEEDS_MARKER_ENCODING_CHARS } -> encodeBindingNameToMarkerIdentifier(name)
        else -> "`$name`"
    }

/**
 * A valid JVM unqualified member name (JVM spec 4.2.2). Duplicated from
 * `org.jetbrains.kotlin.name.Name.isValidIdentifier` so that this module doesn't depend on the compiler.
 */
private fun isValidJvmUnqualifiedName(name: String): Boolean =
    name.isNotEmpty() && !name.startsWith("<") && name.none { it == '.' || it == ';' || it == '[' || it == '/' }

/**
 * A dot-separated identifier chain the Kotlin parser accepts as a type reference. Filters out
 * synthetic names that have a non-null `KClass.qualifiedName` on some JDKs but cannot be embedded.
 */
private fun isParseableKotlinQualifiedName(qualifiedName: String): Boolean {
    if (qualifiedName.isEmpty()) return false
    return qualifiedName.split('.').all { isValidJvmUnqualifiedName(it) }
}

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

    // Not a top-level property: its initializer would load javax.script.* on the first call even for
    // non-JSR-223 compilations, since MainKtsScriptDefinition wires this callback unconditionally.
    val requiredImplicitReceivers = listOf(ScriptContext::class, ScriptTemplateWithBindings::class)

    // The engine threads a single, mutated configuration across evals, so appending unconditionally
    // would grow the receiver list per eval, while the evaluator always passes exactly one of each
    // (KotlinJsr223ScriptEngineIT.testSimpleEvalInEval).
    val existingReceivers = context.compilationConfiguration[ScriptCompilationConfiguration.implicitReceivers].orEmpty()
    val missingReceivers = requiredImplicitReceivers.filter { KotlinType(it) !in existingReceivers }
    if (missingReceivers.isEmpty()) return context.compilationConfiguration.asSuccess()

    return ScriptCompilationConfiguration(context.compilationConfiguration) {
        implicitReceivers(*missingReceivers.toTypedArray())
    }.asSuccess()
}

/**
 * Renders the declaration of one exposed (or [removed]) binding. A backtick-quoted [encodedName] has
 * to be declared with a delegate, see [encodeBindingNameToKotlinIdentifier].
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

    // Declared in every synthetic snippet, so that the property accessors resolve `bindings` from
    // their own class, i.e. the ScriptContext active at that eval.
    bindingsSnippet += "val bindings: javax.script.Bindings = getBindings(javax.script.ScriptContext.ENGINE_SCOPE)\n\n"

    if (context.compilationConfiguration[ScriptCompilationConfiguration.rootBindingsConfigured] != true) {
        // Declared once, in the first synthetic snippet, so that these helpers reference the default
        // ENGINE_SCOPE bindings, which is what eval-in-eval needs to save and restore. `.put()` and
        // explicit null checks avoid the @InlineOnly stdlib operators.
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

// A property delegate used only for backtick-quoted binding properties, which cannot be declared
// with a hardcoded get()/set().
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
        // Bindings that cannot be exposed as typed properties stay reachable via `bindings["..."]`.
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

        // A new or retyped binding gets a fresh accessor, shadowing the stale one in later snippets.
        for ([name, type] in currentBindings) {
            if (knownBindings[name] == type) continue
            val encodedName = encodeBindingNameToKotlinIdentifier(name)!!
            val safeKey = escapeForKotlinStringLiteral(name)
            // KotlinType.typeName strips the trailing `?`, and a non-null cast would NPE on a null value.
            val renderedType = if (type.isNullable) "${type.typeName}?" else type.typeName
            bindingsSnippet += renderBindingProperty(encodedName, renderedType, safeKey, removed = false)
        }

        // A removed binding keeps a shadowing accessor of the previous type, so that existing user
        // code still type-checks, but throws a clear diagnostic instead of the stale getter's NPE.
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

// Wraps the same live, mutable Bindings map that backs ScriptContext's ENGINE_SCOPE, so that both
// receivers see the same data.
private class Jsr223ScriptTemplateWithBindings(bindings: Map<String, Any?>) : ScriptTemplateWithBindings(bindings)

fun configureExposedJsr223Context(context: ScriptEvaluationConfigurationRefinementContext): ResultWithDiagnostics<ScriptEvaluationConfiguration> {
    val jsr223context = context.evaluationConfiguration[ScriptEvaluationConfiguration.jsr223.getScriptContext]?.invoke()
        ?: return context.evaluationConfiguration.asSuccess() // likely an error

    // The order has to match the compile-time overload above.
    val engineBindings = jsr223context.getBindings(ScriptContext.ENGINE_SCOPE) ?: emptyMap<String, Any?>()
    return context.evaluationConfiguration.with {
        implicitReceivers(jsr223context, Jsr223ScriptTemplateWithBindings(engineBindings))
    }.asSuccess()
}

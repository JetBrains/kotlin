/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvm.jsr223

import java.lang.ref.WeakReference
import java.lang.reflect.Modifier
import javax.script.Bindings
import javax.script.ScriptContext
import kotlin.reflect.KClass
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.impl._isSyntheticSnippet
import kotlin.script.experimental.jvm.jsr223.base.KOTLIN_SCRIPT_ENGINE_BINDINGS_KEY
import kotlin.script.experimental.jvm.jsr223.base.KOTLIN_SCRIPT_STATE_BINDINGS_KEY
import kotlin.script.experimental.util.PropertiesCollection
import kotlin.script.experimental.templates.ScriptWithBindings

private val ScriptCompilationConfigurationKeys.exposedBindings by PropertiesCollection.key<Map<String, KotlinType>>()
private val ScriptCompilationConfigurationKeys.rootBindingsConfigured by PropertiesCollection.key(false)
private val ScriptCompilationConfigurationKeys.capturedScopeBindings by PropertiesCollection.key<CapturedScopeBindings>(isTransient = true)

/**
 * Set by the engine to true if the snippet is evaluated right after its compilation, with the same ScriptContext (`eval`),
 * false if it may be evaluated later with another one (`compile`). See [generateBindingSnippetIfNeeded] for reasons.
 */
internal val ScriptCompilationConfigurationKeys.jsr223EvaluatedWithCompilationContext by PropertiesCollection.key(false, isTransient = true)

/**
 * The ENGINE_SCOPE and GLOBAL_SCOPE bindings captured by the last synthetic snippet. Referenced weakly.
 */
private class CapturedScopeBindings(engineBindings: Bindings?, globalBindings: Bindings?) {
    private val engineBindingsRef = WeakReference(engineBindings)
    private val globalBindingsRef = WeakReference(globalBindings)

    fun isSameAs(engineBindings: Bindings?, globalBindings: Bindings?): Boolean =
        engineBindingsRef.get() === engineBindings && globalBindingsRef.get() === globalBindings
}

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

/**
 * Renders a binding value's class as a type reference with star-projected type arguments. Returns null
 * for an inner class of a generic class: such a reference needs type arguments on its outer part, which
 * a dot-separated qualified name cannot carry.
 */
private fun renderBindingTypeName(qualifiedName: String, kclass: KClass<*>): String? {
    val jClass = kclass.java
    var outer = jClass.enclosingGenericOwner()
    while (outer != null) {
        if (outer.typeParameters.isNotEmpty()) return null
        outer = outer.enclosingGenericOwner()
    }
    val arity = jClass.typeParameters.size
    return if (arity == 0) qualifiedName
    else (1..arity).joinToString(", ", "$qualifiedName<", ">") { "*" }
}

private fun Class<*>.enclosingGenericOwner(): Class<*>? =
    if (isMemberClass && !Modifier.isStatic(modifiers)) enclosingClass else null

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
    // attributes API) and ScriptWithBindings (the bindings-only shape). Their members
    // do not collide under normal use.
    //
    // Computed here rather than as a top-level property. A top-level list literal would be evaluated
    // by this file's static initializer on first call, forcing javax.script.* classes to load even
    // for non-JSR-223 compilations (MainKtsScriptDefinition wires this callback unconditionally).
    // That previously caused a spurious NoClassDefFoundError on plain .main.kts scripts.
    val requiredImplicitReceivers = listOf(ScriptContext::class, ScriptWithBindings::class)

    // Add each receiver only once. The engine threads the resulting ScriptCompilationConfiguration
    // from one eval into the next one compiled by the same REPL compiler. Appending unconditionally would grow the receiver
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

// The GLOBAL_SCOPE bindings of the ScriptContext active at the snippet's eval. Captured in a snippet property along with
// `bindings`, since the implicit ScriptContext receiver is not accessible from the snippet property accessors.
private const val GLOBAL_BINDINGS_PROPERTY = "__jsr223GlobalBindings"

/**
 * Renders the `var $encodedName: $renderedType ...` declaration for one exposed (or [removed]) binding.
 * A backtick-quoted [encodedName] is declared with a [__Jsr223BindingDelegate] (`by ...`) instead of a
 * hardcoded `get()`/`set()`. See the doc comment on [encodeBindingNameToKotlinIdentifier] for why.
 */
private fun renderBindingProperty(encodedName: String, renderedType: String, safeKey: String, removed: Boolean): String =
    if (encodedName.startsWith("`")) {
        """
            var $encodedName: $renderedType by __Jsr223BindingDelegate<$renderedType>(bindings, $GLOBAL_BINDINGS_PROPERTY, "$safeKey"${if (removed) ", removed = true" else ""})

        """.trimIndent() + "\n"
    } else if (!removed) {
        """
            @Suppress("UNCHECKED_CAST")
            var $encodedName: $renderedType
                get() = __jsr223GetBinding(bindings, $GLOBAL_BINDINGS_PROPERTY, "$safeKey") as $renderedType
                set(value) { __jsr223SetBinding(bindings, $GLOBAL_BINDINGS_PROPERTY, "$safeKey", value) }

        """.trimIndent() + "\n"
    } else {
        """
            @Suppress("UNCHECKED_CAST")
            var $encodedName: $renderedType
                get() = throw java.util.NoSuchElementException("JSR-223 binding \"$safeKey\" is no longer available")
                set(value) { __jsr223SetBinding(bindings, $GLOBAL_BINDINGS_PROPERTY, "$safeKey", value) }

        """.trimIndent() + "\n"
    }

fun generateBindingSnippetIfNeeded(context: ScriptConfigurationRefinementContext):
        ResultWithDiagnostics<Pair<ScriptCompilationConfiguration, SourceCode?>>
{
    val jsr223context =
        context.compilationConfiguration[ScriptCompilationConfiguration.jsr223.getScriptContext]?.invoke()
            ?: return (context.compilationConfiguration to null).asSuccess()

    val engineScopeBindings = jsr223context.getBindings(ScriptContext.ENGINE_SCOPE)
    val globalScopeBindings = jsr223context.getBindings(ScriptContext.GLOBAL_SCOPE)
    val rootBindingsConfigured = context.compilationConfiguration[ScriptCompilationConfiguration.rootBindingsConfigured] == true
    val isEvaluatedWithCompilationContext =
        context.compilationConfiguration[ScriptCompilationConfiguration.jsr223EvaluatedWithCompilationContext] == true
    // A synthetic snippet is needed if it declares anything new: the root helpers, the binding accessors (see below), or
    // `bindings` capturing other scope bindings than the previous one. The latter is also the case if the snippet may be
    // evaluated with another context than the current one.
    var isSnippetNeeded = !rootBindingsConfigured || !isEvaluatedWithCompilationContext ||
            context.compilationConfiguration[ScriptCompilationConfiguration.capturedScopeBindings]
                ?.isSameAs(engineScopeBindings, globalScopeBindings) != true

    var bindingsSnippet = ""

    // Declared in every synthetic snippet so each snippet's property accessors resolve `bindings` and
    // the GLOBAL_SCOPE bindings from their own class (the ScriptContext active at that eval), not from
    // synthetic-snippet-0.
    // That avoids stale-context bugs when eval is called with a custom Bindings argument.
    bindingsSnippet += "val bindings: javax.script.Bindings = getBindings(javax.script.ScriptContext.ENGINE_SCOPE)\n"
    bindingsSnippet += "val $GLOBAL_BINDINGS_PROPERTY: javax.script.Bindings? = getBindings(javax.script.ScriptContext.GLOBAL_SCOPE)\n\n"

    if (!rootBindingsConfigured) {
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

fun __jsr223GetBinding(bindings: javax.script.Bindings, globalBindings: javax.script.Bindings?, key: String): Any? =
    if (globalBindings == null || bindings.containsKey(key)) bindings[key] else globalBindings[key]

fun __jsr223SetBinding(bindings: javax.script.Bindings, globalBindings: javax.script.Bindings?, key: String, value: Any?) {
    if (globalBindings != null && !bindings.containsKey(key) && globalBindings.containsKey(key)) globalBindings.put(key, value)
    else bindings.put(key, value)
}

class __Jsr223BindingDelegate<T>(
    private val bindings: javax.script.Bindings,
    private val globalBindings: javax.script.Bindings?,
    private val key: String,
    private val removed: Boolean = false,
) {
    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T {
        if (removed) throw java.util.NoSuchElementException("JSR-223 binding \"${'$'}key\" is no longer available")
        return __jsr223GetBinding(bindings, globalBindings, key) as T
    }
    operator fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: T) {
        __jsr223SetBinding(bindings, globalBindings, key, value)
    }
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
        val allBindings = (globalScopeBindings?.toMutableMap() ?: hashMapOf()).apply {
            if (engineScopeBindings != null)
                putAll(engineScopeBindings)
        }
        // Bindings whose name is a usable identifier and whose value type can be embedded as a Kotlin
        // type reference are exposed as typed properties. The rest, including indy lambdas and
        // local/anonymous classes, stay reachable only via `bindings["..."]`.
        val currentBindings = LinkedHashMap<String, KotlinType>()
        for ([k, v] in allBindings) {
            if (k in ENGINE_INTERNAL_BINDING_KEYS) continue
            if (encodeBindingNameToKotlinIdentifier(k) == null) continue
            if (v == null) {
                currentBindings[k] = KotlinType(Any::class, isNullable = true)
                continue
            }
            val qn = v::class.qualifiedName
            if (qn == null || !isParseableKotlinQualifiedName(qn)) continue
            val renderedTypeName = renderBindingTypeName(qn, v::class) ?: continue
            // TODO: find out how this is implemented in other JSR-223 engines for typed languages, since
            //  this approach prevents certain usage scenarios, e.g. assigning back a value of a "sibling" type.
            currentBindings[k] = KotlinType(renderedTypeName)
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
            isSnippetNeeded = true
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
            isSnippetNeeded = true
        }

        updatedBindings = currentBindings
    }
    // Nothing new to declare: the snippet uses the same declarations of the previous synthetic snippet.
    if (!isSnippetNeeded) return (context.compilationConfiguration to null).asSuccess()

    val source = bindingsSnippet.toScriptSource(SYNTHETIC_SNIPPET_PREFIX + context.script.name)
    return (
            context.compilationConfiguration.with {
                rootBindingsConfigured(true)
                exposedBindings(updatedBindings)
                // The bindings captured at the evaluation are only known in advance if it follows the compilation
                if (isEvaluatedWithCompilationContext) capturedScopeBindings(CapturedScopeBindings(engineScopeBindings, globalScopeBindings))
                else reset(capturedScopeBindings)
            } to source).asSuccess()
}

// Concrete subclass of the abstract ScriptWithBindings. It wraps the same live, mutable
// Bindings map that already backs ScriptContext's ENGINE_SCOPE, so both receivers see the same data
// without separate synchronization.
private class Jsr223ScriptWithBindings(bindings: Map<String, Any?>) : ScriptWithBindings(bindings)

fun configureExposedJsr223Context(context: ScriptEvaluationConfigurationRefinementContext): ResultWithDiagnostics<ScriptEvaluationConfiguration> {
    val jsr223context = context.evaluationConfiguration[ScriptEvaluationConfiguration.jsr223.getScriptContext]?.invoke()
        ?: return context.evaluationConfiguration.asSuccess() // likely an error

    // Order matches the compile-time overload of configureExposedJsr223Context above:
    // ScriptContext first, then ScriptWithBindings.
    val engineBindings = jsr223context.getBindings(ScriptContext.ENGINE_SCOPE) ?: emptyMap<String, Any?>()
    return context.evaluationConfiguration.with {
        implicitReceivers(jsr223context, Jsr223ScriptWithBindings(engineBindings))
    }.asSuccess()
}

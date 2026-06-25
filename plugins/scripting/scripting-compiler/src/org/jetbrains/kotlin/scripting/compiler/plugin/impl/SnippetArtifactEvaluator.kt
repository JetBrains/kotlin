/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * Execution-side counterpart of [K2ReplStatelessCompiler] for the **stateless K2 REPL** prototype.
 *
 * [K2ReplStatelessCompiler] turns each REPL snippet into a portable [SnippetArtifact] (class bytes
 * + JSON sidecar). This file closes the loop: given the ordered list of artifacts for a whole REPL
 * session, [SnippetArtifactEvaluator] materialises every snippet's `.class` bytes onto a single
 * in-memory [ClassLoader] and drives each snippet's `$$eval` driver in history order, exactly as the
 * stateful evaluators do (`K2ReplEvaluator` / `AbstractReplTestBaseClasses`):
 *
 *  * each snippet wrapper is a JVM `object` exposing a public static `INSTANCE` field,
 *  * the snippet body is the `$$eval` ([REPL_SNIPPET_EVAL_FUN_NAME]) method on that object,
 *  * an expression snippet's value is stored in the `$$result`
 *    ([SnippetArtifactSidecar.resultPropertyName] / [REPL_SNIPPET_EVAL_FUN_NAME]) field.
 *
 * Because every snippet of the session is replayed from a flat classloader that already holds *all*
 * the snippets' classes, cross-snippet references (`Snippet_2.$$eval` reading `Snippet_1.INSTANCE.x`)
 * resolve directly — there is no need for the chained per-snippet classloaders the incremental
 * stateful path uses. The one hard requirement is that `$$eval` is invoked in history order, since a
 * prior snippet's `val`s are initialised inside its `$$eval`, not its constructor.
 *
 * This is the in-process execution proof that the artifacts produced by the stateless compiler are
 * actually runnable — not merely diagnostically equivalent. It is **internal** and prototype-only,
 * mirroring [K2ReplStatelessCompiler]; the eventual stable surface lives in
 * `libraries/scripting/common`.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import org.jetbrains.kotlin.scripting.compiler.plugin.irLowerings.REPL_SNIPPET_EVAL_FUN_NAME
import java.lang.reflect.InvocationTargetException

/**
 * Replays a stateless REPL session by reflectively executing each snippet artifact in order.
 *
 * @param parentClassLoader the classloader supplying everything the snippet classes link against
 *   (Kotlin stdlib, script runtime, any added dependencies). Defaults to this class's own loader,
 *   which is correct for in-process callers that already have those on their classpath.
 */
internal class SnippetArtifactEvaluator(
    private val parentClassLoader: ClassLoader = SnippetArtifactEvaluator::class.java.classLoader,
) {

    /**
     * Materialises [artifacts] onto a fresh in-memory classloader and invokes each snippet's
     * `$$eval` in history order.
     *
     * @param artifacts ordered list of the session's snippet artifacts (snippet 1..N). Must be
     *   non-empty.
     * @return a [SnippetArtifactEvalResult] holding the per-snippet wrapper classes/instances and
     *   the last snippet's captured result value.
     * @throws IllegalArgumentException if [artifacts] is empty.
     * @throws SnippetArtifactEvaluationException if a snippet body throws during `$$eval`.
     */
    fun evaluate(artifacts: List<SnippetArtifact>): SnippetArtifactEvalResult {
        require(artifacts.isNotEmpty()) { "SnippetArtifactEvaluator: nothing to evaluate (empty artifact list)" }

        val sidecars = artifacts.map { it.decodeSidecar() }

        // Collect every class of every snippet into one binary-name-keyed map. Later snippets'
        // classes never collide with earlier ones (wrapper class names embed the snippet id), so a
        // single flat namespace is sufficient and makes cross-snippet linkage trivial.
        val classBytesByBinaryName = HashMap<String, ByteArray>()
        for (artifact in artifacts) {
            for ([internalName, bytes] in artifact.classFiles) {
                classBytesByBinaryName[internalName.replace('/', '.')] = bytes
            }
        }
        val loader = InMemoryArtifactClassLoader(classBytesByBinaryName, parentClassLoader)

        val evalFunName = REPL_SNIPPET_EVAL_FUN_NAME.asString()
        val snippetClasses = ArrayList<Class<*>>(artifacts.size)
        val snippetInstances = ArrayList<Any>(artifacts.size)

        for ([index, sidecar] in sidecars.withIndex()) {
            val binaryName = sidecar.snippetClassInternalName.replace('/', '.')
            val snippetClass = loader.loadClass(binaryName)
            val evalMethod = snippetClass.methods.firstOrNull { it.name == evalFunName }
                ?: error(
                    "SnippetArtifactEvaluator: snippet[$index] '$binaryName' has no '$evalFunName' method — " +
                            "the artifact does not look like a compiled REPL snippet"
                )
            val instance = snippetClass.getField("INSTANCE").get(null)
            evalMethod.isAccessible = true
            try {
                evalMethod.invoke(instance)
            } catch (e: InvocationTargetException) {
                throw SnippetArtifactEvaluationException(
                    "stateless REPL: snippet[$index] '${sidecar.snippetName}' threw during \$\$eval",
                    e.targetException ?: e,
                )
            }
            snippetClasses += snippetClass
            snippetInstances += instance
        }

        // Read the last snippet's result field, if it declares one. The field name is the sidecar's
        // `resultPropertyName`; it may legitimately be absent (declaration-only snippets emit no
        // result field), in which case the result value is `null` — mirroring `K2ReplEvaluator`,
        // which falls back to `ResultValue.Unit` when the field is missing.
        val lastClass = snippetClasses.last()
        val lastInstance = snippetInstances.last()
        val resultFieldName = sidecars.last().resultPropertyName?.takeIf { it.isNotBlank() }
        val lastResultValue = resultFieldName?.let { name ->
            val field = runCatching { lastClass.getDeclaredField(name) }.getOrNull()
            field?.isAccessible = true
            field?.get(lastInstance)
        }

        return SnippetArtifactEvalResult(
            classLoader = loader,
            snippetClasses = snippetClasses,
            snippetInstances = snippetInstances,
            resultFieldName = resultFieldName,
            lastResultValue = lastResultValue,
        )
    }
}

/**
 * Outcome of replaying a stateless REPL session via [SnippetArtifactEvaluator.evaluate].
 *
 * @property classLoader the in-memory classloader holding all replayed snippet classes. Kept alive
 *   so callers can reflectively read additional fields after the run.
 * @property snippetClasses the loaded wrapper classes, in history order.
 * @property snippetInstances the `INSTANCE` singleton of each wrapper class, in history order.
 * @property resultFieldName the name of the last snippet's result field, or `null` if it had none.
 * @property lastResultValue the value of the last snippet's result field, or `null` if it had none.
 */
internal class SnippetArtifactEvalResult(
    val classLoader: ClassLoader,
    val snippetClasses: List<Class<*>>,
    val snippetInstances: List<Any>,
    val resultFieldName: String?,
    val lastResultValue: Any?,
) {
    val lastSnippetClass: Class<*> get() = snippetClasses.last()
    val lastSnippetInstance: Any get() = snippetInstances.last()

    /**
     * Reflectively reads the value of declared field [fieldName] from the snippet at [snippetIndex]
     * (0-based, history order). Useful for asserting that a `val` introduced by an earlier snippet
     * actually holds the expected runtime value.
     */
    fun readDeclaredField(snippetIndex: Int, fieldName: String): Any? {
        val field = snippetClasses[snippetIndex].getDeclaredField(fieldName).also { it.isAccessible = true }
        return field.get(snippetInstances[snippetIndex])
    }
}

/** Thrown when a snippet body raises an exception during `$$eval` replay. */
internal class SnippetArtifactEvaluationException(
    message: String,
    cause: Throwable,
) : RuntimeException(message, cause)

/**
 * A [ClassLoader] that defines the replayed snippet classes from in-memory bytes and delegates
 * everything else (stdlib, script runtime, dependencies) to its parent.
 *
 * Standard parent-first delegation is preserved: only classes present in [classBytesByBinaryName]
 * are defined here, so a snippet class that happens to share a name with a parent class would still
 * be shadowed — acceptable for the prototype, where wrapper class names are unique per session.
 */
private class InMemoryArtifactClassLoader(
    private val classBytesByBinaryName: Map<String, ByteArray>,
    parent: ClassLoader,
) : ClassLoader(parent) {
    override fun findClass(name: String): Class<*> {
        val bytes = classBytesByBinaryName[name] ?: throw ClassNotFoundException(name)
        return defineClass(name, bytes, 0, bytes.size)
    }
}

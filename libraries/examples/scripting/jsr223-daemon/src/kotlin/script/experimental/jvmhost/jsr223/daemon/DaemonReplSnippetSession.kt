/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.daemon

import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactCodec
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.decodeHeader
import java.lang.reflect.InvocationTargetException

/**
 * Executes, in-process, the class-file artifacts produced out-of-process by
 * [DaemonReplSnippetCompiler] -- one call to [evaluateNext] per new snippet, in history order.
 *
 * Each new artifact's classes are added to a single classloader shared for the whole session, so
 * cross-snippet references (`snippet_2`'s `$$eval` reading `snippet_1`'s `x`) resolve directly.
 * This class is **incremental**: it only defines/evaluates the newly-added snippet on each call
 * (the earlier snippets' classes are already defined and already ran), which is the shape a live
 * JSR-223 engine needs.
 *
 * A small, from-scratch reimplementation of the artifact-replay logic, not a reuse of
 * `SnippetArtifactEvaluator`/`ArtifactBackedFirReplHistoryProvider`'s decoding helpers (those are
 * `internal` to `:kotlin-scripting-compiler` and therefore not visible from this module). The wire
 * codec types used here ([SnippetArtifactCodec], `SnippetArtifactHeader`) *are* public and are
 * reused as-is. (Mirrors `libraries/scripting/jsr223-bta`'s `BtaReplSnippetSession`, which faces the
 * exact same constraint.)
 *
 * ### Bindings / implicit receivers are out of scope
 *
 * `$$eval` is invoked with **no arguments**. The stateless compiler / [DaemonReplSnippetCompiler]
 * have no receiver-passing support at all yet (unlike the in-process
 * [kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl], which adds
 * `ScriptContext`/`ScriptTemplateWithBindings` implicit receivers via `propertiesFromContext.kt`),
 * so a snippet with a non-empty `$$eval` receiver-parameter list would fail here with a reflective
 * `IllegalArgumentException`. Deliberately deferred for this first cut, same as `jsr223-bta`.
 */
internal class DaemonReplSnippetSession(
    parentClassLoader: ClassLoader = DaemonReplSnippetSession::class.java.classLoader,
) {
    private val classBytesByBinaryName = LinkedHashMap<String, ByteArray>()

    private val loader = object : ClassLoader(parentClassLoader) {
        override fun findClass(name: String): Class<*> {
            val bytes = classBytesByBinaryName[name] ?: throw ClassNotFoundException(name)
            return defineClass(name, bytes, 0, bytes.size)
        }
    }

    /**
     * Decodes [artifactBytes] (the wire bytes produced by [DaemonReplSnippetCompiler]), defines its
     * classes onto this session's shared classloader, invokes its `$$eval`, and returns the
     * snippet's captured result value (`null` for a declaration-only snippet, or one whose result
     * is `Unit`).
     *
     * @throws DaemonReplSnippetEvaluationException if the snippet body throws during `$$eval`.
     */
    fun evaluateNext(artifactBytes: ByteArray): Any? {
        val artifact = SnippetArtifactCodec.decode(artifactBytes)
        val header = artifact.decodeHeader()
        for (entry in artifact.classFiles) {
            classBytesByBinaryName[entry.key.replace('/', '.')] = entry.value
        }

        val binaryName = header.snippetClassInternalName.replace('/', '.')
        val snippetClass = loader.loadClass(binaryName)
        val evalMethod = snippetClass.methods.firstOrNull { it.name == EVAL_FUN_NAME }
            ?: error(
                "KotlinJsr223DaemonScriptEngine: snippet '$binaryName' has no '$EVAL_FUN_NAME' method -- " +
                        "the artifact does not look like a compiled REPL snippet"
            )
        val instance = snippetClass.getField("INSTANCE").get(null)
        evalMethod.isAccessible = true
        try {
            evalMethod.invoke(instance)
        } catch (e: InvocationTargetException) {
            throw DaemonReplSnippetEvaluationException(
                "KotlinJsr223DaemonScriptEngine: snippet '${header.snippetName}' threw during $EVAL_FUN_NAME",
                e.targetException ?: e,
            )
        }

        val resultFieldName = header.resultPropertyName?.takeIf { it.isNotBlank() } ?: return null
        val field = runCatching { snippetClass.getDeclaredField(resultFieldName) }.getOrNull() ?: return null
        field.isAccessible = true
        return field.get(instance)
    }

    companion object {
        // See "Critical Patterns" in plugins/scripting/.ai/AGENT_INSTRUCTIONS.md: the "$$eval" /
        // "$$result" constants are stable and must not be renamed/shadowed. Hardcoded here (rather
        // than importing the compiler's `Name`-typed REPL_SNIPPET_EVAL_FUN_NAME constant) to avoid
        // pulling `kotlin-compiler` into this module's dependency footprint for a single stable
        // string literal.
        private const val EVAL_FUN_NAME = "\$\$eval"
    }
}

/** Thrown by [DaemonReplSnippetSession.evaluateNext] when a snippet body raises during `$$eval`. */
internal class DaemonReplSnippetEvaluationException(message: String, cause: Throwable) : RuntimeException(message, cause)

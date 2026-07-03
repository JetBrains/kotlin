/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.jvmhost.jsr223.bta

import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactCodec
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.decodeHeader
import java.lang.reflect.InvocationTargetException

/**
 * Executes, in-process, the class-file artifacts produced out-of-process by
 * [org.jetbrains.kotlin.buildtools.api.jvm.operations.CompileReplSnippetOperation] -- one call to
 * [evaluateNext] per new snippet, in history order.
 *
 * Each new artifact's classes are added to a single classloader shared for the whole session, so
 * cross-snippet references (`snippet_2`'s `$$eval` reading `snippet_1`'s `x`) resolve directly --
 * mirroring how `SnippetArtifactEvaluator` (`:kotlin-scripting-compiler`, internal/test-only)
 * replays a whole stateless-REPL session. Unlike that evaluator, this class is **incremental**: it
 * only defines/evaluates the newly-added snippet on each call (the earlier snippets' classes are
 * already defined and already ran), which is the shape a live JSR-223 engine needs.
 *
 * This is a small, from-scratch reimplementation rather than a reuse of
 * `SnippetArtifactEvaluator`/`ArtifactBackedFirReplHistoryProvider`'s decoding helpers, because
 * those are `internal` to `:kotlin-scripting-compiler` and therefore not visible from this module.
 * The wire codec types used here ([SnippetArtifactCodec], `SnippetArtifactHeader`) *are* public and
 * are reused as-is.
 *
 * ### Bindings / implicit receivers are out of scope
 *
 * `$$eval` is invoked with **no arguments**. The stateless compiler /
 * [org.jetbrains.kotlin.buildtools.api.jvm.operations.CompileReplSnippetOperation] have no
 * receiver-passing support at all yet (unlike the in-process
 * [kotlin.script.experimental.jvmhost.jsr223.KotlinJsr223ScriptEngineImpl], which adds
 * `ScriptContext`/`ScriptTemplateWithBindings` implicit receivers via
 * `propertiesFromContext.kt`), so a snippet with a non-empty `$$eval` receiver-parameter list would
 * fail here with a reflective `IllegalArgumentException`. Deliberately deferred for this first cut
 * -- see the class doc of [KotlinJsr223BtaScriptEngineImpl].
 */
internal class BtaReplSnippetSession(
    parentClassLoader: ClassLoader = BtaReplSnippetSession::class.java.classLoader,
) {
    private val classBytesByBinaryName = LinkedHashMap<String, ByteArray>()

    private val loader = object : ClassLoader(parentClassLoader) {
        override fun findClass(name: String): Class<*> {
            val bytes = classBytesByBinaryName[name] ?: throw ClassNotFoundException(name)
            return defineClass(name, bytes, 0, bytes.size)
        }
    }

    /**
     * Decodes [artifactBytes] (the wire bytes of a
     * [org.jetbrains.kotlin.buildtools.api.jvm.operations.ReplSnippetCompilationResult.Success]),
     * defines its classes onto this session's shared classloader, invokes its `$$eval`, and returns
     * the snippet's captured result value (`null` for a declaration-only snippet, or one whose
     * result is `Unit`).
     *
     * @throws BtaReplSnippetEvaluationException if the snippet body throws during `$$eval`.
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
                "KotlinJsr223BtaScriptEngine: snippet '$binaryName' has no '$EVAL_FUN_NAME' method -- " +
                        "the artifact does not look like a compiled REPL snippet"
            )
        val instance = snippetClass.getField("INSTANCE").get(null)
        evalMethod.isAccessible = true
        try {
            evalMethod.invoke(instance)
        } catch (e: InvocationTargetException) {
            throw BtaReplSnippetEvaluationException(
                "KotlinJsr223BtaScriptEngine: snippet '${header.snippetName}' threw during $EVAL_FUN_NAME",
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

/** Thrown by [BtaReplSnippetSession.evaluateNext] when a snippet body raises during `$$eval`. */
internal class BtaReplSnippetEvaluationException(message: String, cause: Throwable) : RuntimeException(message, cause)

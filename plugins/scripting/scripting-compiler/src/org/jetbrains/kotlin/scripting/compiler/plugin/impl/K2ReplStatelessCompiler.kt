/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.impl

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.cli.common.environment.setIdeaIoUseFallback
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.scripting.compiler.plugin.services.ArtifactBackedFirReplHistoryProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.services.firReplHistoryProvider
import org.jetbrains.kotlin.scripting.compiler.plugin.services.isReplSnippetSource
import org.jetbrains.kotlin.scripting.compiler.plugin.services.replStateObjectFqName
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeBytes
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptCompilationConfiguration
import kotlin.script.experimental.api.SourceCode
import kotlin.script.experimental.api.asDiagnostics
import kotlin.script.experimental.api.asSuccess
import kotlin.script.experimental.api.dependencies
import kotlin.script.experimental.api.makeFailureResult
import kotlin.script.experimental.api.onSuccess
import kotlin.script.experimental.api.repl
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.jvm.JvmDependency
import kotlin.script.experimental.jvm.defaultJvmScriptingHostConfiguration

/**
 * **Stateless** K2 REPL compiler prototype.
 *
 * Unlike the stateful [K2ReplCompiler], `K2ReplStatelessCompiler` keeps **no** cross-call FIR
 * state. Each [compile] invocation gets an ordered list of [SnippetArtifact]s for the previous
 * snippets (1..N-1) plus a fresh [SourceCode] for snippet N, and produces a new [SnippetArtifact]
 * for snippet N — fully self-describing, suitable for being persisted, shipped over the wire, or
 * fed into a later stateless call as a prior snippet.
 *
 * Implementation:
 *  * Each call builds a brand-new [K2ReplCompilationState] via `K2ReplCompiler.createCompilationState`,
 *    with the host configuration overridden so that `repl.firReplHistoryProvider` is an
 *    [ArtifactBackedFirReplHistoryProvider] bound to [priorSnippets][SnippetArtifact].
 *  * Each prior snippet's `classFiles` are dumped into a per-call temp directory and added to the
 *    compilation configuration's `dependencies` as a [JvmDependency]. This is the same mechanism
 *    `K2ReplCompiler.compileImpl` uses to honor added dependencies mid-session — it picks up the
 *    new dependencies through `ReplModuleDataProvider.addNewLibraryModuleDataIfNeeded`.
 *  * A capture hook on [K2ReplCompilationState.snippetCompilationObserver] receives the
 *    just-compiled `FirReplSnippet`, producing `FirSession`, and `GenerationState`, and the
 *    resulting [SnippetArtifact] is built from them via [buildSnippetArtifactFromCompile].
 *
 * Callers must retain **synthetic** prior snippets (e.g. JSR-223 binding cells) in
 * [SnippetArtifact] order as well — the sidecar carries the `isSynthetic` flag but the resolve
 * extension does not branch on it, so dropping synthetic snippets would break references from
 * subsequent user snippets.
 *
 * This class is **internal** and prototype-only. The eventual stable surface lives in
 * `libraries/scripting/common` as `StatelessReplCompiler` (planned for a later step).
 */
class K2ReplStatelessCompiler {

    /**
     * Compiles [snippet] against the given ordered [priorSnippets].
     *
     * @param priorSnippets ordered list of prior-snippet artifacts (1..N-1). The list is treated
     *   as immutable.
     * @param snippet new snippet source (snippet N).
     * @param scriptCompilationConfiguration per-call compilation configuration. The configuration's
     *   `dependencies` are augmented with the per-call temp-dir holding the prior classfiles.
     * @param hostConfiguration host configuration. The provided value's
     *   `repl.firReplHistoryProvider` and `repl.isReplSnippetSource` are overridden by this method.
     *   `repl.replStateObjectFqName` is validated against every prior snippet's sidecar; a
     *   mismatch causes a [ResultWithDiagnostics.Failure] return.
     * @param parentDisposable optional caller-owned [Disposable] under which the compilation
     *   environment will be created. When `null` (default), this call allocates a private
     *   top-level [Disposable] and disposes it before returning — this is the right shape for
     *   out-of-process callers where each call owns its full lifecycle. When supplied (typical in
     *   in-process / test scenarios where another lifecycle already owns project state), the
     *   stateless compiler attaches its per-call environment to [parentDisposable] and does *not*
     *   trigger `Disposer.dispose` at the end, leaving cleanup to the caller. This avoids
     *   IntelliJ-platform "Write access is allowed inside write-action only" failures that would
     *   otherwise fire on per-call disposal inside a hosted test fixture's project lifecycle.
     * @return either a fresh [SnippetArtifact] for snippet N or a failure with diagnostics.
     */
    suspend fun compile(
        priorSnippets: List<SnippetArtifact>,
        snippet: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
        hostConfiguration: ScriptingHostConfiguration = defaultJvmScriptingHostConfiguration,
        parentDisposable: Disposable? = null,
    ): ResultWithDiagnostics<SnippetArtifact> {
        if (parentDisposable != null) {
            // Caller-owned lifecycle: do NOT call Disposer.dispose ourselves on any path
            // (success *or* failure), because the parent's owner manages teardown — and on
            // failure the per-snippet result may legitimately be a `ResultWithDiagnostics.Failure`
            // (e.g. unresolved-reference diagnostics) which would otherwise trigger a per-call
            // disposal that violates IntelliJ-platform threading inside hosted test fixtures.
            val messageCollector = ScriptDiagnosticsMessageCollector(parentMessageCollector = null)
            return try {
                setIdeaIoUseFallback()
                compileWithCollector(
                    priorSnippets, snippet, scriptCompilationConfiguration, hostConfiguration,
                    messageCollector, parentDisposable,
                )
            } catch (ex: Throwable) {
                failure(messageCollector, ex.asDiagnostics(path = snippet.locationId))
            }
        }
        return withMessageCollectorAndDisposable(snippet) { messageCollector, disposable ->
            compileWithCollector(
                priorSnippets, snippet, scriptCompilationConfiguration, hostConfiguration,
                messageCollector, disposable,
            )
        }
    }

    private suspend fun compileWithCollector(
        priorSnippets: List<SnippetArtifact>,
        snippet: SourceCode,
        scriptCompilationConfiguration: ScriptCompilationConfiguration,
        hostConfiguration: ScriptingHostConfiguration,
        messageCollector: ScriptDiagnosticsMessageCollector,
        disposable: Disposable,
    ): ResultWithDiagnostics<SnippetArtifact> {
        // 1. Decode sidecars + validate state-object FQ name agreement.
        val sidecars = try {
            priorSnippets.map { it.decodeSidecar() }
        } catch (t: Throwable) {
            return makeFailureResult("stateless REPL: failed to decode prior snippet sidecar: ${t.message}")
        }
        val callerStateObjectFqName = hostConfiguration[ScriptingHostConfiguration.repl.replStateObjectFqName]
        for ((index, sidecar) in sidecars.withIndex()) {
            if (sidecar.stateObjectFqName.isEmpty()) continue
            if (callerStateObjectFqName != null && sidecar.stateObjectFqName != callerStateObjectFqName) {
                return makeFailureResult(
                    "stateless REPL: stateObjectFqName mismatch — prior snippet[$index] " +
                            "carries '${sidecar.stateObjectFqName}', caller hostConfiguration " +
                            "supplies '$callerStateObjectFqName'"
                )
            }
        }
        for (i in 1 until sidecars.size) {
            val prev = sidecars[i - 1].stateObjectFqName
            val cur = sidecars[i].stateObjectFqName
            if (prev.isNotEmpty() && cur.isNotEmpty() && prev != cur) {
                return makeFailureResult(
                    "stateless REPL: stateObjectFqName disagreement across priorSnippets — " +
                            "snippet[${i - 1}]='$prev' vs snippet[$i]='$cur'"
                )
            }
        }
        debug("compile(): ${priorSnippets.size} prior snippet(s); state-object FQ=${callerStateObjectFqName ?: "<from-defaults>"}")

        // 2. Write prior class files into a per-call temp directory.
        val tempDir: Path = Files.createTempDirectory("k2-repl-stateless-")
        val sessionRef = AtomicReference<FirSession?>()
        try {
            for ((i, artifact) in priorSnippets.withIndex()) {
                val sidecar = sidecars[i]
                writeClassFiles(tempDir, artifact, sidecar)
            }

            // 3. Build the stateless-flavoured host configuration:
            //    * `firReplHistoryProvider` -> ArtifactBackedFirReplHistoryProvider
            //    * `isReplSnippetSource`   -> always true (matches the K2ReplCompiler default behaviour)
            val artifactProvider = ArtifactBackedFirReplHistoryProvider(priorSnippets) { sessionRef.get() }
            val statelessHostConfiguration = ScriptingHostConfiguration(hostConfiguration) {
                repl {
                    firReplHistoryProvider(artifactProvider)
                    isReplSnippetSource { _, _ -> true }
                }
            }

            // 4. Augment the script compilation configuration with the temp dir on the classpath.
            val augmentedConfig = ScriptCompilationConfiguration(scriptCompilationConfiguration) {
                val existingDependencies = scriptCompilationConfiguration[ScriptCompilationConfiguration.dependencies].orEmpty()
                set(
                    ScriptCompilationConfiguration.dependencies,
                    existingDependencies + JvmDependency(tempDir.toFile())
                )
            }

            // 5. Build a fresh K2ReplCompilationState.
            val state = K2ReplCompiler.createCompilationState(
                messageCollector,
                disposable,
                augmentedConfig,
                statelessHostConfiguration,
            )

            // 6. Install the capture hooks:
            //    * early — populates `sessionRef` so the artifact-backed history provider can look
            //      up deserialized prior-snippet symbols against the *live* source session during
            //      resolution (this is the only point at which the source session is known and not
            //      yet consumed by `runResolution`);
            //    * post-compile — captures the FirReplSnippet + GenerationState so we can emit a
            //      new SnippetArtifact.
            state.sourceSessionReadyObserver = { session ->
                sessionRef.set(session)
                debug("sourceSessionReadyObserver: captured FirSession for snippet=${snippet.name}")
            }
            val capturedRef = AtomicReference<Triple<FirReplSnippet, FirSession, GenerationState>?>()
            state.snippetCompilationObserver = { firSnippet, session, generationState ->
                sessionRef.set(session)
                capturedRef.set(Triple(firSnippet, session, generationState))
            }

            // 7. Drive the compile.
            debug("compile(): driving K2ReplCompiler.compile() for snippet=${snippet.name}")
            val compiler = K2ReplCompiler(state)
            val compileResult = compiler.compile(snippet)

            // Build a [SnippetArtifact] from the capture hook if it fired and codegen produced
            // at least one class file. The hook fires both on full success **and** in
            // best-effort mode (i.e. compile returned [ResultWithDiagnostics.Failure] but
            // codegen still emitted usable bytes) — see `compileImpl` in `K2ReplCompiler.kt`.
            val captured = capturedRef.get()
            val artifactOrNull: SnippetArtifact? = captured?.let { (firSnippet, session, generationState) ->
                val classFileCount = generationState.factory.asList().size
                if (classFileCount == 0) {
                    debug("compile(): capture hook fired but codegen produced 0 class files — best-effort artifact unavailable")
                    null
                } else {
                    buildSnippetArtifactFromCompile(
                        firSnippet = firSnippet,
                        session = session,
                        generationState = generationState,
                        scriptCompilationConfiguration = augmentedConfig,
                        hostConfiguration = statelessHostConfiguration,
                        historyIndex = priorSnippets.size,
                    ).also {
                        debug(
                            "compile(): produced artifact with ${it.classFiles.size} class file(s), " +
                                    "historyIndex=${priorSnippets.size}, compileStatus=${compileResult::class.simpleName}"
                        )
                    }
                }
            }

            return when (compileResult) {
                is ResultWithDiagnostics.Success ->
                    artifactOrNull?.asSuccess()
                        ?: makeFailureResult("stateless REPL: capture hook did not fire — internal error")

                is ResultWithDiagnostics.Failure ->
                    // Best-effort path: the snippet had FIR errors but codegen produced usable
                    // class files. Repackage as Success(artifact, original-reports) so the caller
                    //   (a) sees the same diagnostics on `result.reports` as before, AND
                    //   (b) is able to add the partial artifact to a prior-snippets accumulator,
                    //       so that subsequent snippets resolve declarations that *did* compile.
                    // When no artifact is available, return the original Failure unchanged.
                    if (artifactOrNull != null) {
                        ResultWithDiagnostics.Success(artifactOrNull, compileResult.reports)
                    } else compileResult
            }
        } finally {
            try {
                @OptIn(kotlin.io.path.ExperimentalPathApi::class)
                tempDir.deleteRecursively()
            } catch (_: Throwable) {
                // best-effort cleanup
            }
            // disposable is disposed by withMessageCollectorAndDisposable
        }
    }

    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    private fun writeClassFiles(tempDir: Path, artifact: SnippetArtifact, sidecar: SnippetArtifactSidecar) {
        for ((internalName, bytes) in artifact.classFiles) {
            val rel = internalName + ".class"
            val target = tempDir.resolve(rel.replace('/', File.separatorChar))
            target.parent?.createDirectories()
            target.writeBytes(bytes)
        }
        debug("writeClassFiles: wrote ${artifact.classFiles.size} file(s) for ${sidecar.snippetName} under ${tempDir.absolutePathString()}")
    }

    companion object {
        private val DEBUG_ENABLED: Boolean =
            System.getProperty("kotlin.scripting.repl.stateless.debug") == "true"

        private fun debug(message: String) {
            if (DEBUG_ENABLED) System.err.println("[STATELESS_REPL] $message")
        }
    }
}

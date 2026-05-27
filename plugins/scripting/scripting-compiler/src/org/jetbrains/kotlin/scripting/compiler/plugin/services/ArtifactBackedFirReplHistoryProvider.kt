/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.declarations.FirScriptReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.isReplSnippetDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.originalReplSnippetSymbol
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.extensions.FirReplHistoryProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReplSnippetSymbol
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifact
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactSidecar
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.decodeSidecar

/**
 * A [FirReplHistoryProvider] that reconstructs `FirReplSnippetSymbol` views of prior REPL snippets
 * from a list of portable [SnippetArtifact]s rather than from a live in-memory chain.
 *
 * Lifecycle and threading:
 *  * The provider is constructed by `K2ReplStatelessCompiler` **before** the FIR source session
 *    exists; it is installed into the host configuration under `repl.firReplHistoryProvider`.
 *  * On the first call to [getSnippets] (which the existing `FirReplSnippetResolveExtensionImpl`
 *    does during resolution), the provider asks [sourceSessionProvider] for the session, looks
 *    each prior wrapper class up via `session.symbolProvider.getClassLikeSymbolByClassId`, and
 *    tags every declaration listed in the sidecar with the two REPL-only attributes
 *    (`isReplSnippetDeclaration = true` and `originalReplSnippetSymbol = <reconstructedSymbol>`).
 *
 * **One-session-per-call invariant.** Because the provider mutates declaration attributes on
 * deserialized FIR declarations (which can be shared between sessions in principle), each
 * stateless `compile(...)` call must use a *fresh* `FirSession`. `K2ReplStatelessCompiler`
 * guarantees this by constructing a brand-new `K2ReplCompilationState` per call.
 *
 * @param priorSnippets ordered list of prior-snippet artifacts.
 * @param sourceSessionProvider callback to obtain the source session that has the prior snippets'
 *   wrapper classes available via the library symbol provider. May return `null` until the
 *   session is built; in that case [getSnippets] returns an empty iterable and re-attempts on the
 *   next call.
 */
internal class ArtifactBackedFirReplHistoryProvider(
    private val priorSnippets: List<SnippetArtifact>,
    private val sourceSessionProvider: () -> FirSession?,
) : FirReplHistoryProvider() {

    @Volatile
    private var cached: List<FirReplSnippetSymbol>? = null

    private val decodedSidecars: List<SnippetArtifactSidecar> by lazy {
        priorSnippets.map { it.decodeSidecar() }
    }

    /**
     * The state-object FQ name agreed upon by every prior snippet. The orchestrator validates this
     * against the caller's host configuration before installing the provider.
     */
    val agreedStateObjectFqName: String? by lazy {
        decodedSidecars.asSequence()
            .map { it.stateObjectFqName }
            .filter { it.isNotEmpty() }
            .firstOrNull()
    }

    /**
     * Snapshot of the `isImplicit` flag for every prior snippet, in history order.
     *
     * Exposed for the Q10b read path: callers that walk [getSnippets] and need to distinguish
     * user-authored snippets from implicitly-prepended ones (e.g. JSR-223 binding cells emitted via
     * `prependSyntheticSnippets`) can index this list by the position the corresponding symbol
     * occupies in [getSnippets]'s output.
     *
     * The list is `priorSnippets.size`-long and order-aligned with `priorSnippets`, *not* with the
     * `getSnippets()` result. If `materialize()` skipped an artifact (lookup MISS), that index is
     * still present here — consumers wanting the [FirReplSnippetSymbol]→`isImplicit` mapping should
     * look up the symbol's owning sidecar via [findSidecarFor] instead.
     */
    val implicitFlags: List<Boolean> by lazy {
        decodedSidecars.map { it.isImplicit }
    }

    /**
     * Returns the [SnippetArtifactSidecar] whose reconstructed symbol equals [symbol], or `null`
     * if [symbol] does not correspond to any prior snippet known to this provider.
     *
     * Cheap O(N) walk — fine for the prototype because [priorSnippets] is bounded by the REPL
     * session length per call.
     */
    fun findSidecarFor(symbol: FirReplSnippetSymbol): SnippetArtifactSidecar? {
        val materialized = cached ?: return null
        val index = materialized.indexOfFirst { it === symbol }
        if (index < 0) return null
        // `cached` may be shorter than `priorSnippets` (lookup misses are skipped). Recover the
        // original sidecar by matching on the wrapper class's short name, which is unique within
        // a REPL session.
        val sidecarName = materialized[index].snippetClassSymbol.classId.shortClassName.asString()
        return decodedSidecars.firstOrNull {
            it.snippetClassInternalName.substringAfterLast('/').substringAfterLast('$') == sidecarName
        }
    }

    /** `true` if [symbol] corresponds to a prior snippet that was implicitly prepended. */
    fun isImplicit(symbol: FirReplSnippetSymbol): Boolean = findSidecarFor(symbol)?.isImplicit == true

    override fun getSnippets(): Iterable<FirReplSnippetSymbol> {
        cached?.let { return it }
        val session = sourceSessionProvider() ?: run {
            debug("getSnippets(): source session not ready yet — returning empty")
            return emptyList()
        }
        return materialize(session).also { cached = it }
    }

    override fun putSnippet(symbol: FirReplSnippetSymbol) {
        // no-op: the new snippet is the consumer's responsibility; we do not retain it
    }

    override fun isFirstSnippet(symbol: FirReplSnippetSymbol): Boolean {
        val list = cached ?: return false
        return list.firstOrNull() === symbol
    }

    override fun getSnippetCount(): Int = priorSnippets.size

    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    private fun materialize(session: FirSession): List<FirReplSnippetSymbol> {
        val result = ArrayList<FirReplSnippetSymbol>(priorSnippets.size)
        for ((index, artifact) in priorSnippets.withIndex()) {
            val sidecar = decodedSidecars[index]
            val classId = sidecar.toClassId()
            val classSymbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol
            if (classSymbol == null) {
                debug("materialize: lookup MISS for snippet[$index] $classId")
                continue
            }
            debug("materialize: lookup HIT for snippet[$index] $classId")

            val reconstructedSymbol = FirReplSnippetSymbol(classSymbol)

            // Bind a minimal FirReplSnippet to the symbol so that downstream code which reads
            // `symbol.moduleData` (such as `FirReplSnippetResolveExtensionImpl.getImportsFromHistory`)
            // does not throw "Fir is not initialized". The body is intentionally a stub:
            //  * `moduleData` returns the session's module data (so `.session.firProvider` is reachable);
            //  * `snippetClass` is the deserialized wrapper class — the only field downstream code reads
            //    for prior-snippet declarations beyond `moduleData`;
            //  * `evalFunctionSymbol` is taken from the wrapper class's `$$eval` declaration when present;
            //  * `source` throws if anyone tries to read it — the existing resolve / Fir2Ir extensions
            //    never touch a *prior* snippet's `source`.
            val classFir = classSymbol.fir
            val evalSymbol = findEvalSymbol(classSymbol)
            ReconstructedFirReplSnippet(
                snippetName = sidecar.snippetName,
                snippetModuleData = classFir.moduleData,
                snippetClassFir = classFir,
                snippetSymbol = reconstructedSymbol,
                evalSymbol = evalSymbol,
            )

            val byName = sidecar.replSnippetDeclarations.associateBy { it.name }
            var tagged = 0
            for (declSymbol in classSymbol.declarationSymbols) {
                val fir = declSymbol.fir
                val name = when (fir) {
                    is FirProperty -> fir.name.asString()
                    is FirNamedFunction -> fir.name.asString()
                    is FirRegularClass -> fir.name.asString()
                    is FirTypeAlias -> fir.name.asString()
                    else -> null
                } ?: continue
                if (byName.containsKey(name)) {
                    fir.isReplSnippetDeclaration = true
                    fir.originalReplSnippetSymbol = reconstructedSymbol
                    tagged++
                }
            }
            // Note: the sidecar carries `visibility` and `returnTypeSignature` per [MemberRef] but
            // this provider does **not** consume them at materialise time today — the resolver
            // already sees the deserialised declarations' real visibility/return type via
            // `.kotlin_metadata`. The fields are recorded so that *downstream* tooling (e.g. IDE
            // inspections, debugger, or a future cross-snippet anonymous-return-type checker) can
            // reason about prior-snippet shapes without re-loading the wrapper class. See
            // `iterations/2026-05-27_stateless-repl-sidecar-v3.md` for the rationale.
            debug("materialize: tagged $tagged/${classSymbol.declarationSymbols.size} declarations on snippet[$index] (${sidecar.snippetName})")

            result += reconstructedSymbol
        }
        return result
    }

    companion object {
        private val DEBUG_ENABLED: Boolean =
            System.getProperty("kotlin.scripting.repl.stateless.debug") == "true"

        private fun debug(message: String) {
            if (DEBUG_ENABLED) System.err.println("[STATELESS_REPL] $message")
        }
    }
}

/** Parse the sidecar's package + internal-name pair into a [ClassId]. */
private fun SnippetArtifactSidecar.toClassId(): ClassId {
    val pkgSlashed = packageFqName.replace('.', '/')
    val relative = when {
        pkgSlashed.isEmpty() -> snippetClassInternalName
        snippetClassInternalName.startsWith("$pkgSlashed/") -> snippetClassInternalName.removePrefix("$pkgSlashed/")
        else -> snippetClassInternalName
    }
    val relativeFq = relative.replace('$', '.')
    return ClassId(FqName(packageFqName), FqName(relativeFq), /* isLocal = */ false)
}

/**
 * Look up `$$eval` on the deserialized wrapper class, if present. Returns `null` if not found —
 * acceptable for the prototype because the only consumer of [FirReplSnippet.evalFunctionSymbol] is
 * code that operates on the *current* snippet, not prior ones; in the unlikely event a future
 * consumer reads this for a prior snippet, an empty placeholder symbol is supplied.
 */
@OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
private fun findEvalSymbol(classSymbol: FirRegularClassSymbol): FirNamedFunctionSymbol? {
    for (decl in classSymbol.fir.declarations) {
        if (decl is FirNamedFunction && decl.name.asString() == "\$\$eval") {
            return decl.symbol
        }
    }
    return null
}

/**
 * A minimal stub of [FirReplSnippet] sufficient for the resolve-extension code path that reads
 * `symbol.moduleData` and `symbol.snippetClassSymbol.declarationSymbols`. All other fields are
 * unused by prior-snippet consumers in the current pipeline; [source] throws if anyone reads it.
 */
@OptIn(FirImplementationDetail::class)
private class ReconstructedFirReplSnippet(
    snippetName: String,
    snippetModuleData: FirModuleData,
    snippetClassFir: FirRegularClass,
    snippetSymbol: FirReplSnippetSymbol,
    evalSymbol: FirNamedFunctionSymbol?,
) : FirReplSnippet() {
    override val annotations: List<FirAnnotation> = emptyList()
    override val moduleData: FirModuleData = snippetModuleData
    override val origin: FirDeclarationOrigin = FirDeclarationOrigin.Library
    override val attributes: FirDeclarationAttributes = FirDeclarationAttributes()
    override val name: Name = Name.identifier(snippetName)
    override val symbol: FirReplSnippetSymbol = snippetSymbol
    override val source: KtSourceElement
        get() = throw UnsupportedOperationException(
            "ReconstructedFirReplSnippet has no source — reading source on a prior REPL snippet stub is not supported"
        )
    override val receivers: List<FirScriptReceiverParameter> = emptyList()
    override var snippetClass: FirRegularClass = snippetClassFir
    override val evalFunctionSymbol: FirNamedFunctionSymbol =
        evalSymbol ?: FirNamedFunctionSymbol(
            org.jetbrains.kotlin.name.CallableId(snippetClassFir.symbol.classId, Name.identifier("\$\$eval"))
        )

    init {
        symbol.bind(this)
    }

    override fun replaceAnnotations(newAnnotations: List<FirAnnotation>) {
        // no-op — stub
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): ReconstructedFirReplSnippet = this
    override fun <D> transformReceivers(transformer: FirTransformer<D>, data: D): ReconstructedFirReplSnippet = this
    override fun <D> transformSnippetClass(transformer: FirTransformer<D>, data: D): ReconstructedFirReplSnippet = this

    override fun <R, D> acceptChildren(visitor: org.jetbrains.kotlin.fir.visitors.FirVisitor<R, D>, data: D) {}
    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): ReconstructedFirReplSnippet = this
}

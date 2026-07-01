/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirDeclarationAttributes
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.declarations.FirScriptReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.declarations.builder.buildImport
import org.jetbrains.kotlin.fir.declarations.utils.compilerPluginMetadata
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
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.REPL_SIDECAR_PLUGIN_ID
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifact
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactHeader
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactSidecar
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactSidecarProtoCodec
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.decodeHeader

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

    /**
     * Identity-keyed mapping from a reconstructed [FirReplSnippetSymbol] back to the **embedded**
     * [SnippetArtifactSidecar] (read from the wrapper class's `.kotlin_metadata`) it was
     * materialised from.
     *
     * Populated as a side-effect of [materialize] (in lock-step with [cached]); read by
     * [getSnippetImports] to project the sidecar's `ImportEntry`s into [FirImport]s without a
     * second `materialize`-style walk. Identity-based (`===`) because two prior snippets that
     * happen to share a wrapper-class short name would otherwise collide; `FirReplSnippetSymbol`
     * has no inherent identity beyond reference equality. A `null` value records a prior snippet
     * whose wrapper class carried no embedded sidecar (so it contributes no imports).
     */
    private val symbolToEmbeddedSidecar: MutableMap<FirReplSnippetSymbol, SnippetArtifactSidecar?> = HashMap()

    private val decodedHeaders: List<SnippetArtifactHeader> by lazy {
        priorSnippets.map { it.decodeHeader() }
    }

    /**
     * The state-object FQ name agreed upon by every prior snippet. The orchestrator validates this
     * against the caller's host configuration before installing the provider.
     */
    val agreedStateObjectFqName: String? by lazy {
        decodedHeaders.asSequence()
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
     * look up the symbol's owning header via [findHeaderFor] instead.
     */
    val implicitFlags: List<Boolean> by lazy {
        decodedHeaders.map { it.isImplicit }
    }

    /**
     * Returns the [SnippetArtifactHeader] whose reconstructed symbol equals [symbol], or `null`
     * if [symbol] does not correspond to any prior snippet known to this provider.
     *
     * Cheap O(N) walk — fine for the prototype because [priorSnippets] is bounded by the REPL
     * session length per call.
     */
    fun findHeaderFor(symbol: FirReplSnippetSymbol): SnippetArtifactHeader? {
        val materialized = cached ?: return null
        val index = materialized.indexOfFirst { it === symbol }
        if (index < 0) return null
        // `cached` may be shorter than `priorSnippets` (lookup misses are skipped). Recover the
        // original header by matching on the wrapper class's short name, which is unique within
        // a REPL session.
        val headerName = materialized[index].snippetClassSymbol.classId.shortClassName.asString()
        return decodedHeaders.firstOrNull {
            it.snippetClassInternalName.substringAfterLast('/').substringAfterLast('$') == headerName
        }
    }

    /** `true` if [symbol] corresponds to a prior snippet that was implicitly prepended. */
    fun isImplicit(symbol: FirReplSnippetSymbol): Boolean = findHeaderFor(symbol)?.isImplicit == true

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
        // The snippet currently being compiled is never part of our reconstructed prior-history
        // list (we only materialise priors 1..N-1). So when there are no priors, the snippet being
        // asked about is necessarily the *first* snippet of the session.
        //
        // This matters for codegen: `Fir2IrReplSnippetConfiguratorExtensionImpl.prepareSnippet`
        // only generates the shared REPL state object (`ReplState`, the `HashMap` singleton every
        // subsequent snippet references) for the first snippet. Returning `false` here for the
        // empty-priors case (the previous behaviour) made the first stateless snippet *reference* a
        // `ReplState` class that was never emitted into its artifact — surfacing at eval time as
        // `NoClassDefFoundError: ReplState`. The stateless diagnostics corpus never caught this
        // because it checks compile-time diagnostics only, not execution.
        if (priorSnippets.isEmpty()) return true
        val list = cached ?: return false
        return list.firstOrNull() === symbol
    }

    override fun getSnippetCount(): Int = priorSnippets.size

    /**
     * Project the originating sidecar's [SnippetArtifactSidecar.ImportEntry] list into
     * [FirImport]s.
     *
     * Returns `null` for symbols this provider does not own (so the resolver falls back to the
     * default firProvider-based path), and an empty list when the sidecar carries no imports
     * (which **asserts** "no imports", not "unknown").
     *
     * The synthesized `FirImport`s carry no source element (the original `.kts` is gone) and no
     * resolution status — they are equivalent to "as-written" imports, and the regular
     * `ImportTransformer` step will resolve them against the source session when the consumer
     * snippet is processed. This is the read-side complement of `SnippetArtifactEmission`'s
     * import capture and the empirical fix for the `import_visible_in_next_snippet` diagnostic
     * in the stateless suite. See `iterations/2026-05-28_stateless-repl-read-side-wiring.md`.
     */
    override fun getSnippetImports(symbol: FirReplSnippetSymbol): List<FirImport>? {
        // Force materialise so symbolToEmbeddedSidecar is populated when this hook fires *before*
        // the resolver's first call to getSnippets() (it doesn't today, but defensive).
        cached ?: getSnippets()
        val sidecar = symbolToEmbeddedSidecar[symbol] ?: return null
        return sidecar.imports.map { entry ->
            buildImport {
                // No source: the original .kts is not available cross-compile; the resolver
                // treats import sources as advisory (used for diagnostics / IDE only).
                source = null
                importedFqName = FqName(entry.fqName)
                isAllUnder = entry.isAllUnder
                aliasName = entry.aliasName?.let { Name.identifier(it) }
            }
        }
    }

    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    private fun materialize(session: FirSession): List<FirReplSnippetSymbol> {
        val result = ArrayList<FirReplSnippetSymbol>(priorSnippets.size)
        for ([index, artifact] in priorSnippets.withIndex()) {
            val header = decodedHeaders[index]
            val classId = header.toClassId()
            val classSymbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol
            if (classSymbol == null) {
                debug("materialize: lookup MISS for snippet[$index] $classId")
                continue
            }

            // After the "full cut", the reconstruction payload (declarations + visibilities +
            // imports) lives ONLY in the wrapper class's `.kotlin_metadata`, embedded via the
            // generic `CompilerPluginData` channel (keyed by [REPL_SIDECAR_PLUGIN_ID]). The
            // out-of-band [header] supplies only the class id used for the lookup above, the snippet
            // name, and the config-only flags (`isImplicit`, read via [findHeaderFor]); the
            // frontend-derivable fields are read from the embedded sidecar. A snippet produced by
            // the stateless compiler always carries it; if it is somehow absent we still surface the
            // snippet in history (so it stays visible) but tag no declarations and project no
            // imports.
            val embeddedSidecar = readEmbeddedSidecar(classSymbol)
            debug("materialize: lookup HIT for snippet[$index] $classId (embedded sidecar=${embeddedSidecar != null})")

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
                snippetName = header.snippetName,
                snippetModuleData = classFir.moduleData,
                snippetClassFir = classFir,
                snippetSymbol = reconstructedSymbol,
                evalSymbol = evalSymbol,
            )

            val byName = embeddedSidecar?.replSnippetDeclarations.orEmpty().associateBy { it.name }
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

                    // Restamp visibility from the sidecar onto the materialised member.
                    //
                    // Why: REPL snippet declarations are JVM-emitted with `public` access on the
                    // wrapper class so that subsequent snippets can reference them at runtime via
                    // reflection (stateful path) or direct class-file linkage (stateless path).
                    // As a side-effect, `.kotlin_metadata` records the elevated `public`
                    // visibility rather than the source-level visibility the user wrote. When the
                    // stateless reader walks `classSymbol.declarationSymbols`, every member shows
                    // up as PUBLIC — and the FIR visibility checker happily lets a subsequent
                    // snippet reference a member the user wrote as `private`.
                    //
                    // The sidecar [MemberRef.visibility] is the source of truth (captured at
                    // emit time from `FirMemberDeclaration.status.visibility` before any
                    // metadata-level elevation). Project it back onto the materialised FIR so
                    // the resolver's existing visibility-check path fires `INVISIBLE_REFERENCE`
                    // cross-snippet for `private`/`protected` declarations — matching the
                    // stateful semantics on which the golden testdata is gated. Closes the
                    // `property_visibility` diagnostic on the stateless path.
                    val sidecarVisibility = byName[name]?.visibility?.toFirVisibility()
                    if (sidecarVisibility != null && fir is FirMemberDeclaration) {
                        restampVisibility(fir, sidecarVisibility, classSymbol)
                    }
                }
            }
            // Note: the sidecar's [returnTypeSignature] is **not** consumed at materialise time
            // today — the deserialised `.kotlin_metadata` already carries the real type. The
            // field is recorded so that downstream tooling (e.g. IDE inspections, debugger,
            // or a future cross-snippet anonymous-return-type checker) can reason about
            // prior-snippet shapes without re-loading the wrapper class. See
            // `iterations/2026-05-27_stateless-repl-sidecar-v3.md` for the rationale.
            debug("materialize: tagged $tagged/${classSymbol.declarationSymbols.size} declarations on snippet[$index] (${header.snippetName})")

            symbolToEmbeddedSidecar[reconstructedSymbol] = embeddedSidecar
            result += reconstructedSymbol
        }
        return result
    }

    /**
     * Decode the [SnippetArtifactSidecar] embedded in [classSymbol]'s `.kotlin_metadata` via the
     * generic `ProtoBuf.CompilerPluginData` channel (keyed by [REPL_SIDECAR_PLUGIN_ID]), or `null`
     * if the prior snippet's wrapper class carries no such payload (e.g. it was produced by a
     * compiler version that predates the metadata-embedding write side). After the "full cut" this
     * embedded copy is the **only** source of the reconstruction payload — there is no standalone
     * fallback — so a `null` here means the snippet's declarations/imports cannot be recovered.
     */
    @OptIn(SymbolInternals::class)
    private fun readEmbeddedSidecar(classSymbol: FirRegularClassSymbol): SnippetArtifactSidecar? {
        val bytes = classSymbol.fir.compilerPluginMetadata?.get(REPL_SIDECAR_PLUGIN_ID) ?: return null
        return try {
            SnippetArtifactSidecarProtoCodec.decode(bytes)
        } catch (t: Throwable) {
            debug("readEmbeddedSidecar: failed to decode embedded sidecar for ${classSymbol.classId}: ${t.message}")
            null
        }
    }

    companion object {
        private val DEBUG_ENABLED: Boolean =
            System.getProperty("kotlin.scripting.repl.stateless.debug") == "true"

        private fun debug(message: String) {
            if (DEBUG_ENABLED) System.err.println("[STATELESS_REPL] $message")
        }
    }
}

/** Parse the header's package + internal-name pair into a [ClassId]. */
private fun SnippetArtifactHeader.toClassId(): ClassId {
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
 * Map a sidecar [SnippetArtifactSidecar.MemberRef.Visibility] back to a FIR [Visibility].
 *
 * Returns `null` for [SnippetArtifactSidecar.MemberRef.Visibility.UNKNOWN] — meaning "no opinion,
 * leave the materialised declaration's visibility alone". This mirrors the producer-side
 * `toMemberRefVisibility()` map in `SnippetArtifactEmission.kt`.
 */
private fun SnippetArtifactSidecar.MemberRef.Visibility.toFirVisibility(): Visibility? = when (this) {
    SnippetArtifactSidecar.MemberRef.Visibility.PUBLIC -> Visibilities.Public
    SnippetArtifactSidecar.MemberRef.Visibility.INTERNAL -> Visibilities.Internal
    SnippetArtifactSidecar.MemberRef.Visibility.PROTECTED -> Visibilities.Protected
    SnippetArtifactSidecar.MemberRef.Visibility.PRIVATE -> Visibilities.Private
    SnippetArtifactSidecar.MemberRef.Visibility.UNKNOWN -> null
}

/**
 * Restamp the visibility on a deserialised REPL-snippet member with [newVisibility], preserving
 * the existing [org.jetbrains.kotlin.descriptors.Modality] and status [flags][FirResolvedDeclarationStatusImpl].
 *
 * The replacement keeps the materialised declaration's identity (declaration symbols are not
 * recreated — `replaceStatus` mutates in place); the change is visible to downstream FIR
 * visibility checks that fire during resolution of the consumer snippet.
 *
 * [ownerSymbol] is the wrapper class symbol; passed to `toEffectiveVisibility` so the computed
 * `effectiveVisibility` reflects the visibility narrowed by the wrapper class's own visibility
 * (which, in REPL, is always `public` but the parameter exists for correctness).
 */
private fun restampVisibility(
    fir: FirMemberDeclaration,
    newVisibility: Visibility,
    ownerSymbol: FirRegularClassSymbol,
) {
    val current = fir.status
    val modality = current.modality ?: org.jetbrains.kotlin.descriptors.Modality.FINAL
    val forClass = fir is FirRegularClass || fir is FirTypeAlias
    val newEffective = newVisibility.toEffectiveVisibility(ownerSymbol, forClass = forClass)
    // Use the public `resolved(...)` factory on `FirDeclarationStatusImpl`, which internally
    // calls the package-private 4-arg `FirResolvedDeclarationStatusImpl` constructor and
    // **preserves the existing modifier flags** (e.g. `OVERRIDE`, `OPERATOR`, `INFIX`).
    val newStatus = (current as? org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl)
        ?.resolved(newVisibility, modality, newEffective)
        ?: FirResolvedDeclarationStatusImpl(newVisibility, modality, newEffective)
    fir.replaceStatus(newStatus)
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

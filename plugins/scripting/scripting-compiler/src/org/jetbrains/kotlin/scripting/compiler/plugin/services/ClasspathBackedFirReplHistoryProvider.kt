/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirCallableDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirImport
import org.jetbrains.kotlin.fir.declarations.FirMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.builder.buildImport
import org.jetbrains.kotlin.fir.declarations.utils.isReplSnippetDeclaration
import org.jetbrains.kotlin.fir.declarations.utils.originalReplSnippetSymbol
import org.jetbrains.kotlin.fir.extensions.FirReplHistoryProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReplSnippetSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactSidecar
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.replMemberOverloadSignature

/**
 * A [FirReplHistoryProvider] that serves two kinds of prior REPL snippets for the same compile
 * session, combined in history order:
 *
 *  * **Classpath-reconstructed priors** ([priorClassIds]): snippets compiled in an *earlier*,
 *    already-finished daemon/CLI compile call. Their `FirReplSnippetSymbol` views are reconstructed
 *    purely from their [ClassId]s, with **no artifact blob or header of any kind** — the compiled
 *    classes reach the frontend through the regular classpath (`-cp`), and every other piece of
 *    reconstruction data (declarations, visibilities, imports) is read directly from the class's own
 *    embedded `.kotlin_metadata` sidecar via [readEmbeddedSidecar]/[restampVisibility]/[findEvalSymbol]/
 *    [ReconstructedFirReplSnippet] (`StatelessReplSnippetSupport.kt`). This is what a same-machine
 *    CLI/daemon caller uses to compile a `.kts` source as a *chained* REPL snippet through the
 *    compiler's regular frontend/backend (see `ScriptingProcessSourcesBeforeCompilingExtension` and
 *    the `repl-snippet-prior-class` plugin option): a snippet's own [ClassId] is fully deterministic
 *    from its source file name (`NameUtils.getSnippetTargetClassName`), so a caller that names the
 *    file it writes already knows, with zero round-trip, what to pass here for the next snippet.
 *  * **Live, same-batch siblings** ([putSnippet]): snippets compiled *in this very call*, alongside
 *    [priorClassIds]'s (or an empty list of) genuine priors -- e.g. a synthetic bindings-exposing
 *    snippet followed, in the same daemon compile invocation, by the main snippet that references
 *    its declarations. Unlike a classpath-reconstructed prior, such a sibling has **no compiled
 *    bytecode at all yet** (codegen for the whole module only happens once, after every file's body
 *    is resolved) -- it can never be resolved through the classpath+sidecar mechanism above. Instead,
 *    [putSnippet] is called with its *live* `FirReplSnippetSymbol` directly by the regular REPL body-resolve
 *    machinery ([org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirDeclarationsResolveTransformer],
 *    via [org.jetbrains.kotlin.fir.extensions.FirReplSnippetResolveExtension.updateResolved]) as soon as that
 *    sibling's own body is resolved -- which, since FIR body resolve visits every file of one module
 *    sequentially, always happens before a *later* sibling's body resolve begins. No reconstruction
 *    of any kind is needed for these: their FIR is already fully live in this same session, so
 *    [org.jetbrains.kotlin.scripting.compiler.plugin.services.FirReplSnippetResolveExtensionImpl] can
 *    read declarations/imports straight off them.
 *
 * [getSnippets] returns the classpath-reconstructed priors first, followed by the live same-batch
 * siblings observed so far, in the exact order [putSnippet] was called -- i.e. always in true history
 * order, regardless of which of the two mechanisms a given snippet came through.
 *
 * @param priorClassIds ordered [ClassId]s of genuinely prior snippets (compiled in an earlier call);
 *   their compiled classes must already be on this compile's classpath.
 * @param sourceSessionProvider callback to obtain the source session that has the prior snippets'
 *   wrapper classes available via the library symbol provider. May return `null` until the session
 *   is built; in that case [getSnippets] returns only the live same-batch siblings observed so far,
 *   and re-attempts the classpath-based part on the next call.
 */
internal class ClasspathBackedFirReplHistoryProvider(
    private val priorClassIds: List<ClassId>,
    private val sourceSessionProvider: () -> FirSession?,
) : FirReplHistoryProvider() {

    @Volatile
    private var classpathPriors: List<FirReplSnippetSymbol>? = null

    // Live, same-batch siblings -- see the class KDoc's "Live, same-batch siblings" section. Filled
    // in history order purely by putSnippet (called once per sibling, as soon as its own body is
    // resolved), never by materialize()/getSnippets() itself.
    private val liveBatchSnippets = mutableListOf<FirReplSnippetSymbol>()

    private val symbolToEmbeddedSidecar: MutableMap<FirReplSnippetSymbol, SnippetArtifactSidecar?> = HashMap()

    override fun getSnippets(): Iterable<FirReplSnippetSymbol> {
        val classpathList = classpathPriors ?: run {
            val session = sourceSessionProvider() ?: run {
                statelessReplDebug("getSnippets(): source session not ready yet — returning only the live same-batch siblings observed so far")
                return liveBatchSnippets.toList()
            }
            materialize(session).also { classpathPriors = it }
        }
        return if (liveBatchSnippets.isEmpty()) classpathList else classpathList + liveBatchSnippets
    }

    override fun putSnippet(symbol: FirReplSnippetSymbol) {
        liveBatchSnippets += symbol
    }

    override fun isFirstSnippet(symbol: FirReplSnippetSymbol): Boolean {
        if (priorClassIds.isEmpty() && liveBatchSnippets.isEmpty()) return true
        val list = classpathPriors ?: return false
        return list.firstOrNull() === symbol
    }

    override fun getSnippetCount(): Int = priorClassIds.size + liveBatchSnippets.size

    override fun getSnippetImports(symbol: FirReplSnippetSymbol): List<FirImport>? {
        classpathPriors ?: getSnippets()
        val sidecar = symbolToEmbeddedSidecar[symbol] ?: return null
        return sidecar.imports.map { entry ->
            buildImport {
                source = null
                importedFqName = FqName(entry.fqName)
                isAllUnder = entry.isAllUnder
                aliasName = entry.aliasName?.let { Name.identifier(it) }
            }
        }
    }

    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    private fun materialize(session: FirSession): List<FirReplSnippetSymbol> {
        val result = ArrayList<FirReplSnippetSymbol>(priorClassIds.size)
        for ([index, classId] in priorClassIds.withIndex()) {
            val classSymbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol
            if (classSymbol == null) {
                statelessReplDebug("materialize: lookup MISS for snippet[$index] $classId")
                continue
            }

            val embeddedSidecar = readEmbeddedSidecar(classSymbol)
            statelessReplDebug("materialize: lookup HIT for snippet[$index] $classId (embedded sidecar=${embeddedSidecar != null})")

            val reconstructedSymbol = FirReplSnippetSymbol(classSymbol)
            val classFir = classSymbol.fir
            val evalSymbol = findEvalSymbol(classSymbol)
            ReconstructedFirReplSnippet(
                snippetName = classId.shortClassName.asString(),
                snippetModuleData = classFir.moduleData,
                snippetClassFir = classFir,
                snippetSymbol = reconstructedSymbol,
                evalSymbol = evalSymbol,
            )

            // Grouped by name (not associateBy, which would silently drop all but one MemberRef of
            // an overloaded name): a snippet may declare several functions sharing a name, each with
            // its own source-level visibility, and each must be paired with — and re-tagged from —
            // its own MemberRef. See matchMemberRef for the overload-safe pairing.
            val byName: Map<String, List<SnippetArtifactSidecar.MemberRef>> =
                embeddedSidecar?.replSnippetDeclarations.orEmpty().groupBy { it.name }
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
                val candidates = byName[name] ?: continue
                val matched = matchMemberRef(name, fir, candidates, classId)
                fir.isReplSnippetDeclaration = true
                fir.originalReplSnippetSymbol = reconstructedSymbol
                tagged++

                val sidecarVisibility = matched.visibility.toFirVisibility()
                if (sidecarVisibility != null && fir is FirMemberDeclaration) {
                    restampVisibility(fir, sidecarVisibility, classSymbol)
                }
            }
            statelessReplDebug("materialize: tagged $tagged/${classSymbol.declarationSymbols.size} declarations on snippet[$index] ($classId)")

            symbolToEmbeddedSidecar[reconstructedSymbol] = embeddedSidecar
            result += reconstructedSymbol
        }
        return result
    }

    /**
     * Pairs a deserialised wrapper-class declaration [fir] (named [name]) with the specific
     * [SnippetArtifactSidecar.MemberRef] it originated from, among all [candidates] that share that
     * name.
     *
     * The common case is a single candidate (a unique name) -- returned directly. When a name is
     * shared by several declarations (function overloads), each declaration is paired with the
     * candidate whose overload signature (see [replMemberOverloadSignature]) matches its own, so
     * every overload is re-tagged from — and gets the source-level visibility of — its *own*
     * MemberRef rather than an arbitrary same-named one. A non-callable declaration and a legacy
     * sidecar (both rendering a `null` signature) pair with a `null`-descriptor candidate, so
     * name-only entries still resolve. If no signature matches (e.g. a sidecar produced by an
     * incompatible signature scheme), the first candidate is used as a best-effort fallback and the
     * mismatch is surfaced via [statelessReplDebug].
     */
    private fun matchMemberRef(
        name: String,
        fir: FirDeclaration,
        candidates: List<SnippetArtifactSidecar.MemberRef>,
        classId: ClassId,
    ): SnippetArtifactSidecar.MemberRef {
        if (candidates.size == 1) return candidates.single()
        val signature = (fir as? FirCallableDeclaration)?.let { replMemberOverloadSignature(it) }
        return candidates.firstOrNull { it.descriptor == signature }
            ?: candidates.first().also {
                statelessReplDebug(
                    "materialize: no overload-signature match for '$name' (signature=$signature) among " +
                            "${candidates.size} candidates on $classId; falling back to the first"
                )
            }
    }
}

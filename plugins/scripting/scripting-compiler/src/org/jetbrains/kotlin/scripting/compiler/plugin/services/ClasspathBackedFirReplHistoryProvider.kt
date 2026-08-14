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
 * A [FirReplHistoryProvider] that serves two kinds of previous REPL snippets for the same compile
 * session, combined in history order:
 *
 *  * **Classpath-reconstructed** ([priorClassIds]): snippets compiled in an earlier,
 *    already-finished compile call. Their `FirReplSnippetSymbol` views are reconstructed purely
 *    from their [ClassId]s, which are reachable via the regular classpath. Declarations,
 *    visibilities, and imports come from each class's embedded `.kotlin_metadata` sidecar (see
 *    `readEmbeddedSidecar`, `restampVisibility`, and `findEvalSymbol` in
 *    `StatelessReplSnippetSupport.kt`).
 *  * **Live, same-batch siblings** ([putSnippet]): snippets compiled in this very call, whose FIR
 *    is already live in this session. They have no compiled bytecode yet, so they cannot be
 *    resolved through the classpath-and-sidecar mechanism above.
 *
 * [getSnippets] returns the classpath-reconstructed snippets first, then the live same-batch
 * siblings in the exact order [putSnippet] was called. That is always true history order,
 * regardless of which mechanism a given snippet came through.
 *
 * @param priorClassIds ordered [ClassId]s of previous snippets; their compiled classes must already
 *   be on this compile's classpath.
 * @param sourceSessionProvider callback for the source session that has the previous snippets'
 *   wrapper classes available. May return `null` until the session is built; [getSnippets] then
 *   returns only the live siblings observed so far and retries the classpath lookup on the next call.
 */
internal class ClasspathBackedFirReplHistoryProvider(
    private val priorClassIds: List<ClassId>,
    private val sourceSessionProvider: () -> FirSession?,
) : FirReplHistoryProvider() {

    @Volatile
    private var classpathSnippets: List<FirReplSnippetSymbol>? = null

    // Filled in history order by putSnippet only; never by materialize() or getSnippets().
    private val liveBatchSnippets = mutableListOf<FirReplSnippetSymbol>()

    private val symbolToEmbeddedSidecar: MutableMap<FirReplSnippetSymbol, SnippetArtifactSidecar?> = HashMap()

    override fun getSnippets(): Iterable<FirReplSnippetSymbol> {
        val classpathList = classpathSnippets ?: run {
            val session = sourceSessionProvider() ?: run {
                statelessReplDebug("getSnippets(): source session not ready yet — returning only the live same-batch siblings observed so far")
                return liveBatchSnippets.toList()
            }
            materialize(session).also { classpathSnippets = it }
        }
        return if (liveBatchSnippets.isEmpty()) classpathList else classpathList + liveBatchSnippets
    }

    override fun putSnippet(symbol: FirReplSnippetSymbol) {
        liveBatchSnippets += symbol
    }

    override fun isFirstSnippet(symbol: FirReplSnippetSymbol): Boolean {
        if (priorClassIds.isEmpty() && liveBatchSnippets.isEmpty()) return true
        val list = classpathSnippets ?: return false
        return list.firstOrNull() === symbol
    }

    override fun getSnippetCount(): Int = priorClassIds.size + liveBatchSnippets.size

    override fun getSnippetImports(symbol: FirReplSnippetSymbol): List<FirImport>? {
        classpathSnippets ?: getSnippets()
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

            // groupBy, not associateBy: an overloaded name has several MemberRefs (see matchMemberRef).
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
     * Pairs a deserialized declaration [fir] (named [name]) with the [SnippetArtifactSidecar.MemberRef]
     * it originated from, among [candidates] that share that name. A single candidate is returned
     * directly. Overloads are matched by their [replMemberOverloadSignature] so each gets its own
     * visibility. If no signature matches, falls back to the first candidate and logs via
     * [statelessReplDebug].
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

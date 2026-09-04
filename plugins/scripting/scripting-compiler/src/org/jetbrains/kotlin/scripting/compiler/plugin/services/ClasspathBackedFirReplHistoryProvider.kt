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
 * A [FirReplHistoryProvider] for a compiler that keeps no state between snippets: history is
 * reconstructed from the compiled wrapper classes of [priorClassIds] on the classpath plus their
 * embedded `.kotlin_metadata` sidecars, and combined with the live same-batch siblings reported
 * through [putSnippet] and [putImportedSnippet], which have no bytecode yet.
 *
 * [getSnippets] must return the reconstructed snippets before the live siblings: that is history order.
 *
 * @param sourceSessionProvider may return `null` until the session is built; the classpath lookup
 *   is then retried on the next [getSnippets] call.
 */
internal class ClasspathBackedFirReplHistoryProvider(
    private val priorClassIds: List<ClassId>,
    private val sourceSessionProvider: () -> FirSession?,
) : FirReplHistoryProvider(), FirReplHistoryProviderWithImports {

    @Volatile
    private var classpathSnippets: List<FirReplSnippetSymbol>? = null

    private val liveBatchSnippets = mutableListOf<FirReplSnippetSymbol>()

    /** The subset of [liveBatchSnippets] registered via [putImportedSnippet]: they do not consume the snippet numbers. */
    private val liveImportedSnippets = HashSet<FirReplSnippetSymbol>()

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
        if (symbol !in liveBatchSnippets) liveBatchSnippets += symbol
    }

    override fun putImportedSnippet(symbol: FirReplSnippetSymbol) {
        if (symbol !in liveBatchSnippets) {
            liveBatchSnippets += symbol
            liveImportedSnippets += symbol
        }
    }

    override fun isFirstSnippet(symbol: FirReplSnippetSymbol): Boolean {
        if (priorClassIds.isEmpty() && liveBatchSnippets.isEmpty()) return true
        val list = classpathSnippets ?: return false
        return list.firstOrNull() === symbol
    }

    override fun getSnippetCount(): Int = priorClassIds.size + liveBatchSnippets.size - liveImportedSnippets.size

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

    /** Picks the [SnippetArtifactSidecar.MemberRef] a deserialized declaration originated from, disambiguating overloads by signature. */
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

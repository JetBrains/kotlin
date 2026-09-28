/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
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
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.isReplSnippetDeclaration
import org.jetbrains.kotlin.fir.extensions.FirReplHistoryProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirReplSnippetSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.SnippetArtifactMetadata
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.replMemberOverloadSignature

/**
 * A [FirReplHistoryProvider] that serves two kinds of previous REPL snippets for the same compile
 * session, combined in history order:
 *
 *  * **Classpath-reconstructed** (the prior snippets, seeded by [configuredPriorClassIds]): snippets
 *    compiled in an earlier, compile call. Their `FirReplSnippetSymbol` views are reconstructed from
 *    their [ClassId]s, which are reachable via the compile's classpath. Declarations, visibilities, and
 *    imports come from each class's embedded `.kotlin_metadata` extension.
 *  * **Live, same-batch siblings** ([putSnippet]): snippets compiled in this very call.
 */
internal class ClasspathBackedFirReplHistoryProvider(
    private val configuredPriorClassIds: List<ClassId>,
    private val sourceSessionProvider: () -> FirSession?,
) : FirReplHistoryProvider() {

    @Volatile
    private var classpathSnippets: List<FirReplSnippetSymbol>? = null

    private val liveBatchSnippets = mutableListOf<FirReplSnippetSymbol>()

    private val symbolToEmbeddedMetadata: MutableMap<FirReplSnippetSymbol, SnippetArtifactMetadata?> = HashMap()

    override fun getSnippets(): Iterable<FirReplSnippetSymbol> {
        val classpathList = classpathSnippets ?: run {
            val session = sourceSessionProvider() ?: return liveBatchSnippets.toList()
            materialize(session).also { classpathSnippets = it }
        }
        return if (liveBatchSnippets.isEmpty()) classpathList else classpathList + liveBatchSnippets
    }

    override fun putSnippet(symbol: FirReplSnippetSymbol) {
        liveBatchSnippets += symbol
    }

    override fun isFirstSnippet(symbol: FirReplSnippetSymbol): Boolean {
        if (configuredPriorClassIds.isEmpty() && liveBatchSnippets.isEmpty()) return true
        val list = classpathSnippets ?: return false
        return list.firstOrNull() === symbol
    }

    override fun getSnippetCount(): Int =
        (classpathSnippets?.size ?: configuredPriorClassIds.size) + liveBatchSnippets.size

    @OptIn(SymbolInternals::class)
    fun predecessorClassIdOf(symbol: FirReplSnippetSymbol): ClassId? {
        val history = getSnippets().toList()
        val index = history.indexOfFirst { it === symbol }
        if (index <= 0) return null
        return history[index - 1].fir.snippetClass.symbol.classId
    }

    override fun getSnippetImports(symbol: FirReplSnippetSymbol): List<FirImport>? {
        classpathSnippets ?: getSnippets()
        val metadata = symbolToEmbeddedMetadata[symbol] ?: return null
        return metadata.imports.map { entry ->
            buildImport {
                source = null
                importedFqName = FqName(entry.fqName)
                isAllUnder = entry.isAllUnder
                aliasName = entry.aliasName?.let { Name.identifier(it) }
            }
        }
    }

    private fun resolveChain(session: FirSession): List<ClassId> {
        if (configuredPriorClassIds.isEmpty()) return emptyList()
        val visited = LinkedHashSet(configuredPriorClassIds)
        val recovered = ArrayDeque<ClassId>()
        var current = predecessorOf(session, configuredPriorClassIds.first())
        while (current != null) {
            check(visited.add(current)) {
                "REPL snippet $current is reached twice while walking the session back-links: either the " +
                        "chain is cyclic or the configured prior snippet classes are not in snippet order"
            }
            recovered.addFirst(current)
            current = predecessorOf(session, current)
        }
        return recovered + configuredPriorClassIds
    }

    private fun predecessorOf(session: FirSession, classId: ClassId): ClassId? =
        readEmbeddedChainLink(snippetClassSymbol(session, classId))?.let(ClassId::fromString)

    private fun snippetClassSymbol(session: FirSession, classId: ClassId): FirRegularClassSymbol =
        session.symbolProvider.getClassLikeSymbolByClassId(classId) as? FirRegularClassSymbol
            ?: error("Compiled class of the prior REPL snippet $classId is not on this compile's classpath")

    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    private fun materialize(session: FirSession): List<FirReplSnippetSymbol> {
        val priorClassIds = resolveChain(session)
        val result = ArrayList<FirReplSnippetSymbol>(priorClassIds.size)
        for (classId in priorClassIds) {
            val classSymbol = snippetClassSymbol(session, classId)
            val embeddedMetadata = readEmbeddedMetadata(classSymbol)

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
            val byName: Map<String, List<SnippetArtifactMetadata.MemberRef>> =
                embeddedMetadata?.replSnippetDeclarations.orEmpty().groupBy { it.name }
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
                val matched = matchMemberRef(fir, candidates)
                fir.isReplSnippetDeclaration = true

                val metadataVisibility = matched.visibility.toFirVisibility()
                if (metadataVisibility != null && fir is FirMemberDeclaration) {
                    updateVisibility(fir, metadataVisibility, classSymbol)
                }
            }

            symbolToEmbeddedMetadata[reconstructedSymbol] = embeddedMetadata
            result += reconstructedSymbol
        }
        return result
    }

    private fun matchMemberRef(
        fir: FirDeclaration,
        candidates: List<SnippetArtifactMetadata.MemberRef>,
    ): SnippetArtifactMetadata.MemberRef {
        if (candidates.size == 1) return candidates.single()
        val signature = (fir as? FirCallableDeclaration)?.let { replMemberOverloadSignature(it) }
        return candidates.firstOrNull { it.signature == signature } ?: candidates.first()
    }
}

private fun updateVisibility(
    fir: FirMemberDeclaration,
    newVisibility: Visibility,
    ownerSymbol: FirRegularClassSymbol,
) {
    val current = fir.status
    val modality = current.modality ?: Modality.FINAL
    val forClass = fir is FirRegularClass || fir is FirTypeAlias
    val newEffective = newVisibility.toEffectiveVisibility(ownerSymbol, forClass = forClass)
    val newStatus = (current as? FirDeclarationStatusImpl)
        ?.resolved(newVisibility, modality, newEffective)
        ?: FirResolvedDeclarationStatusImpl(newVisibility, modality, newEffective)
    fir.replaceStatus(newStatus)
}

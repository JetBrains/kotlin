/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
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

/**
 * A [FirReplHistoryProvider] that reconstructs `FirReplSnippetSymbol` views of prior REPL snippets
 * purely from their [ClassId]s, with **no artifact blob or header of any kind** — the prior
 * snippets' compiled classes reach the frontend through the regular classpath (`-cp`), and every
 * other piece of reconstruction data (declarations, visibilities, imports) is read directly from
 * the class's own embedded `.kotlin_metadata` sidecar via [readEmbeddedSidecar]/[restampVisibility]/
 * [findEvalSymbol]/[ReconstructedFirReplSnippet] (`StatelessReplSnippetSupport.kt`).
 *
 * This is what a same-machine CLI/daemon caller uses to compile a `.kts` source as a *chained* REPL
 * snippet through the compiler's regular frontend/backend (see `ScriptingProcessSourcesBeforeCompilingExtension`
 * and the `repl-snippet-prior-class` plugin option): a snippet's own [ClassId] is fully deterministic
 * from its source file name (`NameUtils.getSnippetTargetClassName`), so a caller that names the file
 * it writes already knows, with zero round-trip, what to pass here for the next snippet.
 *
 * @param priorClassIds ordered [ClassId]s of prior snippets (1..N-1); their compiled classes must
 *   already be on this compile's classpath.
 * @param sourceSessionProvider callback to obtain the source session that has the prior snippets'
 *   wrapper classes available via the library symbol provider. May return `null` until the session
 *   is built; in that case [getSnippets] returns an empty iterable and re-attempts on the next call.
 */
internal class ClasspathBackedFirReplHistoryProvider(
    private val priorClassIds: List<ClassId>,
    private val sourceSessionProvider: () -> FirSession?,
) : FirReplHistoryProvider() {

    @Volatile
    private var cached: List<FirReplSnippetSymbol>? = null

    private val symbolToEmbeddedSidecar: MutableMap<FirReplSnippetSymbol, SnippetArtifactSidecar?> = HashMap()

    override fun getSnippets(): Iterable<FirReplSnippetSymbol> {
        cached?.let { return it }
        val session = sourceSessionProvider() ?: run {
            statelessReplDebug("getSnippets(): source session not ready yet — returning empty")
            return emptyList()
        }
        return materialize(session).also { cached = it }
    }

    override fun putSnippet(symbol: FirReplSnippetSymbol) {
        // no-op: the new snippet is the consumer's responsibility; we do not retain it
    }

    override fun isFirstSnippet(symbol: FirReplSnippetSymbol): Boolean {
        if (priorClassIds.isEmpty()) return true
        val list = cached ?: return false
        return list.firstOrNull() === symbol
    }

    override fun getSnippetCount(): Int = priorClassIds.size

    override fun getSnippetImports(symbol: FirReplSnippetSymbol): List<FirImport>? {
        cached ?: getSnippets()
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

                    val sidecarVisibility = byName[name]?.visibility?.toFirVisibility()
                    if (sidecarVisibility != null && fir is FirMemberDeclaration) {
                        restampVisibility(fir, sidecarVisibility, classSymbol)
                    }
                }
            }
            statelessReplDebug("materialize: tagged $tagged/${classSymbol.declarationSymbols.size} declarations on snippet[$index] ($classId)")

            symbolToEmbeddedSidecar[reconstructedSymbol] = embeddedSidecar
            result += reconstructedSymbol
        }
        return result
    }
}

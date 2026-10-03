/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isReplSnippetDeclaration
import org.jetbrains.kotlin.fir.extensions.FirReplHistoryProvider
import org.jetbrains.kotlin.fir.extensions.FirReplSnippetResolveExtension
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.scripting.resolve.FirReplHistoryScope
import kotlin.script.experimental.api.ReplScriptingHostConfigurationKeys
import kotlin.script.experimental.api.repl
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.util.PropertiesCollection

/**
 * The key for passing an implementation of frontend REPL history container. Not optional - should be provided by the REPL implementation!
 *
 * Although default implementation [FirReplSnippetResolveExtensionImpl] is sufficient, due to the extension lifecycle, it cannot
 * be provided by default and should be configured in the REPL implementation.
 */
val ReplScriptingHostConfigurationKeys.firReplHistoryProvider by PropertiesCollection.key<FirReplHistoryProvider>(isTransient = true)

class FirReplHistoryProviderImpl : FirReplHistoryProvider() {
    private val history = LinkedHashSet<FirReplSnippetSymbol>()
    private val importedSnippets = HashSet<FirReplSnippetSymbol>()

    override fun getSnippets(): Iterable<FirReplSnippetSymbol> = history.asIterable()

    override fun putSnippet(symbol: FirReplSnippetSymbol) {
        history.add(symbol)
    }

    override fun putImportedSnippet(symbol: FirReplSnippetSymbol) {
        if (history.add(symbol)) importedSnippets.add(symbol)
    }

    override fun isImportedSnippet(symbol: FirReplSnippetSymbol): Boolean = symbol in importedSnippets

    override fun removeSnippets(symbols: Collection<FirReplSnippetSymbol>) {
        history.removeAll(symbols)
        importedSnippets.removeAll(symbols)
    }

    override fun isFirstSnippet(symbol: FirReplSnippetSymbol): Boolean = history.firstOrNull() == symbol

    override fun getSnippetCount(): Int = history.size - importedSnippets.size
}

class FirReplSnippetResolveExtensionImpl(
    session: FirSession,
    hostConfiguration: ScriptingHostConfiguration,
) : FirReplSnippetResolveExtension(session) {

    override val replHistoryProvider: FirReplHistoryProvider =
        hostConfiguration[ScriptingHostConfiguration.repl.firReplHistoryProvider] ?: FirReplHistoryProviderImpl()

    // The snippets of the same batch are all in the history before resolution starts.
    private fun getPrecedingSnippets(currentSnippet: FirReplSnippet): Sequence<FirReplSnippetSymbol> =
        replHistoryProvider.getSnippets().asSequence().takeWhile { it != currentSnippet.symbol }

    override fun getSnippetHistoryImports(snippet: FirReplSnippet): List<FirImport> =
        getPrecedingSnippets(snippet).flatMap { precedingSnippet ->
            replHistoryProvider.getSnippetImports(precedingSnippet)
                ?: precedingSnippet.moduleData.session.firProvider.getFirReplSnippetContainerFile(precedingSnippet)?.imports.orEmpty()
        }.toList()

    @OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
    override fun getSnippetScope(currentSnippet: FirReplSnippet, useSiteSession: FirSession): FirScope {
        // TODO: consider caching (KT-72975)
        val properties = HashMap<Name, ArrayList<FirVariableSymbol<*>>>()
        val functions = HashMap<Name, ArrayList<FirNamedFunctionSymbol>>() // TODO: find out how overloads should work
        val classLikes = HashMap<Name, FirClassLikeSymbol<*>>()
        getPrecedingSnippets(currentSnippet).forEach { snippet ->
            snippet.snippetClassSymbol.declarationSymbols.filter { it.isReplSnippetDeclaration == true }.forEach { symbol ->
                val it = symbol.fir
                when (it) {
                    is FirProperty -> properties.getOrPut(it.name, { ArrayList() }).add(it.symbol)
                    is FirNamedFunction -> functions.getOrPut(it.name, { ArrayList() }).add(it.symbol)
                    is FirRegularClass -> classLikes.put(it.name, it.symbol)
                    is FirTypeAlias -> classLikes.put(it.name, it.symbol)
                    else -> {}
                }
            }
        }
        return FirReplHistoryScope(properties, functions, classLikes, useSiteSession)
    }

    override fun updateResolved(snippet: FirReplSnippet) {
        replHistoryProvider.putSnippet(snippet.symbol)
    }

    companion object {
        fun getFactory(hostConfiguration: ScriptingHostConfiguration): Factory {
            return Factory { session -> FirReplSnippetResolveExtensionImpl(session, hostConfiguration) }
        }
    }
}

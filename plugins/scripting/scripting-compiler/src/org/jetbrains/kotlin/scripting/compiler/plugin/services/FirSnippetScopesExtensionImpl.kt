/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.compiler.plugin.services

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirSnippet
import org.jetbrains.kotlin.fir.extensions.FirSnippetScopesExtension
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.scripting.compiler.plugin.impl.ReplState

class FirSnippetScopesExtensionImpl(session: FirSession, private val replState: ReplState) : FirSnippetScopesExtension(session) {
    override fun contributeVariablesToReplScope(name: Name, processor: (FirVariableSymbol<*>) -> Unit) {
        replState.findVariable(name)?.let { processor(it.symbol) }
    }

    override fun contributeClassifiersToReplScope(name: Name, processor: (FirClassifierSymbol<*>) -> Unit) {
        replState.processPackages { packageName ->
            val symbol = session.symbolProvider.getClassLikeSymbolByClassId(ClassId(packageName, name))

            if (symbol == null) return@processPackages true

            processor(symbol)
            false
        }
    }

    override fun contributeFunctionsToReplScope(name: Name, processor: (FirNamedFunctionSymbol) -> Unit) {
        replState.processPackages { packageName ->
            val symbols = session.symbolProvider.getTopLevelFunctionSymbols(packageName, name)

            // TODO: resolve ambiguity
            symbols.forEach(processor)
            true
        }
    }

    override fun registerVariables(firSnippet: FirSnippet, variables: List<FirVariableSymbol<*>>) {
        variables.forEach { replState.addDeclaration(it) }
    }

    companion object {
        fun getFactory(replState: ReplState): Factory {
            return Factory { session -> FirSnippetScopesExtensionImpl(session, replState) }
        }
    }
}
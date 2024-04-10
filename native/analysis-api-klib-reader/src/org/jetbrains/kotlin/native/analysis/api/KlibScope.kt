/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.analysis.api

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.lifetime.KtLifetimeToken
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.scopes.KtScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KtSymbolWithVisibility
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.name.Name

/**
 * Top-level scope of the given [KtLibraryModule] (if it is based upon a klib).
 *
 * Mostly acts as a convenient wrapper around [readKlibDeclarationAddresses] for APIs built around [KtScope].
 */
public class KlibScope(
    private val libraryModule: KtLibraryModule,
    private val analysisSession: KtAnalysisSession,
) : KtScope {

    override val token: KtLifetimeToken
        get() = analysisSession.token

    private val addresses: Set<KlibDeclarationAddress> by lazy {
        libraryModule.readKlibDeclarationAddresses() ?: emptySet()
    }

    override fun getCallableSymbols(nameFilter: KtScopeNameFilter): Sequence<KtCallableSymbol> = with(analysisSession) {
        addresses.asSequence()
            .filterIsInstance<KlibCallableAddress>()
            // TODO: Check if it works
            .filter { nameFilter(it.callableName) }
            .flatMap { it.getCallableSymbols() }
            .filter { it is KtSymbolWithVisibility && it.visibility.isPublicAPI }
    }

    override fun getCallableSymbols(names: Collection<Name>): Sequence<KtCallableSymbol> =
        getCallableSymbols { it in names }


    override fun getClassifierSymbols(nameFilter: KtScopeNameFilter): Sequence<KtClassifierSymbol> = with(analysisSession) {
        addresses.asSequence()
            // TODO: Name filter
            .filterIsInstance<KlibClassifierAddress>()
            .mapNotNull {
                when (it) {
                    is KlibClassAddress -> it.getClassOrObjectSymbol()
                    is KlibTypeAliasAddress -> it.getTypeAliasSymbol()
                }
            }
            .filter { it is KtSymbolWithVisibility && it.visibility.isPublicAPI }
    }

    override fun getClassifierSymbols(names: Collection<Name>): Sequence<KtClassifierSymbol> =
        getClassifierSymbols { it in names }

    override fun getConstructors(): Sequence<KtConstructorSymbol> = emptySequence()

    override fun getPackageSymbols(nameFilter: KtScopeNameFilter): Sequence<KtPackageSymbol> = emptySequence()

    override fun getPossibleCallableNames(): Set<Name> {
        TODO("Not yet implemented")
    }

    override fun getPossibleClassifierNames(): Set<Name> {
        TODO("Not yet implemented")
    }
}
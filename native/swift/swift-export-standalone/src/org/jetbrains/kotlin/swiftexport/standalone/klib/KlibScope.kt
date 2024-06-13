/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.klib

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.lifetime.KaLifetimeToken
import org.jetbrains.kotlin.analysis.api.scopes.KaScope
import org.jetbrains.kotlin.analysis.api.scopes.KaScopeNameFilter
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.native.analysis.api.*

/**
 * Top-level scope of the given [KtLibraryModule] (if it is based upon a klib).
 *
 * Mostly acts as a convenient wrapper around [readKlibDeclarationAddresses] for APIs built around [KaScope].
 * Some methods are not implemented, and it is OK for now.
 */
public class KlibScope(
    private val libraryModule: KtLibraryModule,
    private val analysisSession: KaSession,
) : KaScope {

    override val token: KaLifetimeToken
        get() = analysisSession.token

    private val addresses: Set<KlibDeclarationAddress> by lazy {
        libraryModule.readKlibDeclarationAddresses() ?: emptySet()
    }

    override fun getCallableSymbols(nameFilter: KaScopeNameFilter): Sequence<KaCallableSymbol> = with(analysisSession) {
        addresses.asSequence()
            .filterIsInstance<KlibCallableAddress>()
            .filter { nameFilter(it.callableName) }
            .flatMap { it.getCallableSymbols() }
    }

    override fun getCallableSymbols(names: Collection<Name>): Sequence<KaCallableSymbol> =
        getCallableSymbols { it in names }

    override fun getClassifierSymbols(nameFilter: KaScopeNameFilter): Sequence<KaClassifierSymbol> = with(analysisSession) {
        addresses.asSequence()
            .filterIsInstance<KlibClassifierAddress>()
            .mapNotNull {
                when (it) {
                    is KlibClassAddress -> it.getClassOrObjectSymbol()
                    is KlibTypeAliasAddress -> it.getTypeAliasSymbol()
                }
            }
            // We don't care about unnamed symbols from the klib.
            .filter { it is KaNamedSymbol && nameFilter(it.name) }
    }

    override fun getClassifierSymbols(names: Collection<Name>): Sequence<KaClassifierSymbol> =
        getClassifierSymbols { it in names }

    // There are no constructors at the top-level scope.
    override fun getConstructors(): Sequence<KaConstructorSymbol> = emptySequence()

    override fun getPackageSymbols(nameFilter: KaScopeNameFilter): Sequence<KaPackageSymbol> =
        throw NotImplementedError("Reading package symbols from ${libraryModule.libraryName} is unsupported. Please report an issue: https://kotl.in/issue")

    override fun getPossibleCallableNames(): Set<Name> =
        addresses.filterIsInstance<KlibCallableAddress>().map { it.callableName }.toSet()

    override fun getPossibleClassifierNames(): Set<Name> =
        addresses.filterIsInstance<KlibClassifierAddress>().map { it.classId.shortClassName }.toSet()
}
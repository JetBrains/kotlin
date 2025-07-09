/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.klib

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassifierSymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.native.analysis.api.*

context(ka: KaSession)
internal fun KaLibraryModule.getAllDeclarations(): Sequence<KaDeclarationSymbol> = sequence {
    val addresses = addresses.asSequence()
    yieldAll(addresses.getAllCallables())
    yieldAll(addresses.getAllClassifier())
}

context(ka: KaSession)
internal fun KaLibraryModule.getAllClassifier(): Sequence<KaDeclarationSymbol> = sequence {
    val addresses = addresses.asSequence()
    yieldAll(addresses.getAllClassifier())
}

context(ka: KaSession)
private fun Sequence<KlibDeclarationAddress>.getAllCallables(): Sequence<KaCallableSymbol> =
    filterIsInstance<KlibCallableAddress>()
        .flatMap { it.getCallableSymbols() }

context(ka: KaSession)
private fun Sequence<KlibDeclarationAddress>.getAllClassifier(): Sequence<KaClassifierSymbol> =
    filterIsInstance<KlibClassifierAddress>()
        .mapNotNull {
            when (it) {
                is KlibClassAddress -> it.getClassOrObjectSymbol()
                is KlibTypeAliasAddress -> it.getTypeAliasSymbol()
            }
        }
        // We don't care about unnamed symbols from the klib.
        .filter { it is KaNamedSymbol }

private val KaLibraryModule.addresses: Set<KlibDeclarationAddress>
    get() = readKlibDeclarationAddresses() ?: emptySet()

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.klib.reader

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*

/**
 * Note: A single [KlibDeclarationAddress] can be shared by multiple symbols.
 * e.g. the [KlibDeclarationAddress] for functions with overloads will be shared.
 *
 * ```kotlin
 * fun foo() = 42
 * fun foo(param: Int) = param
 * ```
 *
 * Both foo functions will live under the same [KlibDeclarationAddress]
 *
 * @return all symbols under the given [KlibDeclarationAddress].
 * @see [getClassOrObjectSymbol]
 * @see [getFunctionSymbols]
 * @see [getPropertySymbols]
 */
context(KtAnalysisSession)
public fun KlibDeclarationAddress.getSymbols(): Sequence<KtSymbol> {
    return when (this) {
        is KlibClassAddress -> getClassOrObjectSymbol()?.let { symbol -> sequenceOf(symbol) } ?: emptySequence()
        is KlibTypealiasAddress -> getTypeAliasSymbol()?.let { symbol -> sequenceOf(symbol) } ?: emptySequence()
        is KlibFunctionAddress -> getFunctionSymbols()
        is KlibPropertyAddress -> getPropertySymbols()
    }
}

/**
 * @see [getSymbols]
 */
context(KtAnalysisSession)
public fun KlibClassAddress.getClassOrObjectSymbol(): KtClassOrObjectSymbol? {
    return getClassOrObjectSymbolByClassId(classId)
}

context(KtAnalysisSession)
public fun KlibTypealiasAddress.getTypeAliasSymbol(): KtTypeAliasSymbol? {
    return getTypeAliasByClassId(classId)
}

/**
 * @see [getSymbols]
 */
context(KtAnalysisSession)
public fun KlibCallableAddress.getCallableSymbols(): Sequence<KtCallableSymbol> {
    return when (this) {
        is KlibFunctionAddress -> getFunctionSymbols()
        is KlibPropertyAddress -> getPropertySymbols()
    }
}

/**
 * @see [getSymbols]
 */
context(KtAnalysisSession)
public fun KlibFunctionAddress.getFunctionSymbols(): Sequence<KtFunctionSymbol> {
    return getTopLevelCallableSymbols(packageFqName, callableName)
        .filterIsInstance<KtFunctionSymbol>()
}

/**
 * @see [getSymbols]
 */
context(KtAnalysisSession)
public fun KlibPropertyAddress.getPropertySymbols(): Sequence<KtPropertySymbol> {
    return getTopLevelCallableSymbols(packageFqName, callableName)
        .filterIsInstance<KtPropertySymbol>()
}


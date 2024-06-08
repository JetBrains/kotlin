/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.analysis.api

import org.jetbrains.kotlin.analysis.api.KaAnalysisNonPublicApi
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.project.structure.KtLibraryModule

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
        is KlibTypeAliasAddress -> getTypeAliasSymbol()?.let { symbol -> sequenceOf(symbol) } ?: emptySequence()
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
        ?.takeIf { symbol -> symbol in this }
}

context(KtAnalysisSession)
public fun KlibTypeAliasAddress.getTypeAliasSymbol(): KtTypeAliasSymbol? {
    return getTypeAliasByClassId(classId)
        ?.takeIf { symbol -> symbol in this }
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
        .filter { symbol -> symbol in this }
}

/**
 * @see [getSymbols]
 */
context(KtAnalysisSession)
public fun KlibPropertyAddress.getPropertySymbols(): Sequence<KtPropertySymbol> {
    return getTopLevelCallableSymbols(packageFqName, callableName)
        .filterIsInstance<KtPropertySymbol>()
        .filter { symbol -> symbol in this }
}

context(KtAnalysisSession)
@OptIn(KaAnalysisNonPublicApi::class)
private operator fun KlibDeclarationAddress.contains(symbol: KtDeclarationSymbol): Boolean {
    val symbolKlibSourceFileName = symbol.klibSourceFileName
    val symbolLibraryModule = symbol.containingModule as? KtLibraryModule ?: return false

    /* check if symbol comes from the same klib library: symbolKlibSourceFile not known -> checking library module */
    if (libraryPath !in symbolLibraryModule.getBinaryRoots()) {
        return false
    }

    /* Check if symbol comes from the same source file (if known) */
    if (this.sourceFileName != null && symbolKlibSourceFileName != sourceFileName) {
        return false
    }

    return true
}

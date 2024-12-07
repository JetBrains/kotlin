/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.analysis.api

import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.projectStructure.KaLibraryModule
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
context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
public fun KlibDeclarationAddress.getSymbols(): Sequence<KaSymbol> {
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
context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
public fun KlibClassAddress.getClassOrObjectSymbol(): KaClassSymbol? {
    return findClass(classId)
        ?.takeIf { symbol -> symbol in this }
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
public fun KlibTypeAliasAddress.getTypeAliasSymbol(): KaTypeAliasSymbol? {
    return findTypeAlias(classId)
        ?.takeIf { symbol -> symbol in this }
}

/**
 * @see [getSymbols]
 */
context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
public fun KlibCallableAddress.getCallableSymbols(): Sequence<KaCallableSymbol> {
    return when (this) {
        is KlibFunctionAddress -> getFunctionSymbols()
        is KlibPropertyAddress -> getPropertySymbols()
    }
}

/**
 * @see [getSymbols]
 */
context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
public fun KlibFunctionAddress.getFunctionSymbols(): Sequence<KaNamedFunctionSymbol> {
    return findTopLevelCallables(packageFqName, callableName)
        .filterIsInstance<KaNamedFunctionSymbol>()
        .filter { symbol -> symbol in this }
}

/**
 * @see [getSymbols]
 */
context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
public fun KlibPropertyAddress.getPropertySymbols(): Sequence<KaPropertySymbol> {
    return findTopLevelCallables(packageFqName, callableName)
        .filterIsInstance<KaPropertySymbol>()
        .filter { symbol -> symbol in this }
}

context(KaSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
@OptIn(KaNonPublicApi::class)
private operator fun KlibDeclarationAddress.contains(symbol: KaDeclarationSymbol): Boolean {
    val symbolKlibSourceFileName = symbol.klibSourceFileName
    val symbolLibraryModule = symbol.containingModule as? KaLibraryModule ?: return false

    /* check if symbol comes from the same klib library: symbolKlibSourceFile not known -> checking library module */
    if (libraryPath !in symbolLibraryModule.binaryRoots) {
        return false
    }

    /* Check if symbol comes from the same source file (if known) */
    if (this.sourceFileName != null && symbolKlibSourceFileName != sourceFileName) {
        return false
    }

    return true
}

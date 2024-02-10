/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.psi.KtFile

internal val StableFileOrder: Comparator<KtFile>
    get() = compareBy<KtFile> { file -> file.packageFqName.asString() }
        .thenComparing { file -> file.name }

internal val StablePropertyOrder: Comparator<KtPropertySymbol> = compareBy { it.name }

internal val StableFunctionOrder: Comparator<KtFunctionSymbol>
    get() = compareBy(
        { it.isConstructor },
        { it.name },
        { it.valueParameters.size },
        // TODO NOW! { KonanManglerDesc.run { it.signatureString(false) } }
    )

internal val StableClassifierOrder: Comparator<KtClassifierSymbol> =
    compareBy<KtClassifierSymbol> { classifier ->
        if (classifier !is KtClassOrObjectSymbol) return@compareBy 0
        else 2
    }.thenComparing { classifier ->
        if (classifier is KtClassLikeSymbol) classifier.classIdIfNonLocal?.toString().orEmpty()
        else ""
    }

internal val StableCallableOrder: Comparator<KtCallableSymbol> = compareBy<KtCallableSymbol> {
    when (it) {
        is KtConstructorSymbol -> 0
        is KtFunctionSymbol -> 1
        is KtPropertySymbol -> 2
        else -> 3
    }
}
    .thenComparing(StablePropertyOrder)
    .thenComparing(StableFunctionOrder)

internal val StableSymbolOrder: Comparator<KtSymbol> = compareBy<KtSymbol> { symbol ->
    when (symbol) {
        is KtFileSymbol -> 0
        is KtClassifierSymbol -> 1
        is KtCallableSymbol -> 2
        else -> Int.MAX_VALUE
    }
}
    .thenComparing(StableClassifierOrder)
    .thenComparing(StableCallableOrder)

private inline fun <T, reified R> Comparator<T>.thenComparing(comparator: Comparator<R>): Comparator<T> where R : T {
    return thenComparing { a, b ->
        if (a is R && b is R) comparator.compare(a, b) else 0
    }
}

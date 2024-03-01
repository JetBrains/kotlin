/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.*

internal val StableFileOrder: Comparator<KtObjCExportFile>
    get() = compareBy<KtObjCExportFile> { file -> file.packageFqName.asString() }
        .thenComparing { file -> file.fileName }

internal val StablePropertyOrder: Comparator<KtPropertySymbol> = compareBy { it.name }

internal val StableFunctionOrder: Comparator<KtFunctionSymbol>
    get() = compareBy(
        { it.isConstructor },
        { it.name },
        { it.valueParameters.size },
        /**
         * Signature order should be added
         *
         * See KT-66066
         * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorKt.makeMethodsOrderStable]
         * { KonanManglerDesc.run { it.signatureString(false) } }
         */
    )

internal val StableConstructorOrder: Comparator<KtConstructorSymbol>
    get() = compareBy(
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
    .thenComparing(StableConstructorOrder)
    .thenComparing(StablePropertyOrder)
    .thenComparing(StableFunctionOrder)

private inline fun <T, reified R> Comparator<T>.thenComparing(comparator: Comparator<R>): Comparator<T> where R : T {
    return thenComparing { a, b ->
        if (a is R && b is R) comparator.compare(a, b) else 0
    }
}

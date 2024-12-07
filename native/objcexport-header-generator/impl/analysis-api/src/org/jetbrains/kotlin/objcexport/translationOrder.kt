/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getStringSignature

internal val StableFileOrder: Comparator<KtObjCExportFile>
    get() = compareBy<KtObjCExportFile> { file -> file.packageFqName.asString() }
        .thenComparing { file -> file.fileName }

internal fun KaSession.getStableFunctionOrder(): Comparator<KaNamedFunctionSymbol> = compareBy(
    { it.isConstructor },
    { it.name },
    { it.valueParameters.size },
    { getStringSignature(it) }
)

internal fun KaSession.getStableConstructorOrder(): Comparator<KaConstructorSymbol> = compareBy(
    { it.valueParameters.size },
    { getStringSignature(it) }
)

internal val StableClassifierOrder: Comparator<KaClassifierSymbol> =
    compareBy<KaClassifierSymbol> { classifier ->
        if (classifier !is KaClassSymbol) return@compareBy 0
        else 2
    }.thenComparing { classifier ->
        if (classifier is KaClassLikeSymbol) classifier.classId?.toString().orEmpty()
        else ""
    }

//Figure out when name is special and enable logic. See KT-66510
//    when (it) {
//        is KtPropertySymbol -> {
//            /**
//             * K1 property names are special: [org.jetbrains.kotlin.name.Name.special] == true
//             * So we need to wrap setter/getter
//             */
//            if (it.setter == null) "<get-${it.name}>"
//            else "<set-${it.name}>"
//        }
//        else -> it.name.toString()
//    }
internal val StableNamedOrder: Comparator<KaNamedSymbol> = compareBy { it.name.toString() }

internal fun KaSession.getStableCallableOrder(): Comparator<KaCallableSymbol> = compareBy<KaCallableSymbol> {
    when (it) {
        is KaConstructorSymbol -> -1
        is KaNamedFunctionSymbol -> 1
        is KaPropertySymbol -> if (isObjCProperty(it)) 2 else 0
        else -> 3
    }
}.thenComparing(getStableConstructorOrder())
    .thenComparing(getStableFunctionOrder())
    .thenComparing(StableNamedOrder)

private inline fun <T, reified R> Comparator<T>.thenComparing(comparator: Comparator<R>): Comparator<T> {
    return thenComparing { a, b ->
        if (a is R && b is R) comparator.compare(a, b) else 0
    }
}

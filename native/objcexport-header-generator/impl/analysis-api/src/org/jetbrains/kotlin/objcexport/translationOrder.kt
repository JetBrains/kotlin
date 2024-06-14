/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol

internal val StableFileOrder: Comparator<KtObjCExportFile>
    get() = compareBy<KtObjCExportFile> { file -> file.packageFqName.asString() }
        .thenComparing { file -> file.fileName }

internal val StableFunctionOrder: Comparator<KaFunctionSymbol>
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

internal val StableConstructorOrder: Comparator<KaConstructorSymbol>
    get() = compareBy(
        { it.valueParameters.size },
        // TODO NOW! { KonanManglerDesc.run { it.signatureString(false) } }
    )

internal val StableClassifierOrder: Comparator<KaClassifierSymbol> =
    compareBy<KaClassifierSymbol> { classifier ->
        if (classifier !is KaClassOrObjectSymbol) return@compareBy 0
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

internal val StableCallableOrder: Comparator<KaCallableSymbol> = compareBy<KaCallableSymbol> {
    when (it) {
        is KaConstructorSymbol -> 0
        is KaFunctionSymbol -> 1
        is KaPropertySymbol -> 2
        else -> 3
    }
}.thenComparing(StableConstructorOrder)
    .thenComparing(StableFunctionOrder)
    .thenComparing(StableNamedOrder)

private inline fun <T, reified R> Comparator<T>.thenComparing(comparator: Comparator<R>): Comparator<T> {
    return thenComparing { a, b ->
        if (a is R && b is R) comparator.compare(a, b) else 0
    }
}

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportClassOrProtocolName
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCNonNullReferenceType
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getDefaultSuperClassOrProtocolName
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getSuperClassTypeNotAny

internal data class KtObjCSuperClassTranslation(
    val superClassName: ObjCExportClassOrProtocolName,
    val superClassGenerics: List<ObjCNonNullReferenceType>,
)

internal fun ObjCExportContext.translateSuperClass(symbol: KaClassSymbol): KtObjCSuperClassTranslation {
    val superClassType = kaSession.getSuperClassTypeNotAny(symbol)
    val defaultName = exportSession.getDefaultSuperClassOrProtocolName()
    val superClassName = if (superClassType == null) {
        defaultName
    } else {
        getSuperClassName(superClassType) ?: defaultName
    }

    val superClassGenerics: List<ObjCNonNullReferenceType> = symbol.superTypes
        .filterIsInstance<KaClassType>()
        .find { type ->
            val classSymbol = type.symbol as? KaClassSymbol ?: return@find false
            classSymbol.classKind.isClass
        }
        ?.typeArguments
        .orEmpty()
        .mapNotNull { typeProjection ->
            val type = typeProjection.type
            if (type == null) {
                null
            } else {
                mapToReferenceTypeIgnoringNullability(type)
            }
        }

    return KtObjCSuperClassTranslation(superClassName, superClassGenerics)
}

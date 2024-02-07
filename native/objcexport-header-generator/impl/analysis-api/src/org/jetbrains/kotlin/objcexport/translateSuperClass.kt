/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportClassOrProtocolName
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCNonNullReferenceType
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getDefaultSuperClassOrProtocolName
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getSuperClassTypeNotAny

internal data class KtObjCSuperClassTranslation(
    val superClassName: ObjCExportClassOrProtocolName,
    val superClassGenerics: List<ObjCNonNullReferenceType>,
)

context(KtAnalysisSession, KtObjCExportSession)
internal fun KtClassOrObjectSymbol.translateSuperClass(): KtObjCSuperClassTranslation {
    val superClassType = getSuperClassTypeNotAny()
    val superClassName = superClassType?.getSuperClassName() ?: getDefaultSuperClassOrProtocolName()

    val superClassGenerics: List<ObjCNonNullReferenceType> = superTypes
        .filterIsInstance<KtNonErrorClassType>()
        .find { type ->
            val classSymbol = type.classSymbol as? KtClassOrObjectSymbol ?: return@find false
            classSymbol.classKind.isClass
        }
        ?.ownTypeArguments
        .orEmpty()
        .mapNotNull { typeProjection -> typeProjection.type?.mapToReferenceTypeIgnoringNullability() }

    return KtObjCSuperClassTranslation(superClassName, superClassGenerics)
}
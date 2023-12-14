/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.KtClassType
import org.jetbrains.kotlin.analysis.api.types.KtClassTypeQualifier.KtResolvedClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.name.NativeStandardInteropNames
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue

context(KtAnalysisSession)
internal fun KtType.isObjCObjectType(): Boolean {
    /* Check if this type represents 'ObjCObject' */
    (this as? KtClassType)?.qualifiers.orEmpty()
        .filterIsInstance<KtResolvedClassTypeQualifier>()
        .asSequence()
        .map { qualifier -> qualifier.symbol }
        .filterIsInstance<KtNamedClassOrObjectSymbol>()
        .any { symbol -> symbol.classIdIfNonLocal == NativeStandardInteropNames.objCObjectClassId }
        .ifTrue { return true }


    /* Check super types */
    return this.getDirectSuperTypes().any { superType -> superType.isObjCObjectType() }
}

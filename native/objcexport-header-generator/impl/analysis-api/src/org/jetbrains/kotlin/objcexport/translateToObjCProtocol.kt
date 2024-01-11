/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.KtClassType
import org.jetbrains.kotlin.analysis.api.types.KtClassTypeQualifier
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCComment
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProtocol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProtocolImpl
import org.jetbrains.kotlin.backend.konan.objcexport.toNameAttributes
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC
import org.jetbrains.kotlin.objcexport.analysisApiUtils.members

context(KtAnalysisSession, KtObjCExportSession)
fun KtClassOrObjectSymbol.translateToObjCProtocol(): ObjCProtocol? {
    // TODO: check if this symbol shall be exposed in the first place
    require(classKind == KtClassKind.INTERFACE)
    if (!isVisibleInObjC()) return null

    // TODO: Check error type!
    val name = getObjCClassOrProtocolName()

    val members = members().flatMap { it.translateToObjCExportStubs() }

    val comment: ObjCComment? = annotationsList.translateToObjCComment()

    return ObjCProtocolImpl(
        name = name.objCName,
        comment = comment,
        origin = getObjCStubOrigin(),
        attributes = name.toNameAttributes(),
        superProtocols = superProtocols(),
        members = members
    )
}

context(KtAnalysisSession, KtObjCExportSession)
internal fun KtClassOrObjectSymbol.superProtocols(): List<String> {
    return superTypes
        .asSequence()
        .filter { type -> !type.isAny }
        .mapNotNull { type -> type as? KtClassType }
        .flatMap { type -> type.qualifiers }
        .mapNotNull { qualifier -> qualifier as? KtClassTypeQualifier.KtResolvedClassTypeQualifier }
        .mapNotNull { it.symbol as? KtClassOrObjectSymbol }
        .filter { superInterface -> superInterface.classKind == KtClassKind.INTERFACE }
        .map { superInterface -> superInterface.getObjCClassOrProtocolName().objCName }
        .toList()
}
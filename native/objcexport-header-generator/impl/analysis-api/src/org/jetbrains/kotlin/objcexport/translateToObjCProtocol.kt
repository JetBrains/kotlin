/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassOrObjectSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCComment
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProtocol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProtocolImpl
import org.jetbrains.kotlin.backend.konan.objcexport.toNameAttributes
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getDeclaredSuperInterfaceSymbols
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isObjCBaseCallable
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC

context(KaSession, KtObjCExportSession)
fun KaClassOrObjectSymbol.translateToObjCProtocol(): ObjCProtocol? {
    // TODO: check if this symbol shall be exposed in the first place
    require(classKind == KaClassKind.INTERFACE)
    if (!isVisibleInObjC()) return null

    // TODO: Check error type!
    val name = getObjCClassOrProtocolName()

    val members = getCallableSymbolsForObjCMemberTranslation()
        .filter { it.isObjCBaseCallable() }
        .sortedWith(StableCallableOrder)
        .flatMap { it.translateToObjCExportStub() }

    val comment: ObjCComment? = annotations.translateToObjCComment()

    return ObjCProtocolImpl(
        name = name.objCName,
        comment = comment,
        origin = getObjCExportStubOrigin(),
        attributes = name.toNameAttributes(),
        superProtocols = superProtocols(),
        members = members
    )
}

context(KaSession, KtObjCExportSession)
internal fun KaClassOrObjectSymbol.superProtocols(): List<String> {
    return getDeclaredSuperInterfaceSymbols()
        .filter { it.isVisibleInObjC() }
        .map { superInterface -> superInterface.getObjCClassOrProtocolName().objCName }
        .toList()
}
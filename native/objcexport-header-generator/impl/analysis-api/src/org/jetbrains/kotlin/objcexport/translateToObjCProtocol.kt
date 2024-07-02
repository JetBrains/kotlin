/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCComment
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProtocol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProtocolImpl
import org.jetbrains.kotlin.backend.konan.objcexport.toNameAttributes
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getDeclaredSuperInterfaceSymbols
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isObjCBaseCallable
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC

fun ObjCExportContext.translateToObjCProtocol(symbol: KaClassSymbol): ObjCProtocol? {
    // TODO: check if this symbol shall be exposed in the first place
    require(symbol.classKind == KaClassKind.INTERFACE)
    if (!analysisSession.isVisibleInObjC(symbol)) return null

    // TODO: Check error type!
    val name = getObjCClassOrProtocolName(symbol)

    val members = analysisSession.getCallableSymbolsForObjCMemberTranslation(symbol)
        .filter { analysisSession.isObjCBaseCallable(it) }
        .sortedWith(StableCallableOrder)
        .flatMap { translateToObjCExportStub(it) }

    val comment: ObjCComment? = analysisSession.translateToObjCComment(symbol.annotations)

    return ObjCProtocolImpl(
        name = name.objCName,
        comment = comment,
        origin = analysisSession.getObjCExportStubOrigin(symbol),
        attributes = name.toNameAttributes(),
        superProtocols = superProtocols(symbol),
        members = members
    )
}

internal fun ObjCExportContext.superProtocols(symbol: KaClassSymbol): List<String> {
    return analysisSession.getDeclaredSuperInterfaceSymbols(symbol)
        .filter { analysisSession.isVisibleInObjC(it) }
        .map { superInterface -> getObjCClassOrProtocolName(superInterface).objCName }
        .toList()
}

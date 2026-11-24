/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.export.utilities.getDeclaredSuperInterfaceSymbols
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCComment
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProtocol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProtocolImpl
import org.jetbrains.kotlin.backend.konan.objcexport.toNameAttributes
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isObjCBaseCallable
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isVisibleInObjC

fun ObjCExportContext.translateToObjCProtocol(symbol: KaClassSymbol): ObjCProtocol? = withClassifierContext(symbol) {
    // TODO: check if this symbol shall be exposed in the first place
    require(symbol.classKind == KaClassKind.INTERFACE)
    if (!isVisibleInObjC(symbol, session = analysisSession)) return@withClassifierContext null

    // TODO: Check error type!
    val name = getObjCClassOrProtocolName(symbol)

    val members = getCallableSymbolsForObjCMemberTranslation(symbol, session = analysisSession)
        .filter { isObjCBaseCallable(it, session = analysisSession) }
        .sortedWith(getStableCallableOrder(session = analysisSession))
        .flatMap { translateToObjCExportStub(it) }

    val comment: ObjCComment? = translateToObjCComment(symbol.annotations, session = analysisSession)

    ObjCProtocolImpl(
        name = name.objCName,
        comment = comment,
        origin = getObjCExportStubOrigin(symbol, session = analysisSession),
        attributes = name.toNameAttributes(),
        superProtocols = superProtocols(symbol),
        members = members
    )
}

internal fun ObjCExportContext.superProtocols(symbol: KaClassSymbol): List<String> {
    return getDeclaredSuperInterfaceSymbols(symbol, session = analysisSession)
        .filter { isVisibleInObjC(it, session = analysisSession) }
        .map { superInterface -> getObjCClassOrProtocolName(superInterface).objCName }
        .toList()
}

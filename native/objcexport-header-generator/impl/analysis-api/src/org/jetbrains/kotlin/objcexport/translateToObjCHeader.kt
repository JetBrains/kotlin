/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.scopes.KtScope
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind.INTERFACE
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCHeader
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProtocol

context(KtAnalysisSession, KtObjCExportSession)
fun KtScope.translateToObjCHeader(): ObjCHeader {
    val declarationsInScope = getAllSymbols()
        .mapNotNull { symbol -> symbol.translateToObjCExportStubOrNull() }
        .toList()

    return ObjCHeader(
        stubs = declarationsInScope,
        classForwardDeclarations = emptySet(),
        protocolForwardDeclarations = declarationsInScope
            .filterIsInstance<ObjCProtocol>()
            .flatMap { it.superProtocols }
            .toSet(),
        additionalImports = emptyList(),
        exportKDoc = configuration.exportKDoc
    )
}

context(KtAnalysisSession, KtObjCExportSession)
internal fun KtSymbol.translateToObjCExportStubOrNull(): ObjCExportStub? {
    return when {
        this is KtClassOrObjectSymbol && classKind == INTERFACE -> translateToObjCProtocol()
        this is KtPropertySymbol -> translateToObjCProperty()
        else -> null
    }
}

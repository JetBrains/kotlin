/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind.CLASS
import org.jetbrains.kotlin.analysis.api.symbols.KtClassKind.INTERFACE
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCHeader
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProtocol
import org.jetbrains.kotlin.psi.KtFile



context(KtAnalysisSession, KtObjCExportSession)
fun translateToObjCHeader(files: List<KtFile>) : ObjCHeader {
    val declarations = files.flatMap { ktFile -> ktFile.translateToObjCExportStubs() }
    return ObjCHeader(
        stubs = declarations,
        classForwardDeclarations = emptySet(),
        protocolForwardDeclarations = declarations
            .filterIsInstance<ObjCProtocol>()
            .flatMap { it.superProtocols }
            .toSet(),
        additionalImports = emptyList(),
        exportKDoc = configuration.exportKDoc
    )
}

context(KtAnalysisSession, KtObjCExportSession)
fun KtFile.translateToObjCExportStubs(): List<ObjCExportStub> {
    return this.getFileSymbol().translateToObjCExportStubs()
}

context(KtAnalysisSession, KtObjCExportSession)
fun KtFileSymbol.translateToObjCExportStubs(): List<ObjCExportStub> {
    return listOfNotNull(translateToObjCTopLevelInterfaceFileFacade()) + getFileScope().getClassifierSymbols()
        .flatMap { classifierSymbol -> classifierSymbol.translateToObjCExportStubs() }
}


context(KtAnalysisSession, KtObjCExportSession)
internal fun KtSymbol.translateToObjCExportStubs(): List<ObjCExportStub> {
    return when {
        this is KtFileSymbol -> translateToObjCExportStubs()
        this is KtClassOrObjectSymbol && classKind == INTERFACE -> listOfNotNull(translateToObjCProtocol())
        this is KtClassOrObjectSymbol && classKind == CLASS -> listOfNotNull(translateToObjCClass())
        this is KtConstructorSymbol -> translateToObjCConstructors()
        this is KtPropertySymbol -> listOfNotNull(translateToObjCProperty())
        this is KtFunctionSymbol -> listOfNotNull(translateToObjCMethod())
        else -> emptyList()
    }
}
/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCHeader
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.addIfNotNull

context(KtAnalysisSession, KtObjCExportSession)
fun translateToObjCHeader(files: List<KtFile>): ObjCHeader {
    val symbols = ArrayDeque<KtSymbol>()
    val translatedObjCExportStubs = mutableListOf<ObjCExportStub>()

    files.sortedWith(StableFileOrder).map { it.getFileSymbol() }.forEach { file ->
        val translatedTopLevelFileFacade = file.translateToObjCTopLevelInterfaceFileFacade()
        translatedObjCExportStubs.addIfNotNull(translatedTopLevelFileFacade)

        symbols.addAll(
            file.getFileScope().getClassifierSymbols().sortedWith(StableClassifierOrder)
        )
    }

    val processResult = queue.process(symbols)

    return ObjCHeader(
        stubs = processResult.stubs,
        classForwardDeclarations = processResult.forwardClasses,
        protocolForwardDeclarations = processResult.forwardProtocols,
        additionalImports = emptyList(),
    )
}

context(KtAnalysisSession, KtObjCExportSession)
internal fun KtCallableSymbol.translateToObjCExportStubs(): List<ObjCExportStub> {
    return when (this) {
        is KtConstructorSymbol -> translateToObjCConstructors()
        is KtPropertySymbol -> listOfNotNull(translateToObjCProperty())
        is KtFunctionSymbol -> listOfNotNull(translateToObjCMethod())
        else -> emptyList()
    }
}
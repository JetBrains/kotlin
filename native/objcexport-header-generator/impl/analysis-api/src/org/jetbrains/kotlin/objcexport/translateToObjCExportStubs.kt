package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub

context(KtAnalysisSession, KtObjCExportSession)
internal fun KtCallableSymbol.translateToObjCExportStubs(): List<ObjCExportStub> {
    return when (this) {
        is KtConstructorSymbol -> translateToObjCConstructors()
        is KtPropertySymbol -> listOfNotNull(translateToObjCProperty())
        is KtFunctionSymbol -> listOfNotNull(translateToObjCMethod())
        is KtEnumEntrySymbol -> listOfNotNull(translateToObjCEnumProperty())
        else -> emptyList()
    }
}
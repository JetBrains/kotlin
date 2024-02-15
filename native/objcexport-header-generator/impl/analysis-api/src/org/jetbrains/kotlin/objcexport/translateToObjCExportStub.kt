package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub

context(KtAnalysisSession, KtObjCExportSession)
internal fun KtCallableSymbol.translateToObjCExportStub(): ObjCExportStub? {
    return when (this) {
        is KtPropertySymbol -> translateToObjCProperty()
        is KtFunctionSymbol -> translateToObjCMethod()
        else -> null
    }
}

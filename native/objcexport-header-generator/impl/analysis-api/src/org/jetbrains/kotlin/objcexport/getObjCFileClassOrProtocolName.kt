package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtFileSymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportFileName
import org.jetbrains.kotlin.backend.konan.objcexport.toIdentifier
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getFileName

context(KtAnalysisSession, KtObjCExportSession)
internal fun KtFileSymbol.getObjCFileClassOrProtocolName(): ObjCExportFileName? {
    val fileName = getFileName() ?: return null
    return (fileName + "Kt").toIdentifier().getObjCFileName()
}
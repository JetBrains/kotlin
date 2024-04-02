package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportFileName
import org.jetbrains.kotlin.backend.konan.objcexport.toIdentifier

context(KtAnalysisSession, KtObjCExportSession)
fun KtResolvedObjCExportFile.getObjCFileClassOrProtocolName(): ObjCExportFileName {
    return (fileName + "Kt").toIdentifier().getObjCFileName()
}
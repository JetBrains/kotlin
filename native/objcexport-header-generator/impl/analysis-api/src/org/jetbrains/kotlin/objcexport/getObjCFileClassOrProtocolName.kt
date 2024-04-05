package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportFileName
import org.jetbrains.kotlin.backend.konan.objcexport.toIdentifier
import org.jetbrains.kotlin.name.NameUtils

private const val PART_CLASS_NAME_SUFFIX = "Kt"

context(KtAnalysisSession, KtObjCExportSession)
fun KtResolvedObjCExportFile.getObjCFileClassOrProtocolName(): ObjCExportFileName {
    return (NameUtils.getPackagePartClassNamePrefix(fileName) + PART_CLASS_NAME_SUFFIX).toIdentifier().getObjCFileName()
}

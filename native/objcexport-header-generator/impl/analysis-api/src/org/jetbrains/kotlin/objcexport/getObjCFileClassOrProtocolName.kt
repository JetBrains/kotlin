package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportFileName
import org.jetbrains.kotlin.backend.konan.objcexport.toIdentifier
import org.jetbrains.kotlin.name.NameUtils

private const val PART_CLASS_NAME_SUFFIX = "Kt"

fun ObjCExportContext.getObjCFileClassOrProtocolName(file: KtResolvedObjCExportFile): ObjCExportFileName {
    val name = (NameUtils.getPackagePartClassNamePrefix(file.fileName) + PART_CLASS_NAME_SUFFIX).toIdentifier()
    return exportSession.getObjCFileName(name)
}

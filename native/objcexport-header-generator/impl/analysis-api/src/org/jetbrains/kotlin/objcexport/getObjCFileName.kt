package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportFileName


internal fun KtObjCExportSession.getObjCFileName(fileName: String): ObjCExportFileName {
    return ObjCExportFileName(
        swiftName = fileName,
        objCName = "${configuration.frameworkName.orEmpty()}$fileName"
    )
}

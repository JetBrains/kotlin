package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportFileName


context(KtObjCExportSession)
@Suppress("CONTEXT_RECEIVERS_DEPRECATED")
internal fun String.getObjCFileName(): ObjCExportFileName {
    return ObjCExportFileName(
        swiftName = this,
        objCName = "${configuration.frameworkName.orEmpty()}$this"
    )
}

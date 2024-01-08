package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportClassOrProtocolName

/**
 * @return The ObjC/Swift name of a class or protocol present in the Kotlin Stdlib.
 * @receiver The name of the Kotlin class or protocol
 */
context(KtObjCExportSession)
internal fun String.getObjCKotlinStdlibClassOrProtocolName() = ObjCExportClassOrProtocolName(
    swiftName = "Kotlin$this",
    objCName = "${configuration.frameworkName.orEmpty()}$this"
)

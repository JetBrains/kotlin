package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportClassOrProtocolName

/**
 * @return The ObjC/Swift name of a class or protocol present in the Kotlin Stdlib.
 * @receiver The name of the Kotlin class or protocol
 */
internal fun KtObjCExportSession.getObjCKotlinStdlibClassOrProtocolName(name: String) = ObjCExportClassOrProtocolName(
    swiftName = "Kotlin$name",
    objCName = "${configuration.frameworkName.orEmpty()}$name"
)

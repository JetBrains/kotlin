package org.jetbrains.kotlin.objcexport

/**
 * [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamer]
 */
internal object ObjCPropertyNames {
    @Suppress("unused")
    const val kotlinThrowableAsErrorMethodName: String = "asError"

    const val objectPropertyName: String = "shared"

    @Suppress("unused")
    const val companionObjectPropertyName: String = "companion"
}
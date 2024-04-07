package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClassType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import org.jetbrains.kotlin.backend.konan.objcexport.swiftNameAttribute

/**
 * See K1: [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportTranslatorImpl.buildThrowableAsErrorMethod]
 */
internal fun buildThrowableAsErrorMethod(): ObjCMethod {
    return ObjCMethod(
        comment = null,
        isInstanceMethod = true,
        returnType = ObjCClassType("NSError"),
        selectors = listOf("asError"),
        parameters = emptyList(),
        attributes = listOf(swiftNameAttribute("asError()")),
        origin = null
    )
}
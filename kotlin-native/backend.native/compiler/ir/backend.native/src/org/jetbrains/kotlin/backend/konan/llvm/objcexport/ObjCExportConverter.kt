/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objcexport

data class ObjCExportConverter(
        val kotlinFqName: String,
        val converterFunctionName: String
)

val ObjCExportConverter.writableTypeInfoSymbolName: String
    get() = "ktypew:$kotlinFqName"

internal object ObjCExportConverterConstants {
    val standardConverters = [
        ObjCExportConverter("kotlin.String", "Kotlin_ObjCExport_CreateRetainedNSStringFromKString"),
        ObjCExportConverter("kotlin.collections.List", "Kotlin_Interop_CreateRetainedNSArrayFromKList"),
        ObjCExportConverter("kotlin.collections.MutableList", "Kotlin_Interop_CreateRetainedNSMutableArrayFromKList"),
        ObjCExportConverter("kotlin.collections.Set", "Kotlin_Interop_CreateRetainedNSSetFromKSet"),
        ObjCExportConverter("kotlin.collections.MutableSet", "Kotlin_Interop_CreateRetainedKotlinMutableSetFromKSet"),
        ObjCExportConverter("kotlin.collections.Map", "Kotlin_Interop_CreateRetainedNSDictionaryFromKMap"),
        ObjCExportConverter("kotlin.collections.MutableMap", "Kotlin_Interop_CreateRetainedKotlinMutableDictionaryFromKMap"),
    ]
}
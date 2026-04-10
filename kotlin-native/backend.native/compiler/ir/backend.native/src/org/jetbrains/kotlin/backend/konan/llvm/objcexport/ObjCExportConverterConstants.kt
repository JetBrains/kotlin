/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objcexport

/**
 * Shared constants for ObjC export converters that bridge Kotlin "special" classes
 * (String and collection interfaces) to their Obj-C counterparts at runtime.
 */
internal object ObjCExportConverterConstants {

    data class ConverterEntry(
            val kotlinFqName: String,
            /** Runtime C function name, e.g. "Kotlin_ObjCExport_CreateRetainedNSStringFromKString". */
            val converterFunctionName: String,
    ) {
        val writableTypeInfoSymbolName: String = "ktypew:$kotlinFqName"
    }

    val standardConverters = listOf(
            ConverterEntry("kotlin.String", "Kotlin_ObjCExport_CreateRetainedNSStringFromKString"),
            ConverterEntry("kotlin.collections.List", "Kotlin_Interop_CreateRetainedNSArrayFromKList"),
            ConverterEntry("kotlin.collections.MutableList", "Kotlin_Interop_CreateRetainedNSMutableArrayFromKList"),
            ConverterEntry("kotlin.collections.Set", "Kotlin_Interop_CreateRetainedNSSetFromKSet"),
            ConverterEntry("kotlin.collections.MutableSet", "Kotlin_Interop_CreateRetainedKotlinMutableSetFromKSet"),
            ConverterEntry("kotlin.collections.Map", "Kotlin_Interop_CreateRetainedNSDictionaryFromKMap"),
            ConverterEntry("kotlin.collections.MutableMap", "Kotlin_Interop_CreateRetainedKotlinMutableDictionaryFromKMap"),
    )
}

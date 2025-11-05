/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCNSEnum
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProperty
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCRawType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCTopLevel
import org.jetbrains.kotlin.backend.konan.objcexport.ObjcExportNativeEnumEntry

internal fun ObjCExportContext.translateNSEnum(symbol: KaClassSymbol, nsEnumTypeName: String, auxiliaryDeclarations: MutableList<ObjCTopLevel>): ObjCProperty {
    auxiliaryDeclarations.add(ObjCNSEnum(nsEnumTypeName, getNSEnumEntries(symbol, nsEnumTypeName)))
    return ObjCProperty(
        "nsEnum",
        null,
        null,
        ObjCRawType(nsEnumTypeName),
        listOf("readonly")
    )
}


private fun ObjCExportContext.getNSEnumEntries(symbol: KaClassSymbol, typeName: String): List<ObjcExportNativeEnumEntry> {
    val staticMembers = with(analysisSession) { symbol.staticDeclaredMemberScope }.callables.toList()
    // Map the enum entries in declaration order, preserving the ordinal
    return staticMembers.filterIsInstance<KaEnumEntrySymbol>().mapIndexed { ordinal, entry ->
        ObjcExportNativeEnumEntry(
            getEnumEntryName(entry, false),
            typeName + getEnumEntryName(entry, true).replaceFirstChar { it.uppercaseChar() },
            ordinal
        )
    }
}


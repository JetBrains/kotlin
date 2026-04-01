/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNSEnumTypeName
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCNSEnum
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProperty
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCRawType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCTopLevel
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStubOrigin

internal fun ObjCExportContext.translateNSEnum(
    symbol: KaClassSymbol,
    origin: ObjCExportStubOrigin,
    nsEnumTypeName: ObjCExportNSEnumTypeName,
    auxiliaryDeclarations: MutableList<ObjCTopLevel>
): ObjCProperty {
    auxiliaryDeclarations.add(
        ObjCNSEnum(nsEnumTypeName.objCName, nsEnumTypeName.swiftName, origin, getNSEnumEntries(symbol, nsEnumTypeName.objCName)))
    return ObjCProperty(
        ObjCPropertyNames.nsEnumPropertyName,
        null,
        null,
        ObjCRawType(nsEnumTypeName.objCName),
        listOf("readonly")
    )
}


private fun ObjCExportContext.getNSEnumEntries(symbol: KaClassSymbol, objCTypeName: String): List<ObjCNSEnum.Entry> {
    val staticMembers = with(analysisSession) { symbol.staticDeclaredMemberScope }.callables.toList()
    // Map the enum entries in declaration order, preserving the ordinal
    return staticMembers.filterIsInstance<KaEnumEntrySymbol>().mapIndexed { ordinal, entry ->
        ObjCNSEnum.Entry(
            getNSEnumEntryName(entry, true),
            objCTypeName + getNSEnumEntryName(entry, false).replaceFirstChar { it.uppercaseChar() },
            ordinal
        )
    }
}


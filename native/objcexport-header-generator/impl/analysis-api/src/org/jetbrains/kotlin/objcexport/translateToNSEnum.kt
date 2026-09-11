/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.backend.konan.mangleIfStdMacro
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNSEnumTypeName
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCNSClosedEnum
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
        ObjCNSClosedEnum(nsEnumTypeName.objCName, nsEnumTypeName.swiftName, origin, getNSEnumEntries(symbol, nsEnumTypeName.objCName)))
    return ObjCProperty(
        ObjCPropertyNames.nsEnumPropertyName,
        null,
        null,
        ObjCRawType(nsEnumTypeName.objCName),
        listOf("readonly")
    )
}


private fun ObjCExportContext.getNSEnumEntries(symbol: KaClassSymbol, objCTypeName: String): List<ObjCNSClosedEnum.Entry> {
    val staticMembers = with(analysisSession) { symbol.staticDeclaredMemberScope }.callables.toList()
    // Map the enum entries in declaration order, preserving the ordinal
    return staticMembers.filterIsInstance<KaEnumEntrySymbol>().mapIndexed { ordinal, entry ->
        ObjCNSClosedEnum.Entry(
            // Swift names that are passed to swift_name() don't need to be mangled as they're passed
            // as string literals. However, NS_SWIFT_NAME() is a macro, that does not receive names this
            // way. If they're not mangled, the generated header file could cause compilation warnings.
            getNSEnumEntryName(entry, true).mangleIfStdMacro(),
            // The NSEnumEntryName is obtained through getObjCPropertyName() which mangles the
            // name by default should it be macro-like name, primarily for translateToObjCProperty.
            // Since we're appending the name here to objCTypeName, there's no need of that mangling.
            // Hence, the suffix drop op.
            objCTypeName + getNSEnumEntryName(entry, false).dropLastWhile { it == '_' }.replaceFirstChar { it.uppercaseChar() },
            ordinal
        )
    }
}


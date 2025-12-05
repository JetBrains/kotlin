/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNSEnumTypeName
import org.jetbrains.kotlin.name.ClassId

/** Returns the NSEnum type for the given enum type if the corresponding annotation is set; null otherwise */
fun ObjCExportContext.getNSEnumTypeName(symbol: KaClassSymbol): ObjCExportNSEnumTypeName? {
    val classId = ClassId.topLevel(KonanFqNames.objCEnum)
    val annotation = symbol.annotations[classId].firstOrNull() ?: return null

    val name = annotation.findArgument("name")?.resolveStringConstantValue()?.ifEmpty { null }
        ?: (getObjCClassOrProtocolName(symbol).objCName + "NSEnum")
    val swiftName = annotation.findArgument("swiftName")?.resolveStringConstantValue()?.ifEmpty { null } ?: name
    return ObjCExportNSEnumTypeName(swiftName = swiftName, objCName = name)
}
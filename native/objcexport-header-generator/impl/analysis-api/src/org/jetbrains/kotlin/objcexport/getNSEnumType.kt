/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/** Returns the NSEnum type for the given enum type if the corresponding annotation is set; null otherwise */
fun ObjCExportContext.getNSEnumType(symbol: KaClassSymbol): String? {
    val classId = ClassId(FqName("kotlin.native"), FqName("ObjCEnum"), false)
    val annotation = symbol.annotations[classId].firstOrNull()
    return if (annotation == null) {
        null
    } else if (annotation.arguments.isEmpty()) {
        getObjCClassOrProtocolName(symbol).toString()
    } else {
        (annotation.arguments[0].expression as KaAnnotationValue.ConstantValue).value.value as String
    }
}
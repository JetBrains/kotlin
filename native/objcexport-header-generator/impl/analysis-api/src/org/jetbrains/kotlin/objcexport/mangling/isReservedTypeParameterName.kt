/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.builtins.StandardNames.FqNames
import org.jetbrains.kotlin.objcexport.ObjCExportContext
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getDefaultSuperClassOrProtocolName
import org.jetbrains.kotlin.objcexport.getObjCKotlinStdlibClassOrProtocolName

/**
 * See K1 implementation [org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportNamerImpl.GenericTypeParameterNameMapping]
 */
internal fun ObjCExportContext.isReservedTypeParameterName(name: String): Boolean {
    val predefinedClassNames = setOf(
        exportSession.getDefaultSuperClassOrProtocolName().objCName,
        exportSession.getObjCKotlinStdlibClassOrProtocolName(FqNames.mutableSet.shortName().asString()).objCName,
        exportSession.getObjCKotlinStdlibClassOrProtocolName(FqNames.mutableMap.shortName().asString()).objCName
    )

    return (reservedTypeParameterNames + predefinedClassNames).contains(name)
}

private val reservedTypeParameterNames = setOf(
    "id", "NSObject", "NSArray", "NSCopying", "NSNumber", "NSInteger",
    "NSUInteger", "NSString", "NSSet", "NSDictionary", "NSMutableArray", "int", "unsigned", "short",
    "char", "long", "float", "double", "int32_t", "int64_t", "int16_t", "int8_t", "unichar"
)
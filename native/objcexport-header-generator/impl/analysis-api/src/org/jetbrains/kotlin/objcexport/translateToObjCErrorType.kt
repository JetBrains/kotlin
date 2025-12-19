/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.api.projectStructure.KaSourceModule
import org.jetbrains.kotlin.analysis.api.types.KaClassErrorType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCClassType
import org.jetbrains.kotlin.objcexport.analysisApiUtils.objCErrorType
import org.jetbrains.kotlin.objcexport.extras.objCTypeExtras
import org.jetbrains.kotlin.objcexport.extras.requiresForwardDeclaration

/**
 * Unresolved type we handle it in two ways:
 * 1. if current module is source module - we return [objCErrorType] class. Example user have `fun foo() = UndefinedType`
 * 2. if we know [KaClassErrorType] we try to construct
 */
internal fun translateToObjCErrorType(
    type: KaType,
    module: KaModule?,
): ObjCClassType {
    val errorType = type as? KaClassErrorType
    return when {
        module is KaSourceModule -> objCErrorType
        errorType != null && errorType.qualifiers.isNotEmpty() -> {
            ObjCClassType(
                className = getUnresolvedClassName(errorType),
                extras = objCTypeExtras {
                    requiresForwardDeclaration = true
                }
            )
        }
        else -> objCErrorType
    }
}

/**
 * `kotlinx.serialization.encoding.Decoder` -> `Decoder`
 */
private fun getUnresolvedClassName(errorType: KaClassErrorType): String =
    errorType.qualifiers.last().name.asString()
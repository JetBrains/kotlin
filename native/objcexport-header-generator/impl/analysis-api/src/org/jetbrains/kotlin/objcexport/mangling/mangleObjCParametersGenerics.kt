/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCGenericTypeParameterUsage
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCNullableReferenceType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCParameter
import org.jetbrains.kotlin.objcexport.ObjCExportContext

internal fun ObjCExportContext.mangleObjCParametersGenerics(parameters: List<ObjCParameter>): List<ObjCParameter> {
    return parameters.map { parameter ->
        val type = parameter.type
        if (type is ObjCNullableReferenceType) {
            ObjCParameter(
                name = parameter.name,
                origin = parameter.origin,
                type = mangleObjCNullableType(type),
                todo = parameter.todo,
                extras = parameter.extras
            )
        } else parameter
    }
}


internal fun ObjCExportContext.mangleObjCNullableType(type: ObjCNullableReferenceType): ObjCNullableReferenceType {
    return if (type.nonNullType is ObjCGenericTypeParameterUsage)
        type.copy(nonNullType = mangleObjCGenericType(type.nonNullType as ObjCGenericTypeParameterUsage))
    else type
}

internal fun ObjCExportContext.mangleObjCGenericType(parameter: ObjCGenericTypeParameterUsage): ObjCGenericTypeParameterUsage {
    return if (isReservedTypeParameterName(parameter.typeName)) {
        return parameter.copy(typeName = parameter.typeName + "_")
    } else parameter
}
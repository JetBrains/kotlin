/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.*
import org.jetbrains.kotlin.objcexport.ObjCExportContext

internal fun ObjCExportContext.mangleObjCMemberGenerics(stub: ObjCExportStub): ObjCExportStub {
    return when (stub) {
        is ObjCMethod -> ObjCMethod(
            comment = stub.comment,
            origin = stub.origin,
            isInstanceMethod = stub.isInstanceMethod,
            returnType = mangleObjCGenericType(stub.returnType),
            selectors = stub.selectors,
            parameters = mangleObjCParametersGenerics(stub.parameters),
            attributes = stub.attributes,
            extras = stub.extras
        )
        is ObjCProperty -> ObjCProperty(
            name = stub.name,
            comment = stub.comment,
            origin = stub.origin,
            type = mangleObjCGenericType(stub.type),
            propertyAttributes = stub.propertyAttributes,
            setterName = stub.setterName,
            getterName = stub.getterName,
            declarationAttributes = stub.declarationAttributes,
            extras = stub.extras,
        )
        else -> stub
    }
}

private fun ObjCExportContext.mangleObjCGenericType(type: ObjCType): ObjCType {
    return if (type is ObjCNullableReferenceType) mangleObjCNullableType(type) else type
}
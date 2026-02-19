/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterface
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterfaceImpl

/**
 * Extensions mangling isn't required by ObjC language itself.
 * But since CLI uses K1 ObjCExport and it does mangled extensions we need to do it as well.
 * See more at [readme.md]
 */
internal fun mangleExtensionFacadesMembers(stubs: List<ObjCExportStub>): List<ObjCExportStub> {
    val methodMangler = ObjCMethodMangler()
    val propertyMangler = ObjCPropertyMangler()
    return stubs.map { stub ->
        if (stub is ObjCInterface && stub.categoryName != null) {
            ObjCInterfaceImpl(
                name = stub.name,
                comment = stub.comment,
                origin = stub.origin,
                attributes = stub.attributes,
                superProtocols = stub.superProtocols,
                members = stub.members.map { member ->
                    propertyMangler.mangle(
                        methodMangler.mangle(member, stub),
                        stub
                    )
                },
                categoryName = stub.categoryName,
                generics = stub.generics,
                superClass = stub.superClass,
                superClassGenerics = stub.superClassGenerics,
                extras = stub.extras
            )
        } else stub
    }
}

internal val ObjCExportStub.isExtensionFacade: Boolean
    get() {
        return this is ObjCInterface && this.categoryName != null
    }
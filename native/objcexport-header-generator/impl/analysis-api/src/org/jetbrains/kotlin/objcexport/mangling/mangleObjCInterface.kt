package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterface
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterfaceImpl

internal fun ObjCInterface.mangleObjCInterface(name: String): ObjCInterface {
    return ObjCInterfaceImpl(
        name = name,
        comment = this.comment,
        origin = this.origin,
        attributes = this.attributes,
        superProtocols = this.superProtocols,
        members = this.members.mangleObjCMethods().mangleObjCProperties(),
        categoryName = this.categoryName,
        generics = this.generics,
        superClass = this.superClass,
        superClassGenerics = this.superClassGenerics,
        extras = this.extras
    )
}
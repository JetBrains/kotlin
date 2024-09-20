package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProtocol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProtocolImpl

internal fun ObjCProtocol.mangleObjCProtocol(name: String): ObjCProtocol {
    return ObjCProtocolImpl(
        name = name,
        comment = this.comment,
        origin = this.origin,
        attributes = this.attributes,
        superProtocols = this.superProtocols,
        members = this.members.mangleObjCMethods().mangleObjCProperties(),
        extras = this.extras
    )
}
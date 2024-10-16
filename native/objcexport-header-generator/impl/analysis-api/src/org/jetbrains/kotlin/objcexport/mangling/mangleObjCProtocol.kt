package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProtocol
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProtocolImpl
import org.jetbrains.kotlin.objcexport.ObjCExportContext

internal fun ObjCExportContext.mangleObjCProtocol(objCProtocol: ObjCProtocol, name: String): ObjCProtocol {
    return ObjCProtocolImpl(
        name = name,
        comment = objCProtocol.comment,
        origin = objCProtocol.origin,
        attributes = objCProtocol.attributes,
        superProtocols = objCProtocol.superProtocols,
        members = mangleObjCProperties(mangleObjCMethods(objCProtocol.members)),
        extras = objCProtocol.extras
    )
}
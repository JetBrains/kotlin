package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInterface
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProtocol
import org.jetbrains.kotlin.objcexport.ObjCExportContext

internal fun ObjCExportContext.mangleObjCStubs(stubs: List<ObjCExportStub>): List<ObjCExportStub> {
    val interfaceSuffixes = hashMapOf<String, String>() // Maps original interface name to suffix: "Foo" -> "___"
    val protocolSuffixes = hashMapOf<String, String>() // Maps original protocol name to suffix: "Foo" -> "___"
    return stubs.map { stub ->
        when (stub) {
            is ObjCInterface -> {
                val stubName = if (stub.categoryName != null) {
                    stub.name // Extensions already merge into unique interfaces
                } else {
                    interfaceSuffixes[stub.name] = if (interfaceSuffixes[stub.name] == null) "" else interfaceSuffixes[stub.name] + "_"
                    stub.name + interfaceSuffixes[stub.name]
                }
                mangleObjCInterface(stub, stubName)
            }
            is ObjCProtocol -> {
                protocolSuffixes[stub.name] = if (protocolSuffixes[stub.name] == null) "" else protocolSuffixes[stub.name] + "_"
                mangleObjCProtocol(stub, stub.name + protocolSuffixes[stub.name])
            }
            else -> stub
        }
    }.run { mangleExtensionFacadesMembers(this) }
}




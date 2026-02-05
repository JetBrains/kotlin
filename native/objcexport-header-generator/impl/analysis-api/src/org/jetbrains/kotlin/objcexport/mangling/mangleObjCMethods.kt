package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import org.jetbrains.kotlin.objcexport.ObjCExportContext


internal fun ObjCExportContext.mangleObjCMethods(
    stubs: List<ObjCExportStub>,
    containingStub: ObjCExportStub,
): List<ObjCExportStub> {
    if (!stubs.hasMethodConflicts()) return stubs
    val mangler = ObjCMethodMangler()
    return stubs.map { member ->
        mangler.mangle(member, containingStub)
    }.map { stub -> mangleObjCMemberGenerics(stub) }
}

/**
 * Determines if methods conflicts exist by comparing `swift_name` attribute.
 * This function isn't for optimization; it avoids handling complex edge case later during mangling.
 */
internal fun List<ObjCExportStub>.hasMethodConflicts(): Boolean {
    val keys = mutableSetOf<String>()
    forEach { method ->
        if (method is ObjCMethod) {
            val key = getMemberKey(method)
            if (keys.add(key)) return true
        }
    }
    return false
}

internal val ObjCMethod.isInstance: String
    get() {
        return if (this.isInstanceMethod) "+" else "-"
    }

internal fun getMemberKey(method: ObjCMethod) =
    method.isInstance + method.selectors.joinToString("")
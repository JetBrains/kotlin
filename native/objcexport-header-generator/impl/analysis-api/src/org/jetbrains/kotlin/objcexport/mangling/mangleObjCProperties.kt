package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProperty
import org.jetbrains.kotlin.objcexport.ObjCExportContext

internal fun ObjCExportContext.mangleObjCProperties(stubs: List<ObjCExportStub>): List<ObjCExportStub> {
    if (!stubs.hasPropertiesConflicts()) return stubs
    val mangler = ObjCPropertyMangler()
    return stubs.map { member ->
        mangler.mangle(member, containingStub = member)
    }.map { stub -> mangleObjCMemberGenerics(stub) }
}

/**
 * Determines if properties conflicts exist by comparing `swift_name` attribute.
 * This function isn't for optimization; it avoids handling complex edge case later during mangling.
 */
internal fun List<ObjCExportStub>.hasPropertiesConflicts(): Boolean {
    val selectors = hashSetOf<String>()
    forEach { member ->
        if (member is ObjCMethod && member.selectors.size == 1 && member.parameters.isEmpty()) {
            if (!selectors.add(member.selectors[0])) return true
        } else if (member is ObjCProperty) {
            if (!selectors.add(member.name)) return true
        }
    }
    return false
}
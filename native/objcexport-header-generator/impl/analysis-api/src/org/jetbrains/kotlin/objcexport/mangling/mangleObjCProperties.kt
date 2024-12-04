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

internal fun getSwiftNameAttribute(property: ObjCProperty) =
    property.declarationAttributes.first { attr -> attr.startsWith("swift_name") }

/**
 * Determines if properties conflicts exist by comparing `swift_name` attribute.
 * This function isn't for optimization; it avoids handling complex edge case later during mangling.
 */
internal fun List<ObjCExportStub>.hasPropertiesConflicts(): Boolean {
    val swiftNameAttributes = hashSetOf<String>()
    forEach { member ->
        if (member is ObjCMethod && member.isSwiftNameMethod()) {
            val attr = getSwiftNameAttribute(member).replace("()", "")
            swiftNameAttributes.add(attr)
        } else if (member is ObjCProperty && member.isSwiftNameProperty()) {
            val attr = getSwiftNameAttribute(member)
            if (!swiftNameAttributes.add(attr)) return true
        }
    }
    return false
}

internal fun ObjCExportStub.isSwiftNameProperty(): Boolean {
    return (this as? ObjCProperty)?.isSwiftNameProperty() ?: false
}

internal fun ObjCProperty.isSwiftNameProperty(): Boolean {
    return declarationAttributes.firstOrNull { attr -> attr.startsWith("swift_name") } != null
}
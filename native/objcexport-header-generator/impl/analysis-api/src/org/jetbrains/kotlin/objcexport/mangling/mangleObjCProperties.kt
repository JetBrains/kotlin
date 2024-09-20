package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCProperty

internal fun List<ObjCExportStub>.mangleObjCProperties(): List<ObjCExportStub> {
    if (!hasPropertiesConflicts()) return this
    val swiftNameAttributes = hashSetOf<String>()
    return map { member ->
        if (member is ObjCProperty) {
            val attr = getSwiftNameAttribute(member)
            if (swiftNameAttributes.contains(attr)) {
                member.copy("getter=${member.name}_")
            } else member
        } else if (member is ObjCMethod && member.isSwiftNameMethod()) {
            swiftNameAttributes.add(getSwiftNameAttribute(member).replace("()", ""))
            member
        } else member
    }
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

internal fun ObjCProperty.isSwiftNameProperty(): Boolean {
    return declarationAttributes.firstOrNull { attr -> attr.startsWith("swift_name") } != null
}
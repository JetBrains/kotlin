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
        if (member.isSwiftNameMethod()) mangler.mangle(member, containingStub)
        else member
    }.map { stub -> mangleObjCMemberGenerics(stub) }
}

internal fun buildMangledSelectors(postfix: String, selectors: List<String>): List<String> {
    return selectors.mapIndexed { index, selector ->
        when (index) {
            selectors.lastIndex -> {
                selector.mangleSelector(postfix)
            }
            else -> selector
        }
    }
}


internal fun buildMangledSwiftNameMethodAttribute(attribute: ObjCMemberDetails, containingStub: ObjCExportStub): String {
    val parameters = attribute.parameters
    val parametersWithoutError = if (attribute.hasErrorParameter) parameters.dropLast(1) else parameters
    val mangledParameters = parametersWithoutError.mapIndexed { index, parameter ->
        if (index == parametersWithoutError.size - 1) parameter.mangleSelector(attribute.postfix)
        else parameter
    }

    val name = if (containingStub.isExtensionFacade && parametersWithoutError.isEmpty()) {
        attribute.name + attribute.postfix
    } else attribute.name

    return "swift_name(\"${name}(${mangledParameters.joinToString(separator = "")})\")"
}

internal fun buildMangledParameters(attribute: ObjCMemberDetails): List<String> {
    return attribute.parameters.mapIndexed { index, parameter ->
        when (index) {
            /** Last parameter goes always mangled */
            attribute.parameters.size - 1 -> parameter + attribute.postfix
            /** Other parameters remain unchanged */
            else -> parameter
        }
    }
}

internal fun ObjCExportStub.isSwiftNameMethod(): Boolean {
    return this is ObjCMethod && isSwiftNameMethod()
}

internal fun ObjCMethod.isSwiftNameMethod(): Boolean {
    return attributes.firstOrNull { attr -> attr.startsWith("swift_name") } != null
}

internal fun ObjCMemberDetails.mangleAttribute(): ObjCMemberDetails {
    return ObjCMemberDetails(name, parameters, isConstructor, postfix + "_", hasErrorParameter = hasErrorParameter)
}

/**
 * Determines if methods conflicts exist by comparing `swift_name` attribute.
 * This function isn't for optimization; it avoids handling complex edge case later during mangling.
 */
internal fun List<ObjCExportStub>.hasMethodConflicts(): Boolean {
    val swiftNameAttributes = mutableSetOf<String>()
    forEach { method ->
        if (method is ObjCMethod && method.isSwiftNameMethod()) {
            val swiftNameAttribute = getMemberKey(method)
            if (swiftNameAttributes.add(swiftNameAttribute)) return true
        }
    }
    return false
}

internal val String.isReceiver: Boolean
    get() {
        return this == "_:"
    }

internal val ObjCMethod.isInstance: String
    get() {
        return if (this.isInstanceMethod) "+" else "-"
    }

internal fun getMemberKey(method: ObjCMethod) =
    method.isInstance + getSwiftNameAttribute(method)

internal fun getSwiftNameAttribute(method: ObjCMethod) =
    method.attributes.first { attr -> attr.startsWith("swift_name") }

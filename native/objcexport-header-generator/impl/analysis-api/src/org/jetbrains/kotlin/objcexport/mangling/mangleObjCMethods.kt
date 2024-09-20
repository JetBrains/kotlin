package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCExportStub
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCInstanceType
import org.jetbrains.kotlin.backend.konan.objcexport.ObjCMethod

/**
 * ObjC method consists of 3 parts, each part needs to be mangled
 * - selectors [buildMangledSelectors]
 * - parameters [buildMangledParameters]
 * - swift_name attribute [buildMangledSwiftNameAttribute]
 */
internal fun List<ObjCExportStub>.mangleObjCMethods(): List<ObjCExportStub> {
    if (!hasMethodConflicts()) return this
    val attributes = mutableMapOf<String, SwiftNameAttribute>()
    return map { member ->
        if (member is ObjCMethod && member.isSwiftNameMethod()) {
            val selector = getSwiftNameAttribute(member)
            val attribute = attributes[selector]
            if (attribute != null) {
                val mangledAttribute = attribute.mangleAttribute()
                val cloned = member.copy(
                    buildMangledSelectors(mangledAttribute),
                    buildMangledParameters(mangledAttribute),
                    buildMangledSwiftNameAttribute(mangledAttribute)
                )
                attributes[selector] = mangledAttribute
                cloned
            } else {
                attributes[selector] = parseSwiftNameAttribute(getSwiftNameAttribute(member))
                member
            }
        } else member
    }
}

internal fun buildMangledSelectors(attribute: SwiftNameAttribute): List<String> {
    return if (attribute.parameters.isEmpty())
        listOf(attribute.methodName)
    else if (attribute.parameters.size == 1) {
        listOf(
            (attribute.methodName + attribute.parameters.first().replaceFirstChar { it.uppercaseChar() }).mangleSelector(attribute.postfix)
        )
    } else {
        attribute.parameters.mapIndexed { index, param ->
            when (index) {
                0 -> {
                    if (param.isReceiver) {
                        attribute.methodName + ":"
                    } else {
                        /** First selector is a combination of a method name and first parameter */
                        attribute.methodName + param.replaceFirstChar { it.uppercaseChar() }
                    }
                }
                /** Last selector always mangled */
                attribute.parameters.size - 1 -> param.mangleSelector(attribute.postfix)
                /** Middle selectors remain unchanged */
                else -> param
            }
        }.toList()
    }
}


private fun buildMangledSwiftNameAttribute(attribute: SwiftNameAttribute): String {
    val parameters = attribute.parameters.mapIndexed { index, parameter ->
        if (index == attribute.parameters.size - 1) {
            parameter.mangleSelector(attribute.postfix)
        } else {
            parameter
        }
    }
    return "swift_name(\"${attribute.methodName}(${parameters.joinToString(separator = "")})\")"
}

private fun buildMangledParameters(attribute: SwiftNameAttribute): List<String> {
    return attribute.parameters.mapIndexed { index, parameter ->
        when (index) {
            /** Last parameter goes always mangled */
            attribute.parameters.size - 1 -> parameter + attribute.postfix
            /** Other parameters remain unchanged */
            else -> parameter
        }
    }
}

internal fun ObjCMethod.isSwiftNameMethod(): Boolean {
    if (returnType == ObjCInstanceType) return false // Skip constructors
    if (attributes.firstOrNull { attr -> attr.startsWith("swift_name") } == null) return false
    return true
}

internal fun SwiftNameAttribute.mangleAttribute(): SwiftNameAttribute {
    return SwiftNameAttribute(methodName, parameters, postfix + "_")
}

/**
 * Determines if methods conflicts exist by comparing `swift_name` attribute.
 * This function isn't for optimization; it avoids handling complex edge case later during mangling.
 */
internal fun List<ObjCExportStub>.hasMethodConflicts(): Boolean {
    val swiftNameAttributes = mutableSetOf<String>()
    forEach { method ->
        if (method is ObjCMethod && method.isSwiftNameMethod()) {
            val swiftNameAttribute = getSwiftNameAttribute(method)
            if (swiftNameAttributes.add(swiftNameAttribute)) return true
        }
    }
    return false
}

internal val String.isReceiver: Boolean
    get() {
        return this == "_:"
    }

internal fun getSwiftNameAttribute(method: ObjCMethod) =
    method.attributes.first { attr -> attr.startsWith("swift_name") }
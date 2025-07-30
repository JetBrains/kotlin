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

internal fun buildMangledSelectors(attribute: ObjCMemberDetails): List<String> {
    val with = if (attribute.isConstructor) "With" else ""
    return if (attribute.parameters.isEmpty())
        listOf(attribute.name + attribute.postfix)
    else if (attribute.parameters.size == 1) {
        val mangledAttribute = (attribute.name + with + attribute.parameters.first()
            .replaceFirstChar { it.uppercaseChar() }).mangleSelector(attribute.postfix)
        if (attribute.parameters.first() == "_:") {
            /**
             * Function with single parameter and name "_" is an extension function.
             * Where receiver is passed as a parameter with name "_".
             * It is a special mangling case.
             *
             * ```kotlin
             * class Foo {
             *   fun Int.bar() = Foo()
             * }
             * ```
             * ```c
             * interface Foo {
             *   - (Foo *)bar:(int_32_t)receiver __attribute__((swift_name("days(_:)")));
             * }
             * ```
             *
             * So when there is another extension function with the same name we mangle:
             * - swift_name parameter
             * - selector
             *
             * ```kotlin
             * class Foo {
             *   fun Int.bar() = Foo()
             *   fun Double.bar() = Foo()
             * }
             * ```
             * ```c
             * interface Foo {
             *   - (Foo *)bar:(int_32_t)receiver __attribute__((swift_name("days(_:)")));
             *   - (Foo *)bar_:(double)receiver __attribute__((swift_name("days(__:)")));
             * }
             * ```
             *
             * We add '_' to swift_attribute parameter and keep selector with one less '_' char:
             * - bar > _
             * - bar_ > __
             * - bar__ > ___
             * etc
             */
            listOf("${mangledAttribute.dropLast(2)}:")
        } else {
            /**
             * If extension function has parameters we have the same amount of `_` for selector and parameter:
             * - bar param > _:param
             * - bar param_ > _:param_
             * - bar param__ > _:param__
             * etc
             */
            listOf(mangledAttribute)
        }

    } else {
        attribute.parameters.mapIndexed { index, param ->
            when (index) {
                0 -> {
                    if (param.isReceiver) {
                        attribute.name + ":"
                    } else {
                        /** First selector is a combination of a method name and first parameter */
                        attribute.name + with + param.replaceFirstChar { it.uppercaseChar() }
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
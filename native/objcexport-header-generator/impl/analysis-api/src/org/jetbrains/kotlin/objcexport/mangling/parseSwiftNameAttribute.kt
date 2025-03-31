package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCParameter
import org.jetbrains.kotlin.objcexport.analysisApiUtils.errorParameterName
import org.jetbrains.kotlin.objcexport.extras.isErrorParameter

private val swiftNameRegex = """^swift_name\("([^"]+)"\)$""".toRegex()
private val methodNameAndParametersRegex = """^([a-zA-Z0-9]+)\((.*)\)$""".toRegex()
private val parametersRegex = Regex("[a-zA-Z0-9_]+:")

internal fun parseSwiftPropertyNameAttribute(attribute: String): ObjCMemberDetails {
    val swiftNameMatch = swiftNameRegex.find(attribute)
    if (swiftNameMatch != null) {
        val propertyName = swiftNameMatch.groupValues[1]
        return ObjCMemberDetails(propertyName, emptyList())
    } else error("Invalid swift_name property attribute: $attribute")
}

internal fun parseSwiftMethodNameAttribute(
    attribute: String,
    isConstructor: Boolean = false,
    parameters: List<ObjCParameter> = emptyList(),
): ObjCMemberDetails {
    val swiftNameMatch = swiftNameRegex.find(attribute)
    if (swiftNameMatch != null) {
        val swiftName = swiftNameMatch.groupValues[1]
        val methodAndParametersMatch = methodNameAndParametersRegex.find(swiftName)
        if (methodAndParametersMatch != null) {
            val methodName = methodAndParametersMatch.groupValues[1]
            val parametersNames = splitParameters(methodAndParametersMatch.groupValues[2])
            val hasErrorParameter = parameters.any { parameter -> parameter.isErrorParameter }
            return ObjCMemberDetails(
                name = methodName,
                parameters = parametersNames + if (hasErrorParameter) listOf("$errorParameterName:") else emptyList(),
                isConstructor = isConstructor,
                hasErrorParameter = hasErrorParameter
            )
        } else error("Invalid name and parameters of swift_name attribute: $attribute")
    } else error("Invalid swift_name method attribute: $attribute")
}

internal data class ObjCMemberDetails(
    val name: String,
    val parameters: List<String>,
    val isConstructor: Boolean = false,
    val postfix: String = "",
    val hasErrorParameter: Boolean = false,
)

private fun splitParameters(parameters: String): List<String> {
    return parametersRegex.findAll(parameters)
        .map { it.value }
        .toList()
}
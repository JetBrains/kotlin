package org.jetbrains.kotlin.objcexport.mangling

import org.jetbrains.kotlin.backend.konan.objcexport.ObjCParameter
import org.jetbrains.kotlin.objcexport.analysisApiUtils.errorParameterName
import org.jetbrains.kotlin.objcexport.extras.isErrorParameter

private val swiftNameRegex = """^swift_name\("([^"]+)"\)$""".toRegex()

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

        val methodName = swiftName.extractMethodName()
        val parameterNames = parseSwiftNameParameters(swiftName)

        if (!methodName.isNullOrEmpty()) {
            val hasErrorParameter = parameters.any { parameter -> parameter.isErrorParameter }
            return ObjCMemberDetails(
                name = methodName,
                parameters = parameterNames + if (hasErrorParameter) listOf("$errorParameterName:") else emptyList(),
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

/**
 * foo(a:b:) -> [a:, b:]
 */
internal fun parseSwiftNameParameters(swiftNameValue: String): List<String> {
    val functionPattern = Regex("""\w+\((.*?)\)""")
    val match = functionPattern.matchEntire(swiftNameValue.trim())

    return when {
        match != null -> {
            val params = match.groupValues[1]
            if (params.isBlank()) emptyList()
            else {
                params.split(':')
                    .filter { it.isNotEmpty() }
                    .map { param -> "$param:" }
            }
        }
        else -> emptyList()
    }
}

/**
 * `foo(bar) -> foo
 */
internal fun String?.extractMethodName(): String? {
    return this?.substringBefore('(')
}
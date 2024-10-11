package org.jetbrains.kotlin.objcexport.mangling

private val swiftNameRegex = """^swift_name\("([^"]+)"\)$""".toRegex()
private val methodNameAndParametersRegex = """^([a-zA-Z0-9]+)\((.*)\)$""".toRegex()
private val parametersRegex = Regex("[a-zA-Z0-9_]+:")

internal fun parseSwiftNameAttribute(attribute: String, isConstructor: Boolean = false): ObjCMemberDetails {
    val swiftNameMatch = swiftNameRegex.find(attribute)
    if (swiftNameMatch != null) {
        val swiftName = swiftNameMatch.groupValues[1]
        val methodAndParametersMatch = methodNameAndParametersRegex.find(swiftName)
        if (methodAndParametersMatch != null) {
            val methodName = methodAndParametersMatch.groupValues[1]
            val parameters = methodAndParametersMatch.groupValues[2]
            return ObjCMemberDetails(methodName, splitParameters(parameters), isConstructor)
        } else error("Invalid swift_name attribute: $attribute")
    } else error("Invalid swift_name attribute: $attribute")
}

internal data class ObjCMemberDetails(
    val name: String,
    val parameters: List<String>,
    val isConstructor: Boolean = false,
    val postfix: String = "",
)

private fun splitParameters(parameters: String): List<String> {
    return parametersRegex.findAll(parameters)
        .map { it.value }
        .toList()
}
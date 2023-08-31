/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.util

// TODO [beresnev] copy-paste

internal fun propertyNamesBySetMethodName(methodName: String): List<String> =
    listOfNotNull(propertyNameBySetMethodName(methodName, false), propertyNameBySetMethodName(methodName, true))

internal fun propertyNameByGetMethodName(methodName: String): String? =
    propertyNameFromAccessorMethodName(methodName, "get") ?: propertyNameFromAccessorMethodName(methodName, "is", removePrefix = false)

private fun propertyNameBySetMethodName(methodName: String, withIsPrefix: Boolean): String? =
    propertyNameFromAccessorMethodName(methodName, "set", addPrefix = if (withIsPrefix) "is" else null)

private fun propertyNameFromAccessorMethodName(
    methodName: String,
    prefix: String,
    removePrefix: Boolean = true,
    addPrefix: String? = null
): String? {
    val isSpecial = methodName.startsWith("<") // see special in org.jetbrains.kotlin.Name
    if (isSpecial) return null
    if (!methodName.startsWith(prefix)) return null
    if (methodName.length == prefix.length) return null
    if (methodName[prefix.length] in 'a'..'z') return null

    if (addPrefix != null) {
        assert(removePrefix)
        return addPrefix + methodName.removePrefix(prefix)
    }

    if (!removePrefix) return methodName
    val name = methodName.removePrefix(prefix).decapitalizeSmartForCompiler(asciiOnly = true)
    if (!isValidIdentifier(name)) return null
    return name
}

/**
 * "FooBar" -> "fooBar"
 * "FOOBar" -> "fooBar"
 * "FOO" -> "foo"
 * "FOO_BAR" -> "foO_BAR"
 */
private fun String.decapitalizeSmartForCompiler(asciiOnly: Boolean = false): String {
    if (isEmpty() || !isUpperCaseCharAt(0, asciiOnly)) return this

    if (length == 1 || !isUpperCaseCharAt(1, asciiOnly)) {
        return if (asciiOnly) decapitalizeAsciiOnly() else replaceFirstChar(Char::lowercaseChar)
    }

    val secondWordStart = (indices.firstOrNull { !isUpperCaseCharAt(it, asciiOnly) } ?: return toLowerCase(this, asciiOnly)) - 1

    return toLowerCase(substring(0, secondWordStart), asciiOnly) + substring(secondWordStart)
}

private fun String.isUpperCaseCharAt(index: Int, asciiOnly: Boolean): Boolean {
    val c = this[index]
    return if (asciiOnly) c in 'A'..'Z' else c.isUpperCase()
}

private fun toLowerCase(string: String, asciiOnly: Boolean): String {
    return if (asciiOnly) string.toLowerCaseAsciiOnly() else string.lowercase()
}

private fun toUpperCase(string: String, asciiOnly: Boolean): String {
    return if (asciiOnly) string.toUpperCaseAsciiOnly() else string.uppercase()
}

private fun String.decapitalizeAsciiOnly(): String {
    if (isEmpty()) return this
    val c = this[0]
    return if (c in 'A'..'Z')
        c.lowercaseChar() + substring(1)
    else
        this
}

private fun String.toLowerCaseAsciiOnly(): String {
    val builder = StringBuilder(length)
    for (c in this) {
        builder.append(if (c in 'A'..'Z') c.lowercaseChar() else c)
    }
    return builder.toString()
}

private fun String.toUpperCaseAsciiOnly(): String {
    val builder = StringBuilder(length)
    for (c in this) {
        builder.append(if (c in 'a'..'z') c.uppercaseChar() else c)
    }
    return builder.toString()
}

private fun isValidIdentifier(name: String): Boolean {
    if (name.isEmpty() || name.startsWith("<")) return false
    for (element in name) {
        if (element == '.' || element == '/' || element == '\\') {
            return false
        }
    }
    return true
}

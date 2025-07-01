/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.config.nativeBinaryOptions

import org.jetbrains.kotlin.config.CompilerConfigurationKey

class BinaryOptionWithValue<T : Any>(val compilerConfigurationKey: CompilerConfigurationKey<T>, val value: T, val rawStringValue: String) {
    fun asCompilerCliArgument(): String =
        "-Xbinary=${compilerConfigurationKey}=$rawStringValue"
}

fun parseBinaryOptions(
    argumentValue: Array<String>?,
    reportWarning: (String) -> Unit,
    reportError: (String) -> Unit,
): List<BinaryOptionWithValue<*>> {
    val keyValuePairs = parseKeyValuePairs(argumentValue, reportError) ?: return emptyList()

    return keyValuePairs.mapNotNull { (key, value) ->
        val option = BinaryOptions.getByName(key)
        if (option == null) {
            reportWarning("Unknown binary option '$key'")
            null
        } else {
            parseBinaryOption(option, value, reportWarning)
        }
    }
}

private fun <T : Any> parseBinaryOption(
    option: BinaryOption<T>,
    valueName: String,
    reportWarning: (String) -> Unit
): BinaryOptionWithValue<T>? {
    val value = option.valueParser.parse(valueName)
    return if (value == null) {
        reportWarning("Unknown value '$valueName' of binary option '${option.name}'. " +
                "Possible values are: ${option.valueParser.validValuesHint}")
        null
    } else {
        BinaryOptionWithValue(option.compilerConfigurationKey, value, valueName)
    }
}

private fun parseKeyValuePairs(
    argumentValue: Array<String>?,
    reportError: (String) -> Unit
): Map<String, String>? = argumentValue?.mapNotNull {
    val keyValueSeparatorIndex = it.indexOf('=')
    if (keyValueSeparatorIndex > 0) {
        it.substringBefore('=') to it.substringAfter('=')
    } else {
        reportError("incorrect property format: expected '<key>=<value>', got '$it'")
        null
    }
}?.toMap()

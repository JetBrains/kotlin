/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.config

import java.io.File

class LombokConfig(private val config: Map<String, List<String>>) {

    fun getString(key: String): String? = config[key]?.firstOrNull()

    fun getBoolean(key: String): Boolean? = getString(key)?.toBoolean()

    fun getMultiString(key: String): List<String>? = config[key]

    companion object {

        val Empty = LombokConfig(emptyMap())

        fun parse(path: File): LombokConfig = ConfigParser.parse(path)
    }

}

/**
 * Simplified Lombok config parser.
 * Ignores everything it doesn't understand
 */
object ConfigParser {

    //regex is from lombok source code
    private val LINE = "(?:clear\\s+([^=]+))|(?:(\\S*?)\\s*([-+]?=)\\s*(.*?))".toRegex()

    fun parse(path: File): LombokConfig {
        val builder = ConfigBuilder()
        path.forEachLine { parseLine(it, builder) }
        return builder.build()
    }

    private fun parseLine(line: String, builder: ConfigBuilder) {
        LINE.matchEntire(line)?.let { matchResult ->
            if (matchResult.groups[1] == null) {
                val keyName = matchResult.groupValues[2]
                val operator = matchResult.groupValues[3]
                val stringValue = matchResult.groupValues[4]
                when (operator) {
                    "=" -> builder.setValue(keyName, stringValue)
                    "+=" -> builder.plusValue(keyName, stringValue)
                    "-=" -> builder.minusValue(keyName, stringValue)
                    else -> {
                        //do nothing
                    }
                }
            } else {
                //clear
                val keyName = matchResult.groupValues[1]
                builder.clearValue(keyName)
            }
        }
    }

}

class ConfigBuilder {
    private val state: MutableMap<String, List<String>> = mutableMapOf()

    fun setValue(name: String, value: String) {
        state[name] = listOf(value)
    }

    fun clearValue(name: String) {
        state.remove(name)
    }

    fun plusValue(name: String, value: String) {
        state.merge(name, listOf(value)) { a, b -> a + b }
    }

    fun minusValue(name: String, value: String) {
        state.merge(name, listOf(value)) { a, b -> a - b }
    }

    fun build(): LombokConfig = LombokConfig(state)
}



/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cli

internal class BooleanOptionType(
    alias: String,
    description: String,
    mandatory: Boolean
) : OptionType<Boolean>(alias, description, mandatory) {
    override fun parse(rawValue: String, onError: (reason: String) -> Nothing): Option<Boolean> {
        return Option(this, parseBoolean(rawValue, onError))
    }

    companion object {
        internal val TRUE_TOKENS = setOf("1", "on", "yes", "true")
        internal val FALSE_TOKENS = setOf("0", "off", "no", "false")
    }
}

internal fun parseBoolean(rawValue: String, onError: (reason: String) -> Nothing): Boolean {
    return rawValue.lowercase().let {
        when (it) {
            in BooleanOptionType.TRUE_TOKENS -> true
            in BooleanOptionType.FALSE_TOKENS -> false
            else -> onError("Invalid boolean value: $it")
        }
    }
}

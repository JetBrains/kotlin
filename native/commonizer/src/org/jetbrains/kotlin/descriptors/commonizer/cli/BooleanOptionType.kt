/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cli

internal class BooleanOptionType(
    alias: String,
    description: String,
    mandatory: Boolean
) : OptionType<Boolean>(alias, description, mandatory) {
    private val trueTokens = setOf("1", "on", "yes", "true")
    private val falseTokens = setOf("0", "off", "no", "false")

    override fun parse(rawValue: String, onError: (reason: String) -> Nothing): Option<Boolean> {
        val value = rawValue.toLowerCase().let {
            when (it) {
                in trueTokens -> true
                in falseTokens -> false
                else -> onError("Invalid boolean value: $it")
            }
        }

        return Option(this, value)
    }
}

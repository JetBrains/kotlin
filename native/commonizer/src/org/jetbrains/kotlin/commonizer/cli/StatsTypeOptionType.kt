/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cli

import org.jetbrains.kotlin.commonizer.stats.StatsType

internal object StatsTypeOptionType : OptionType<StatsType>(STATS_TYPE_ALIAS, DESCRIPTION, mandatory = false) {
    override fun parse(rawValue: String, onError: (reason: String) -> Nothing): Option<StatsType> {
        val value = StatsType.values().firstOrNull { it.name.equals(rawValue, ignoreCase = true) }
            ?: onError("Invalid stats type: $rawValue")
        return Option(this, value)
    }
}

private val DESCRIPTION = buildString {
    StatsType.values().joinTo(this) {
        val item = "\"${it.name.lowercase()}\""
        if (it == StatsType.NONE) "$item (default)" else item
    }
    append(";\nlog commonization stats")
}

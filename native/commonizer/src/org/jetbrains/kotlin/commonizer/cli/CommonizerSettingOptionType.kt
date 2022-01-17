/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cli

sealed class CommonizerSettingOptionType<T : Any>(
    alias: OptionAlias,
    description: String,
    val defaultValue: T
) : OptionType<T>(
    alias,
    description,
    mandatory = false,
)

internal val AdditionalCommonizerSettings: List<CommonizerSettingOptionType<*>> = listOf(
    PlatformIntegers,
)

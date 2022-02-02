/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cli

import org.jetbrains.kotlin.commonizer.CommonizerSettings

internal sealed class CommonizerSettingOptionType<T : Any>(
    val commonizerSettingKey: CommonizerSettings.Key<T>,
    description: String,
) : OptionType<T>(
    commonizerSettingKey.alias,
    description,
    mandatory = false,
)

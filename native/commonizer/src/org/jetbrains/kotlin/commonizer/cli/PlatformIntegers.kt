/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cli

internal object PlatformIntegers : CommonizerSettingOptionType<Boolean>(
    PlatformIntegersAlias,
    "Boolean (default false)\nEnable support of platform bit width integer commonization",
    defaultValue = false,
) {
    override fun parse(rawValue: String, onError: (reason: String) -> Nothing): Option<Boolean> =
        parseBoolean(rawValue, onError)
}

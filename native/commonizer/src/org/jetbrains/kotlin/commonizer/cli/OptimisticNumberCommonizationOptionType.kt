/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.cli

import org.jetbrains.kotlin.commonizer.OptimisticNumberCommonizationEnabledKey

internal object OptimisticNumberCommonizationOptionType : CommonizerSettingOptionType<Boolean>(
    OPTIMISTIC_NUMBER_COMMONIZATION_ENABLED_OPTION_ALIAS,
    "Boolean (default true)\nEnable commonization of integer types with different bit width to the most narrow among them",
    OptimisticNumberCommonizationEnabledKey,
) {
    override fun parse(rawValue: String, onError: (reason: String) -> Nothing): Option<Boolean> =
        Option(this, parseBoolean(rawValue, onError))
}

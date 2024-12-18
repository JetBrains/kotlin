/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

const val PRESETS_DEPRECATION_MESSAGE_SUFFIX = "API is deprecated and will be removed in 2.2.0 releases."
const val PRESETS_DEPRECATION_SOLUTION = "Migrate to built-in target DSL - for example, use 'jvm()' instead of 'fromPreset(Jvm)'"
const val PRESETS_DEPRECATION_URL_PREFIX = "Learn how to configure targets at:"
const val PRESETS_DEPRECATION_URL = "https://kotl.in/target-configuration"

const val PRESETS_API_IS_DEPRECATED_MESSAGE = "The presets $PRESETS_DEPRECATION_MESSAGE_SUFFIX"

@RequiresOptIn(
    message = PRESETS_API_IS_DEPRECATED_MESSAGE,
    level = RequiresOptIn.Level.ERROR
)
annotation class DeprecatedTargetPresetApi
/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Named
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi

const val PRESETS_DEPRECATION_MESSAGE_SUFFIX =
    "API is deprecated and will be removed in future releases. Learn how to configure targets at: https://kotl.in/target-configuration"
const val PRESETS_API_IS_DEPRECATED_MESSAGE = "The presets $PRESETS_DEPRECATION_MESSAGE_SUFFIX"

@RequiresOptIn(
    message = PRESETS_API_IS_DEPRECATED_MESSAGE,
    level = RequiresOptIn.Level.WARNING
)
annotation class TargetPresetsDeprecation

@TargetPresetsDeprecation
interface KotlinTargetPreset<T: KotlinTarget> : Named {
    @InternalKotlinGradlePluginApi
    fun createTargetInternal(name: String): T

    @OptIn(InternalKotlinGradlePluginApi::class)
    @Deprecated(
        PRESETS_API_IS_DEPRECATED_MESSAGE,
        level = DeprecationLevel.WARNING,
    )
    fun createTarget(name: String): T {
        val target = createTargetInternal(name)
        target.warnAboutCreationOfTargetFromPreset()
        return target
    }
}
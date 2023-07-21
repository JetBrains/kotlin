/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Named
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi

const val apiIsDeprecatedMessage =
    "API is deprecated and will be phased out soon. Learn how to configure targets at: https://kotl.in/target-configuration"
const val presetsApiIsDeprecatedMessage = "Presets $apiIsDeprecatedMessage"

@RequiresOptIn(
    message = presetsApiIsDeprecatedMessage,
    level = RequiresOptIn.Level.WARNING
)
annotation class TargetPresetsDeprecation

@TargetPresetsDeprecation
interface KotlinTargetPreset<T: KotlinTarget> : Named {
    @InternalKotlinGradlePluginApi
    fun createTargetInternal(name: String): T

    @OptIn(InternalKotlinGradlePluginApi::class)
    @Deprecated(
        presetsApiIsDeprecatedMessage,
        level = DeprecationLevel.WARNING,
    )
    fun createTarget(name: String): T {
        val target = createTargetInternal(name)
        target.warnAboutCreationOfTargetFromPreset()
        return target
    }
}
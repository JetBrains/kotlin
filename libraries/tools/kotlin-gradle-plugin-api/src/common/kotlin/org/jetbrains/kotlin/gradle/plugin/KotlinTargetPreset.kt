/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin

import org.gradle.api.Named
import org.jetbrains.kotlin.gradle.PRESETS_DEPRECATION_MESSAGE_SUFFIX
import org.jetbrains.kotlin.gradle.DeprecatedTargetPresetApi
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi

@OptIn(InternalKotlinGradlePluginApi::class)
@DeprecatedTargetPresetApi
interface KotlinTargetPreset<T: KotlinTarget> : Named {
    @Deprecated(
        "The KotlinTargetPreset.createTarget() $PRESETS_DEPRECATION_MESSAGE_SUFFIX",
        level = DeprecationLevel.WARNING,
    )
    fun createTarget(name: String): T
}
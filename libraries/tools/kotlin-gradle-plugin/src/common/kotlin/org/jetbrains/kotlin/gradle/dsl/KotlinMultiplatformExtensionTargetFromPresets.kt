/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetPreset
import org.jetbrains.kotlin.gradle.targets.android.internal.internal

internal fun <T : KotlinTarget> KotlinMultiplatformExtension.targetFromPresetInternal(
    preset: KotlinTargetPreset<T>,
    name: String = preset.name,
    configure: T.() -> Unit = { },
): T = configureOrCreate(name, preset.internal, configure)

internal fun <T : KotlinTarget> KotlinMultiplatformExtension.targetFromPresetInternal(
    preset: KotlinTargetPreset<T>,
    name: String,
    configure: Action<T>,
) = targetFromPresetInternal(preset, name) { configure.execute(this) }

internal fun <T : KotlinTarget> KotlinMultiplatformExtension.targetFromPresetInternal(preset: KotlinTargetPreset<T>) =
    targetFromPresetInternal(preset, preset.name) { }

internal fun <T : KotlinTarget> KotlinMultiplatformExtension.targetFromPresetInternal(preset: KotlinTargetPreset<T>, name: String) =
    targetFromPresetInternal(preset, name) { }

internal fun <T : KotlinTarget> KotlinMultiplatformExtension.targetFromPresetInternal(preset: KotlinTargetPreset<T>, configure: Action<T>) =
    targetFromPresetInternal(preset, preset.name, configure)
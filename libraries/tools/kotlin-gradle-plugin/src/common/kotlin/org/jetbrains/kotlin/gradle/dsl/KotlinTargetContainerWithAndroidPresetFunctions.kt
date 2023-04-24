/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetsContainerWithPresets
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTargetPreset

interface KotlinTargetContainerWithAndroidPresetFunctions : KotlinTargetsContainerWithPresets {

    fun android(
        name: String = "android",
        configure: KotlinAndroidTarget.() -> Unit = { }
    ): KotlinAndroidTarget =
        configureOrCreate(
            name,
            presets.getByName("android") as KotlinAndroidTargetPreset,
            configure
        )

    fun android() = android("android") { }
    fun android(name: String) = android(name) { }
    fun android(name: String, configure: Action<KotlinAndroidTarget>) = android(name) { configure.execute(this) }
    fun android(configure: Action<KotlinAndroidTarget>) = android { configure.execute(this) }

}
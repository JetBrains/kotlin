/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetsContainerWithPresets
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTargetPreset

private const val ANDROID_TARGET_MIGRATION_MESSAGE = "Please use androidTarget() instead. Learn more here: https://kotl.in/android-target-dsl"

interface KotlinTargetContainerWithAndroidPresetFunctions : KotlinTargetsContainerWithPresets {

    fun androidTarget(
        name: String = "android",
        configure: KotlinAndroidTarget.() -> Unit = { }
    ): KotlinAndroidTarget =
        configureOrCreate(
            name,
            presets.getByName("android") as KotlinAndroidTargetPreset,
            configure
        )

    fun androidTarget() = androidTarget("android") { }
    fun androidTarget(name: String) = androidTarget(name) { }
    fun androidTarget(name: String, configure: Action<KotlinAndroidTarget>) = androidTarget(name) { configure.execute(this) }
    fun androidTarget(configure: Action<KotlinAndroidTarget>) = androidTarget { configure.execute(this) }

    @Deprecated(ANDROID_TARGET_MIGRATION_MESSAGE)
    fun android(
        name: String = "android",
        configure: KotlinAndroidTarget.() -> Unit = { }
    ): KotlinAndroidTarget =
        configureOrCreateAndroidTargetAndReportDeprecation(name, configure)

    @Suppress("DEPRECATION")
    @Deprecated(ANDROID_TARGET_MIGRATION_MESSAGE, replaceWith = ReplaceWith("androidTarget()"))
    fun android() = android("android") { }

    @Suppress("DEPRECATION")
    @Deprecated(ANDROID_TARGET_MIGRATION_MESSAGE, replaceWith = ReplaceWith("androidTarget(name)"))
    fun android(name: String) = android(name) { }

    @Suppress("DEPRECATION")
    @Deprecated(ANDROID_TARGET_MIGRATION_MESSAGE)
    fun android(name: String, configure: Action<KotlinAndroidTarget>) = android(name) { configure.execute(this) }

    @Suppress("DEPRECATION")
    @Deprecated(ANDROID_TARGET_MIGRATION_MESSAGE)
    fun android(configure: Action<KotlinAndroidTarget>) = android { configure.execute(this) }

}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.gradle.kotlin.dsl

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

/**
 * This file contains special temporary migration function for android target in the MPP project.
 *
 * In the future special multiplatform android plugin will be introduced.
 * This new plugin will register a `android` extension on [KotlinMultiplatformExtension] class.
 * It means, that in the package `org.gradle.kotlin.dsl` will be generated the following two declarations:
 *  val [KotlinMultiplatformExtension].`android`: KotlinMultiplatformAndroidTarget
 *  fun [KotlinMultiplatformExtension].`android`(configure: Action<KotlinMultiplatformAndroidTarget>): Unit
 * And these new declaration when present should be preferred in build.gradle.kts.
 *
 * But before this new plugin is introduced or if user are using just `com.android.application` or `com.android.library`
 * old way of configuring the android target should be used.
 *
 * Note that these migration function won't help with usages in `build.gradle` files.
 * In case of `build.gradle` [KotlinTargetContainerWithPresetFunctions.androidTarget] should be used
 *
 * This magic works, because:
 *  1) [KotlinMultiplatformExtension] is more specific then [KotlinTargetContainerWithPresetFunctions]
 *  2) function call wins over property + invoke() on this property.
 *  3) inside build.gradle.kts everything from `org.gradle.kotlin.dsl` package is imported by default
 */

fun KotlinTargetContainerWithPresetFunctions.android(
    name: String = "android",
    configure: KotlinAndroidTarget.() -> Unit = { }
): KotlinAndroidTarget = androidTarget(name, configure)

fun KotlinTargetContainerWithPresetFunctions.android() = androidTarget()
fun KotlinTargetContainerWithPresetFunctions.android(name: String) = androidTarget(name)
fun KotlinTargetContainerWithPresetFunctions.android(name: String, configure: Action<KotlinAndroidTarget>) = androidTarget(name, configure)

val KotlinTargetContainerWithPresetFunctions.android: (configure: Action<KotlinAndroidTarget>) -> KotlinAndroidTarget
    get() = { configure -> this.androidTarget { configure.execute(this) } }

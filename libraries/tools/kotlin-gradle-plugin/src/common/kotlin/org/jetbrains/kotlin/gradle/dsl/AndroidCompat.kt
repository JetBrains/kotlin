/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.gradle.kotlin.dsl

import org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithPresetFunctions
import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget

fun KotlinTargetContainerWithPresetFunctions.android() = androidTarget("android") { }

fun KotlinTargetContainerWithPresetFunctions.android(name: String) = androidTarget(name) { }

fun KotlinTargetContainerWithPresetFunctions.android(name: String, configure: Action<KotlinAndroidTarget>) = androidTarget(name) { configure.execute(this) }

val KotlinTargetContainerWithPresetFunctions.android: (configure: Action<in KotlinAndroidTarget>) -> KotlinAndroidTarget
    get() = { configure -> this.androidTarget { configure.execute(this) } }

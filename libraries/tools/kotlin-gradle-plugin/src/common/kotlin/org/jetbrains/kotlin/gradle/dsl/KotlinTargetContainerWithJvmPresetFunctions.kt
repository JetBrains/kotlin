/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetsContainerWithPresets
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmTargetPreset
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

interface KotlinTargetContainerWithJvmPresetFunctions : KotlinTargetsContainerWithPresets {

    fun jvm(
        name: String = "jvm",
        configure: KotlinJvmTarget.() -> Unit = { }
    ): KotlinJvmTarget =
        configureOrCreate(
            name,
            presets.getByName("jvm") as KotlinJvmTargetPreset,
            configure
        )

    fun jvm() = jvm("jvm") { }
    fun jvm(name: String) = jvm(name) { }
    fun jvm(name: String, configure: Action<KotlinJvmTarget>) = jvm(name) { configure.execute(this) }
    fun jvm(configure: Action<KotlinJvmTarget>) = jvm { configure.execute(this) }

}
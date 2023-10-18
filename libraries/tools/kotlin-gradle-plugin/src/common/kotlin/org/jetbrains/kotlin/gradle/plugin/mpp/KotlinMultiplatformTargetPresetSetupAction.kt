/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTargetPreset
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinWasmTargetPreset
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget.*
import org.jetbrains.kotlin.konan.target.presetName

internal val KotlinMultiplatformTargetPresetAction = KotlinProjectSetupAction {
    @Suppress("DEPRECATION")
    with(project.multiplatformExtension.presets) {
        add(KotlinJvmTargetPreset(project))
        add(KotlinJsIrTargetPreset(project))
        add(KotlinWasmTargetPreset(project, KotlinWasmTargetType.JS))
        add(KotlinWasmTargetPreset(project, KotlinWasmTargetType.WASI))
        add(project.objects.newInstance(KotlinAndroidTargetPreset::class.java, project))
        add(KotlinJvmWithJavaTargetPreset(project))

        // Note: modifying these sets should also be reflected in the DSL code generator, see 'presetEntries.kt'
        val nativeTargetsWithHostTests = setOf(LINUX_X64, MACOS_X64, MACOS_ARM64, MINGW_X64)
        val nativeTargetsWithSimulatorTests = setOf(
            IOS_X64, IOS_SIMULATOR_ARM64,
            WATCHOS_X86, WATCHOS_X64, WATCHOS_SIMULATOR_ARM64,
            TVOS_X64, TVOS_SIMULATOR_ARM64
        )

        HostManager().targets.forEach { (_, konanTarget) ->
            val targetToAdd = when (konanTarget) {
                in nativeTargetsWithHostTests ->
                    KotlinNativeTargetWithHostTestsPreset(konanTarget.presetName, project, konanTarget)

                in nativeTargetsWithSimulatorTests ->
                    KotlinNativeTargetWithSimulatorTestsPreset(konanTarget.presetName, project, konanTarget)

                else -> KotlinNativeTargetPreset(konanTarget.presetName, project, konanTarget)
            }

            add(targetToAdd)
        }
    }
}

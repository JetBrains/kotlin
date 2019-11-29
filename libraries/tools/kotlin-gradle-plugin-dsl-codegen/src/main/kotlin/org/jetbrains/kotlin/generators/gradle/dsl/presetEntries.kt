/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.gradle.dsl

import org.jetbrains.kotlin.generators.gradle.dsl.NativeFQNames.Presets
import org.jetbrains.kotlin.generators.gradle.dsl.NativeFQNames.Targets
import org.jetbrains.kotlin.generators.gradle.dsl.NativeFQNames.TestRuns
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName

internal class KotlinPresetEntry(
    val presetName: String,
    val presetType: TypeName,
    val targetType: TypeName
)

internal fun KotlinPresetEntry.typeNames(): Set<TypeName> = setOf(presetType, targetType)

internal const val MPP_PACKAGE = "org.jetbrains.kotlin.gradle.plugin.mpp"

internal object NativeFQNames {
    const val nativePackage = "org.jetbrains.kotlin.gradle.targets.native"

    object Targets {
        const val base = "$MPP_PACKAGE.KotlinNativeTarget"
        const val withTests = "$MPP_PACKAGE.KotlinNativeTargetWithTests"
    }

    object Presets {
        const val simple = "$MPP_PACKAGE.KotlinNativeTargetPreset"
        const val withHostTests = "$MPP_PACKAGE.KotlinNativeTargetWithHostTestsPreset"
        const val withSimulatorTests = "$MPP_PACKAGE.KotlinNativeTargetWithSimulatorTestsPreset"
    }

    object TestRuns {
        const val host = "$nativePackage.KotlinNativeHostTestRun"
        const val simulator = "$nativePackage.KotlinNativeSimulatorTestRun"
    }
}

internal val jvmPresetEntry = KotlinPresetEntry(
    "jvm",
    typeName("$MPP_PACKAGE.KotlinJvmTargetPreset"),
    typeName("org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget")
)

internal val jsPresetEntry = KotlinPresetEntry(
    "js",
    typeName("$MPP_PACKAGE.KotlinJsTargetPreset"),
    typeName("org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget")
)

internal val androidPresetEntry = KotlinPresetEntry(
    "android",
    typeName("$MPP_PACKAGE.KotlinAndroidTargetPreset"),
    typeName("$MPP_PACKAGE.KotlinAndroidTarget")
)

// Note: modifying these sets should also be reflected in the MPP plugin code, see 'setupDefaultPresets'
private val nativeTargetsWithHostTests = setOf(KonanTarget.LINUX_X64, KonanTarget.MACOS_X64, KonanTarget.MINGW_X64)
private val nativeTargetsWithSimulatorTests = setOf(KonanTarget.IOS_X64, KonanTarget.WATCHOS_X86, KonanTarget.TVOS_X64)
private val disabledNativeTargets = setOf(KonanTarget.WATCHOS_X64)

internal val nativePresetEntries = HostManager().targets
    .filter { (_, target) -> target !in disabledNativeTargets }
    .map { (_, target) ->

        val (presetType, targetType) = when (target) {
            in nativeTargetsWithHostTests ->
                typeName(Presets.withHostTests) to typeName(Targets.withTests, TestRuns.host)
            in nativeTargetsWithSimulatorTests ->
                typeName(Presets.withSimulatorTests) to typeName(Targets.withTests, TestRuns.simulator)
            else ->
                typeName(Presets.simple) to typeName(Targets.base)
        }

        KotlinPresetEntry(target.presetName, presetType, targetType)
    }

internal val allPresetEntries = listOf(
    jvmPresetEntry,
    jsPresetEntry,
    androidPresetEntry
) + nativePresetEntries
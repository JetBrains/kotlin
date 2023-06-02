/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.gradle.dsl

import org.jetbrains.kotlin.generators.gradle.dsl.NativeFQNames.Presets
import org.jetbrains.kotlin.generators.gradle.dsl.NativeFQNames.Targets
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName

internal class KotlinPresetEntry(
    val presetName: String,
    val presetType: TypeName,
    val targetType: TypeName,
    val deprecation: Deprecation? = null,
    val entityName: String = presetName,
    // Adds `.also { $alsoBlockAfterConfiguration }` after configureOrCreate(...)
    val alsoBlockAfterConfiguration: String? = null,
    // Extra declarations will be inserted before functions are generated
    val extraTopLevelDeclarations: List<String> = emptyList(),
) {
    class Deprecation(
        val message: String,
        val level: DeprecationLevel,
        val replaceWithOtherPreset: String? = null // when set, it will generate ReplaceWith with related argument names
    )
}

internal fun KotlinPresetEntry.typeNames(): Set<TypeName> = setOf(presetType, targetType)

internal const val MPP_PACKAGE = "org.jetbrains.kotlin.gradle.plugin.mpp"

internal object NativeFQNames {
    object Targets {
        const val base = "$MPP_PACKAGE.KotlinNativeTarget"
        const val withHostTests = "$MPP_PACKAGE.KotlinNativeTargetWithHostTests"
        const val withSimulatorTests = "$MPP_PACKAGE.KotlinNativeTargetWithSimulatorTests"
    }

    object Presets {
        const val simple = "$MPP_PACKAGE.KotlinNativeTargetPreset"
        const val withHostTests = "$MPP_PACKAGE.KotlinNativeTargetWithHostTestsPreset"
        const val withSimulatorTests = "$MPP_PACKAGE.KotlinNativeTargetWithSimulatorTestsPreset"
    }
}

internal val jvmPresetEntry = KotlinPresetEntry(
    "jvm",
    typeName("$MPP_PACKAGE.KotlinJvmTargetPreset"),
    typeName("org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget")
)

internal val androidTargetPresetEntry = KotlinPresetEntry(
    "androidTarget",
    typeName("$MPP_PACKAGE.KotlinAndroidTargetPreset"),
    typeName("$MPP_PACKAGE.KotlinAndroidTarget"),
    entityName = "android",
)

internal val androidPresetEntry = KotlinPresetEntry(
    "android",
    typeName("$MPP_PACKAGE.KotlinAndroidTargetPreset"),
    typeName("$MPP_PACKAGE.KotlinAndroidTarget"),
    deprecation = KotlinPresetEntry.Deprecation(
        message = "ANDROID_TARGET_MIGRATION_MESSAGE",
        level = DeprecationLevel.WARNING,
        replaceWithOtherPreset = "androidTarget"
    ),
    extraTopLevelDeclarations = listOf(
        "private const val ANDROID_TARGET_MIGRATION_MESSAGE" +
                " = \"Please use androidTarget() instead. Learn more here: https://kotl.in/android-target-dsl\""
    ),
    alsoBlockAfterConfiguration = """
            it.project.logger.warn(
                ""${'"'}
                    w: Please use `androidTarget` function instead of `android` to configure android target inside `kotlin { }` block.
                    See the details here: https://kotl.in/android-target-dsl
                ""${'"'}.trimIndent()
            )
    """.trimIndent()
)

// Note: modifying these sets should also be reflected in the MPP plugin code, see 'setupDefaultPresets'
private val nativeTargetsWithHostTests =
    setOf(KonanTarget.LINUX_X64, KonanTarget.MACOS_X64, KonanTarget.MACOS_ARM64, KonanTarget.MINGW_X64)
private val nativeTargetsWithSimulatorTests =
    setOf(
        KonanTarget.IOS_X64,
        KonanTarget.IOS_SIMULATOR_ARM64,

        KonanTarget.WATCHOS_X86,
        KonanTarget.WATCHOS_X64,
        KonanTarget.WATCHOS_SIMULATOR_ARM64,

        KonanTarget.TVOS_X64,
        KonanTarget.TVOS_SIMULATOR_ARM64
    )

internal val nativePresetEntries = HostManager().targets
    .map { (_, target) ->

        val (presetType, targetType) = when (target) {
            in nativeTargetsWithHostTests ->
                Presets.withHostTests to Targets.withHostTests
            in nativeTargetsWithSimulatorTests ->
                Presets.withSimulatorTests to Targets.withSimulatorTests
            else ->
                Presets.simple to Targets.base
        }

        val deprecation = KotlinPresetEntry.Deprecation(
            message = "DEPRECATED_TARGET_MESSAGE",
            level = DeprecationLevel.ERROR
        ).takeIf { target in KonanTarget.deprecatedTargets }
        KotlinPresetEntry(target.presetName, typeName(presetType), typeName(targetType), deprecation)
    }

internal val allPresetEntries = listOf(
    jvmPresetEntry,
    androidTargetPresetEntry,
    androidPresetEntry
) + nativePresetEntries

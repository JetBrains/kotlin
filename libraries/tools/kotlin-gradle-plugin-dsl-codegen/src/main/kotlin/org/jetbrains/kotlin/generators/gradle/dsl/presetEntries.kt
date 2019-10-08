/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.gradle.dsl

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

internal const val KOTLIN_NATIVE_TARGET_PRESET_CLASS_FQNAME = "$MPP_PACKAGE.KotlinNativeTargetPreset"
internal const val KOTLIN_NATIVE_TARGET_CLASS_FQNAME = "$MPP_PACKAGE.KotlinNativeTarget"

internal const val KOTLIN_NATIVE_TARGET_WITH_TESTS_PRESET_CLASS_FQNAME = "$MPP_PACKAGE.KotlinNativeTargetWithTestsPreset"
internal const val KOTLIN_NATIVE_TARGET_WITH_TESTS_CLASS_FQNAME = "$MPP_PACKAGE.KotlinNativeTargetWithTests"

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

// Note: modifying this set should also be reflected in the MPP plugin code, see 'setupDefaultPresets'
private val testableNativeTargets = setOf(KonanTarget.LINUX_X64, KonanTarget.MACOS_X64, KonanTarget.MINGW_X64)

internal val nativePresetEntries = HostManager().targets.map { (_, target) ->
    KotlinPresetEntry(
        target.presetName,
        typeName(
            if (target in testableNativeTargets)
                KOTLIN_NATIVE_TARGET_WITH_TESTS_PRESET_CLASS_FQNAME
            else
                KOTLIN_NATIVE_TARGET_PRESET_CLASS_FQNAME
        ),
        typeName(
            if (target in testableNativeTargets)
                KOTLIN_NATIVE_TARGET_WITH_TESTS_CLASS_FQNAME
            else
                KOTLIN_NATIVE_TARGET_CLASS_FQNAME
        )
    )
}

internal val allPresetEntries = listOf(
    jvmPresetEntry,
    jsPresetEntry,
    androidPresetEntry
) + nativePresetEntries
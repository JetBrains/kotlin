/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.target

/**
 * Name of a preset used in the 'kotlin-multiplatform' Gradle plugin to represent this target.
 */
val KonanTarget.presetName: String
    get() = when (this) {
        KonanTarget.ANDROID_ARM32 -> "androidNativeArm32"
        KonanTarget.ANDROID_ARM64 -> "androidNativeArm64"
        KonanTarget.ANDROID_X86 -> "androidNativeX86"
        KonanTarget.ANDROID_X64 -> "androidNativeX64"
        else -> evaluatePresetName(this.name)
    }

private fun evaluatePresetName(targetName: String): String {
    val nameParts = targetName.split('_').mapNotNull { it.takeIf(String::isNotEmpty) }
    return nameParts.asSequence().drop(1).joinToString("", nameParts.firstOrNull().orEmpty()) { it.capitalize() }
}

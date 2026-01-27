/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget

internal val Project.kotlin: KotlinMultiplatformExtension
    get() = extensions.getByName("kotlin") as KotlinMultiplatformExtension

val hostKotlinNativeTargetName: String
    get() = when (HostManager.host) {
        KonanTarget.LINUX_X64 -> "linuxX64"
        KonanTarget.MACOS_ARM64 -> "macosArm64"
        KonanTarget.MINGW_X64 -> "mingwX64"
        else -> error("Unexpected host: ${HostManager.host}")
    }

internal fun KotlinMultiplatformExtension.hostTarget(): KotlinNativeTarget = when (HostManager.host) {
    KonanTarget.LINUX_X64 -> linuxX64()
    KonanTarget.MACOS_ARM64 -> macosArm64()
    KonanTarget.MINGW_X64 -> mingwX64()
    else -> error("Unexpected host: ${HostManager.host}")
}

/**
 * Common set of targets on which benchmarks are available
 */
fun KotlinMultiplatformExtension.benchmarkingTargets() {
    linuxX64()
    macosArm64()
    mingwX64()
}

val String.capitalized: String
    get() = replaceFirstChar { it.uppercaseChar() }
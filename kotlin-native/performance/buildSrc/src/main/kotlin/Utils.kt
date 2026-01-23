/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.*

internal val Project.kotlin: KotlinMultiplatformExtension
    get() = extensions.getByName("kotlin") as KotlinMultiplatformExtension

val Project.platformManager
    get() = findProperty("platformManager") as PlatformManager

val Project.kotlinNativeDist
    get() = rootProject.file(property("kotlin.native.home") as String)

val Project.hostKotlinNativeTarget: KotlinNativeTarget
    get() = when(HostManager.host) {
        KonanTarget.LINUX_X64 -> project.kotlin.linuxX64()
        KonanTarget.MACOS_ARM64 -> project.kotlin.macosArm64()
        KonanTarget.MINGW_X64 -> project.kotlin.mingwX64()
        else -> error("Unexpected host: ${HostManager.host}")
    }

fun KotlinMultiplatformExtension.benchmarkingTargets() {
    linuxX64()
    macosArm64()
    mingwX64()
}
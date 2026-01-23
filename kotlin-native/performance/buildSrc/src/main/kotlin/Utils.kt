/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin

import org.gradle.api.Project
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByName
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.*

internal val Project.kotlin: KotlinMultiplatformExtension
    get() = extensions.getByName("kotlin") as KotlinMultiplatformExtension

val Project.platformManager
    get() = findProperty("platformManager") as PlatformManager

val Project.kotlinNativeDist
    get() = rootProject.file(property("kotlin.native.home") as String)

val hostKotlinNativeTargetName: String
    get() = when(HostManager.host) {
        KonanTarget.LINUX_X64 -> "linuxX64"
        KonanTarget.MACOS_ARM64 -> "macosArm64"
        KonanTarget.MINGW_X64 -> "mingwX64"
        else -> error("Unexpected host: ${HostManager.host}")
    }

internal val Project.hostKotlinNativeTarget: KotlinNativeTarget
    get() = project.kotlin.targets.getByName(hostKotlinNativeTargetName, KotlinNativeTarget::class)

fun KotlinMultiplatformExtension.benchmarkingTargets() {
    linuxX64()
    macosArm64()
    mingwX64()
}
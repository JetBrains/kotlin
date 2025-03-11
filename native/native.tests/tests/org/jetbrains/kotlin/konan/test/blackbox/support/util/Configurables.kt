/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.util

import org.jetbrains.kotlin.konan.target.Configurables
import org.jetbrains.kotlin.konan.target.Distribution
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.PlatformManager
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeHome
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.KotlinNativeTargets

fun buildConfigurables(home: KotlinNativeHome, targets: KotlinNativeTargets): Configurables {
    val distribution = Distribution(
        home.dir.path,
        // Development variant of LLVM is used to have utilities like FileCheck
        propertyOverrides = mapOf("llvmHome.${HostManager.hostName}" to "\$llvm.${HostManager.hostName}.dev")
    )
    return PlatformManager(distribution).platform(targets.testTarget).configurables
}
/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.blackbox.support.settings

import org.jetbrains.kotlin.config.nativeBinaryOptions.BinaryOptions
import org.jetbrains.kotlin.konan.target.*
import org.jetbrains.kotlin.test.settings.Settings
import org.jetbrains.kotlin.test.settings.SimpleTestClassSettings
import org.jetbrains.kotlin.test.settings.SimpleTestRunSettings
import org.jetbrains.kotlin.test.settings.TestClassSettings
import org.jetbrains.kotlin.test.settings.TestProcessSettings
import org.jetbrains.kotlin.test.settings.TestRunSettings

typealias Settings = Settings
typealias TestProcessSettings = TestProcessSettings
typealias TestClassSettings = TestClassSettings
typealias TestRunSettings = TestRunSettings
typealias SimpleTestClassSettings = SimpleTestClassSettings
typealias SimpleTestRunSettings = SimpleTestRunSettings

val Settings.configurables: Configurables
    get() {
        val propertyOverrides = buildMap {
            // Development variant of LLVM is used to have utilities like FileCheck
            put("llvmHome.${HostManager.hostName}", "\$llvm.${HostManager.hostName}.dev")

            val macabi = get<ExplicitBinaryOptions>().getOrNull(BinaryOptions.macabi) ?: false
            if (macabi) {
                // The same as in KonanConfig. See the motivation there.
                val target = get<KotlinNativeTargets>().testTarget
                val arch = when (target) {
                    KonanTarget.IOS_X64 -> "x86_64"
                    KonanTarget.IOS_SIMULATOR_ARM64 -> "arm64"
                    else -> error("Unsupported target for macabi: $target")
                }
                put("targetTriple.${target.name}", "$arch-apple-ios-macabi")
                put("targetSysRoot.${target.name}", "\$targetSysRoot.macos_arm64")
            }
        }
        val distribution = Distribution(
            get<KotlinNativeHome>().dir.path,
            propertyOverrides = propertyOverrides
        )
        return PlatformManager(distribution).platform(get<KotlinNativeTargets>().testTarget).configurables
    }

/**
 * True, when all tests are required to have platform libraries available.
 */
val Settings.withPlatformLibs: Boolean
    get() = get<XCTestRunner>().isEnabled // XCTest depends on platform libraries, so platform libraries must be available.

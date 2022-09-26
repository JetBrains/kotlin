/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.Architecture.ARM32
import org.jetbrains.kotlin.konan.target.Architecture.ARM64
import org.jetbrains.kotlin.konan.target.Family.*
import org.jetbrains.kotlin.konan.target.KonanTarget

internal object Xcode {
    val currentVersion: String by lazy {
        val out = runCommand(listOf("/usr/bin/xcrun", "xcodebuild", "-version"))
        out.lines()[0].removePrefix("Xcode ")
    }

    fun defaultBitcodeEmbeddingMode(target: KonanTarget, buildType: NativeBuildType): BitcodeEmbeddingMode {
        if (currentVersion.split(".")[0].toInt() < 14) {
            if (target.family in listOf(IOS, WATCHOS, TVOS) && target.architecture in listOf(ARM32, ARM64)) {
                when (buildType) {
                    NativeBuildType.RELEASE -> return BitcodeEmbeddingMode.BITCODE
                    NativeBuildType.DEBUG -> return BitcodeEmbeddingMode.MARKER
                }
            }
        }
        return BitcodeEmbeddingMode.DISABLE
    }
}
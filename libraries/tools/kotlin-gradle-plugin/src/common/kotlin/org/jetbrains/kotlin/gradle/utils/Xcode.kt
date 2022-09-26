/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.konan.target.Architecture.ARM32
import org.jetbrains.kotlin.konan.target.Architecture.ARM64
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.Family.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly

internal object Xcode {
    val currentVersion: String by lazy {
        val out = runCommand(listOf("/usr/bin/xcrun", "xcodebuild", "-version"))
        out.lines()[0].removePrefix("Xcode ")
    }

    private val defaultTestDevices: Map<Family, String> by lazy {
        val osRegex = "-- .* --".toRegex()
        val deviceRegex = """[0-9A-F]{8}-([0-9A-F]{4}-){3}[0-9A-F]{12}""".toRegex()

        val out = runCommand(listOf("/usr/bin/xcrun", "simctl", "list"))

        val result = mutableMapOf<String, String>()
        var os: String? = null
        out.lines().forEach { s ->
            val osFound = osRegex.find(s)?.value
            if (osFound != null) {
                os = osFound.split(" ")[1]
            } else {
                val currentOs = os
                if (currentOs != null) {
                    val deviceFound = deviceRegex.find(s)?.value
                    if (deviceFound != null) {
                        result[currentOs] = deviceFound
                        os = null
                    }
                }
            }
        }

        result.entries.associate { (osName, deviceId) ->
            Family.valueOf(osName.toUpperCaseAsciiOnly()) to deviceId
        }
    }

    fun getDefaultTestDeviceId(target: KonanTarget): String? = defaultTestDevices[target.family]

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
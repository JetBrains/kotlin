/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.internal

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.mpp.BitcodeEmbeddingMode
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly
import java.io.ByteArrayOutputStream
import java.io.File

@Suppress("LeakingThis")
internal open class XcodeVersion : Exec() {
    init {
        onlyIf { HostManager.hostIsMac }
        commandLine(listOf("/usr/bin/xcrun", "xcodebuild", "-version"))
        standardOutput = ByteArrayOutputStream()
    }

    @get:OutputFile
    val outputFile: Provider<File> = project.provider {
        project.rootProject.buildDir.resolve("xcode/version")
    }

    @Internal
    val currentVersion: Provider<String> = outputFile.map { file ->
        if (file.exists()) file.readText()
        else error("XcodeVersion file '${file.path}' does not exist!")
    }

    @TaskAction
    fun run() {
        val output = standardOutput.toString()
        val versionName = output.lines().first().removePrefix("Xcode ")

        val file = outputFile.get()
        file.delete()
        file.createNewFile()
        file.writeText(versionName)
    }
}

internal fun Project.getXcodeVersion(): Provider<String> =
    locateOrRegisterTask<XcodeVersion>("getXcodeVersion")
        .map { task -> task.currentVersion.get() }

internal fun Project.getDefaultBitcodeEmbeddingMode(
    target: KonanTarget,
    buildType: NativeBuildType
): Provider<BitcodeEmbeddingMode> {
    if (!HostManager.hostIsMac) return provider { BitcodeEmbeddingMode.DISABLE }
    return getXcodeVersion().map { currentVersion ->
        var mode: BitcodeEmbeddingMode = BitcodeEmbeddingMode.DISABLE
        if (currentVersion.split(".")[0].toInt() < 14) {
            if (
                target.family in listOf(Family.IOS, Family.WATCHOS, Family.TVOS)
                && target.architecture in listOf(Architecture.ARM32, Architecture.ARM64)
            ) {
                mode = when (buildType) {
                    NativeBuildType.RELEASE -> BitcodeEmbeddingMode.BITCODE
                    NativeBuildType.DEBUG -> BitcodeEmbeddingMode.MARKER
                }
            }
        }
        mode
    }
}

@Suppress("LeakingThis")
internal open class XcodeSimulators : Exec() {
    private val osRegex = "-- .* --".toRegex()
    private val deviceRegex = """[0-9A-F]{8}-([0-9A-F]{4}-){3}[0-9A-F]{12}""".toRegex()

    init {
        onlyIf { HostManager.hostIsMac }
        commandLine(listOf("/usr/bin/xcrun", "simctl", "list", "devices", "available"))
        standardOutput = ByteArrayOutputStream()
    }

    @get:OutputFile
    val outputFile: Provider<File> = project.provider {
        project.rootProject.buildDir.resolve("xcode/simulators")
    }

    @Internal
    val testDevices: Provider<Map<Family, String>> = outputFile.map { file ->
        if (file.exists()) {
            file.readText().lines().associate { line ->
                val key = line.substringBefore("=")
                val value = line.substringAfter("=")
                Family.valueOf(key.toUpperCaseAsciiOnly()) to value
            }
        } else error("XcodeSimulators file '${file.path}' does not exist!")
    }

    @TaskAction
    fun run() {
        val output = standardOutput.toString()
        val osToDevice = mutableMapOf<String, String>()
        var os: String? = null
        output.lines().forEach { s ->
            val osFound = osRegex.find(s)?.value
            if (osFound != null) {
                os = osFound.split(" ")[1]
            } else {
                val currentOs = os
                if (currentOs != null) {
                    val deviceFound = deviceRegex.find(s)?.value
                    if (deviceFound != null) {
                        osToDevice[currentOs] = deviceFound
                        os = null
                    }
                }
            }
        }

        val file = outputFile.get()
        file.delete()
        file.createNewFile()
        file.writeText(osToDevice.map { (os, id) -> "$os=$id" }.joinToString("\n"))
    }
}

internal fun Project.getXcodeSimulators(): Provider<Map<Family, String>> =
    locateOrRegisterTask<XcodeSimulators>("getXcodeSimulators")
        .map { task -> task.testDevices.get() }

internal fun Project.getDefaultXcodeTestDeviceId(target: KonanTarget): Provider<String> {
    if (!HostManager.hostIsMac) return provider { "non_mac_host" }
    return getXcodeSimulators().map { testDevices ->
        testDevices[target.family]
            ?: error("Xcode does not support simulator tests for ${target.name}. Check that requested SDK is installed.")
    }
}
/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.api.logging.LogLevel
import org.gradle.internal.logging.LoggingConfigurationBuildOptions.StacktraceOption
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS

private val EMULATOR_SCRIPT = """
            |#!/bin/sh
            |DIR="$(dirname "$(readlink -f "$0")")"
            |echo "${'$'}@" -no-accel -qemu -machine virt
            |"${'$'}DIR/emulator1" "$@" -no-accel -qemu -machine virt
            """.trimMargin()

/**
 * This function patches the Android emulator to always run without acceleration (-no-accel).
 * This is a necessary hack due to the inability to pass the -no-accel argument directly to a Gradle-managed emulator run.
 * It allows builds to proceed on servers that do not support nested virtualization.
 *
 * Workaround of https://issuetracker.google.com/issues/241029220
 */
private fun patchEmulatorAlwaysNoAccel() {
    val emulatorDir = KtTestUtil.findAndroidSdk().resolve("emulator")
    val emulatorFile = emulatorDir.resolve("emulator")
    val emulator1File = emulatorDir.resolve("emulator1")
    if (!emulator1File.isFile) {
        emulatorFile.renameTo(emulator1File)
        emulatorFile.writeText(EMULATOR_SCRIPT)
        emulatorFile.setExecutable(true)
    } else if (emulatorFile.readText() != EMULATOR_SCRIPT) {
        emulatorFile.writeText(EMULATOR_SCRIPT)
        emulatorFile.setExecutable(true)
    }
}

@DisplayName("codegen tests on android")
@AndroidTestVersions(minVersion = TestVersions.AGP.AGP_83, maxVersion = TestVersions.AGP.AGP_83)
@OsCondition(supportedOn = [OS.MAC, OS.LINUX], enabledOnCI = [OS.MAC, OS.LINUX])
@AndroidCodegenTests
class AndroidCodegenIT : KGPBaseTest() {
    @GradleAndroidTest
    fun testAndroidCodegen(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        project(
            "codegen-tests",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                androidVersion = agpVersion,
                freeArgs = listOf(
                    "-Pandroid.useAndroidX=true",
                    "-Pandroid.experimental.testOptions.managedDevices.setupTimeoutMinutes=0"
                ),
                logLevel = LogLevel.INFO,
                stacktraceMode = StacktraceOption.STACKTRACE_SHORT_OPTION
            ),
            enableGradleDebug = false,
            buildJdk = jdkVersion.location
        ) {
            build(
                "assembleAndroidTest",
                enableGradleDaemonMemoryLimitInMb = 6000
            )
            var nonAcceleratedEmulator = false
            try {
                build("nexusSetup", forceOutput = true, enableGradleDaemonMemoryLimitInMb = 6000)
            } catch (e: org.gradle.testkit.runner.UnexpectedBuildFailure) {
                patchEmulatorAlwaysNoAccel()
                nonAcceleratedEmulator = true
            }
            build(
                "nexusCheck",
                buildOptions = buildOptions.copy(
                    freeArgs = if (nonAcceleratedEmulator)
                        buildOptions.freeArgs + "-Pandroid.testoptions.manageddevices.emulator.gpu=guest" + "-PforceArmEmulator=true"
                    else
                        buildOptions.freeArgs
                ),
                forceOutput = true,
                enableGradleDaemonMemoryLimitInMb = 6000
            )
        }
    }
}

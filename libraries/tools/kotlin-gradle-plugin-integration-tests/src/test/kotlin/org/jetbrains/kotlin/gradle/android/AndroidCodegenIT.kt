/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS

@DisplayName("codegen tests on android")
@AndroidTestVersions(minVersion = TestVersions.AGP.AGP_84, maxVersion = TestVersions.AGP.AGP_84)
/*
 * Non-virtualized runners are necessary with current configuration.
 * This build utilizes the Android Emulator to execute codegen tests,
 * requiring a VM hypervisor for optimal performance.
 * Nested virtualization on CI runners except physical Macs fails.
 */
@OsCondition(enabledOnCI = [OS.MAC])
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
                freeArgs = listOf("-Pandroid.useAndroidX=true", "-Pandroid.experimental.testOptions.managedDevices.setupTimeoutMinutes=0"),
                logLevel = LogLevel.LIFECYCLE
            ),
            enableGradleDebug = false,
            buildJdk = jdkVersion.location
        ) {
            build("assembleAndroidTest", forceOutput = true, enableGradleDaemonMemoryLimitInMb = 6000)
            build("nexusCheck", forceOutput = true, enableGradleDaemonMemoryLimitInMb = 6000)
        }
    }
}

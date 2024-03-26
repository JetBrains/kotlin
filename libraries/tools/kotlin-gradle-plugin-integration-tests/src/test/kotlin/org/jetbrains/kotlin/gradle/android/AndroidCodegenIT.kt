/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.api.logging.LogLevel
import org.gradle.internal.logging.LoggingConfigurationBuildOptions.StacktraceOption
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS

@DisplayName("codegen tests on android")
@AndroidTestVersions(minVersion = TestVersions.AGP.AGP_71, maxVersion = TestVersions.AGP.AGP_71)
@GradleTestVersions(minVersion = TestVersions.Gradle.G_6_9, maxVersion = TestVersions.Gradle.G_6_9)
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
                freeArgs = listOf("-Pandroid.useAndroidX=true"),
                logLevel = LogLevel.INFO,
                stacktraceMode = StacktraceOption.STACKTRACE_SHORT_OPTION
            ),
            enableGradleDebug = false,
            buildJdk = jdkVersion.location
        ) {
//            makeSnapshotTo("/Users/Iaroslav.Postovalov/IdeaProjects/kotlin/build/snapshot/codegen-tests")
//            build(
//                "assembleAndroidTest",
//                enableGradleDaemonMemoryLimitInMb = 6000
//            )
//            build(
//                "nexusCheck",
//                forceOutput = true,
//                enableGradleDaemonMemoryLimitInMb = 6000
//            )
        }
    }
}

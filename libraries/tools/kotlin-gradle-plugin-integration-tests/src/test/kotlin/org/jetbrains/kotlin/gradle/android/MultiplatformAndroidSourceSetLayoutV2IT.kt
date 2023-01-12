/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.AGPVersion
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertNull

@MppGradlePluginTests
@DisplayName("Multiplatform Android Source Set Layout 2")
class MultiplatformAndroidSourceSetLayoutV2IT : KGPBaseTest() {

    @GradleAndroidTest
    @DisplayName("test Android project with flavors")
    @AndroidTestVersions(minVersion = "7.0.4")
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0) // due AGP version limit ^
    fun testProjectWithFlavors(gradleVersion: GradleVersion, agpVersion: String, jdkVersion: JdkVersions.ProvidedJdk) {
        project(
            "multiplatformAndroidSourceSetLayout2",
            gradleVersion,
            defaultBuildOptions.copy(
                androidVersion = agpVersion,
                // Workaround for a deprecation warning from AGP
                // Relying on FileTrees for ignoring empty directories when using @SkipWhenEmpty has been deprecated.
                warningMode = if (AGPVersion.fromString(agpVersion) <= AGPVersion.v7_1_0) WarningMode.None else WarningMode.Fail,
            ),
            buildJdk = jdkVersion.location
        ) {
            build("test") {
                assertTasksExecuted(":testUsaPaidReleaseUnitTest")
                assertTasksExecuted(":testUsaPaidDebugUnitTest")
                assertTasksExecuted(":testUsaFreeReleaseUnitTest")
                assertTasksExecuted(":testUsaFreeDebugUnitTest")

                assertTasksExecuted(":testGermanPaidReleaseUnitTest")
                assertTasksExecuted(":testGermanPaidDebugUnitTest")
                assertTasksExecuted(":testGermanFreeReleaseUnitTest")
                assertTasksExecuted(":testGermanFreeDebugUnitTest")

                assertNull(task(":assembleUsaPaidDebugAndroidTest"))
                assertNull(task(":assembleUsaFreeDebugAndroidTest"))
                assertNull(task(":assembleGermanPaidDebugAndroidTest"))
                assertNull(task(":assembleGermanFreeDebugAndroidTest"))
            }

            build("assembleAndroidTest") {
                assertTasksExecuted(":assembleUsaPaidDebugAndroidTest")
                assertTasksExecuted(":assembleUsaFreeDebugAndroidTest")
                assertTasksExecuted(":assembleGermanPaidDebugAndroidTest")
                assertTasksExecuted(":assembleGermanFreeDebugAndroidTest")
            }
        }
    }
}
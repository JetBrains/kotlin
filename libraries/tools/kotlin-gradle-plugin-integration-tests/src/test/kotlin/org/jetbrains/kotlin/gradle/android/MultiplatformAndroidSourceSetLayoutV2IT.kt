/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertNull

@MppGradlePluginTests
@DisplayName("Multiplatform Android Source Set Layout 2")
class MultiplatformAndroidSourceSetLayoutV2IT : KGPBaseTest() {

    @GradleAndroidTest
    @DisplayName("test Android project with flavors")
    fun testProjectWithFlavors(gradleVersion: GradleVersion, agpVersion: String, jdkVersion: JdkVersions.ProvidedJdk) {
        project(
            "multiplatformAndroidSourceSetLayout2",
            gradleVersion,
            defaultBuildOptions.copy(androidVersion = agpVersion),
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
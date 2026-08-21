/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android.externalAndroidTarget

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*

// Used AGP 9.0 as the minimal stable version supported for the android library compose setup.
@AndroidTestVersions(minVersion = TestVersions.AGP.AGP_90)
@AndroidGradlePluginTests
class AllTestsExternalAndroidTargetIT : KGPBaseTest() {

    @GradleAndroidTest
    fun `test - allTests depends on android JVM tests from Kotlin and Java sources`(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        externalAndroidLibraryProject(
            gradleVersion = gradleVersion,
            androidVersion = androidVersion,
            jdkVersion = jdkVersion,
            namespace = "org.jetbrains.sample.alltests",
            withJava = true,
            androidLibraryConfiguration = {
                withHostTest {}
                withDeviceTest {}
            },
        ) {
            projectPath.source("src/androidMain/kotlin/AndroidMain.kt") {
                """
                class AndroidMain
                """.trimIndent()
            }
            projectPath.source("src/androidHostTest/kotlin/AndroidKotlinAllTestsTest.kt") {
                """
                class AndroidKotlinAllTestsTest
                """.trimIndent()
            }
            projectPath.source("src/androidHostTest/java/AndroidJavaAllTestsTest.java") {
                """
                public class AndroidJavaAllTestsTest {}
                """.trimIndent()
            }
            projectPath.source("src/androidDeviceTest/kotlin/AndroidDeviceAllTestsTest.kt") {
                """
                class AndroidDeviceAllTestsTest
                """.trimIndent()
            }
            projectPath.source("src/androidDeviceTest/java/AndroidJavaDeviceAllTestsTest.java") {
                """
                public class AndroidJavaDeviceAllTestsTest {}
                """.trimIndent()
            }

            build("allTests", "--dry-run") {
                assertOutputContains(":allTests SKIPPED")
                assertOutputContains(":compileAndroidHostTest SKIPPED")
                assertOutputContains(":compileAndroidHostTestJavaWithJavac SKIPPED")
                assertOutputContains(":testAndroidHostTest SKIPPED")
                assertTasksAreNotInTaskGraph(
                    ":compileAndroidDeviceTest",
                    ":compileAndroidDeviceTestJavaWithJavac",
                    ":connectedAndroidDeviceTest",
                )
            }
        }
    }
}

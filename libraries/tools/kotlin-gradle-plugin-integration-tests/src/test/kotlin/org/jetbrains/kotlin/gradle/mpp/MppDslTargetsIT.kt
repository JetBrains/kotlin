/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.testResolveAllConfigurations
import org.jetbrains.kotlin.test.TestMetadata

@MppGradlePluginTests
class MppDslTargetsIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata(value = "newMppMultipleTargetsSamePlatform")
    fun testMultipleTargetsSamePlatform(gradleVersion: GradleVersion) {
        project(
            projectName = "newMppMultipleTargetsSamePlatform",
            gradleVersion = gradleVersion,
        ) {
            testResolveAllConfigurations("app") { _, result ->
                with(result) {
                    assertOutputContains(">> :app:junitCompileClasspath --> lib-junit.jar")
                    assertOutputContains(">> :app:junitCompileClasspath --> junit-4.13.2.jar")

                    assertOutputContains(">> :app:mixedJunitCompileClasspath --> lib-junit.jar")
                    assertOutputContains(">> :app:mixedJunitCompileClasspath --> junit-4.13.2.jar")

                    assertOutputContains(">> :app:testngCompileClasspath --> lib-testng.jar")
                    assertOutputContains(">> :app:testngCompileClasspath --> testng-6.14.3.jar")

                    assertOutputContains(">> :app:mixedTestngCompileClasspath --> lib-testng.jar")
                    assertOutputContains(">> :app:mixedTestngCompileClasspath --> testng-6.14.3.jar")
                }
            }
        }
    }
}

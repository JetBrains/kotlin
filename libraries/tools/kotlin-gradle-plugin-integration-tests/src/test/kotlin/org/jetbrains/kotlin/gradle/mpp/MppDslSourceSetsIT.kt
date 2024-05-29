/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata

@MppGradlePluginTests
class MppDslSourceSetsIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-and-app/sample-lib")
    fun testSourceSetCyclicDependencyDetection(gradleVersion: GradleVersion) {
        project(
            projectName = "new-mpp-lib-and-app/sample-lib",
            gradleVersion = gradleVersion,
        ) {
            buildGradle.append(
                """
                kotlin.sourceSets {
                    a
                    b { dependsOn a }
                    c { dependsOn b }
                    a.dependsOn(c)
                }
                """.trimIndent()
            )

            buildAndFail("assemble") {
                assertOutputContains("a -> c -> b -> a")
            }
        }
    }

    @GradleTest
    @TestMetadata(value = "mpp-empty-sources")
    fun testPublishEmptySourceSets(gradleVersion: GradleVersion) {
        project(
            projectName = "mpp-empty-sources",
            gradleVersion = gradleVersion,
        ) {
            build("publish") {
                assertTasksNoSource(
                    ":compileKotlinJs",
                    ":compileKotlinJvm",
                )
                assertTasksExecuted(":publish")
            }
        }
    }
}

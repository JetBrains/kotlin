/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata

@MppGradlePluginTests
class MppDslResourcesIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-and-app/sample-lib")
    fun testResourceProcessing(gradleVersion: GradleVersion) {
        project(
            projectName = "new-mpp-lib-and-app/sample-lib",
            gradleVersion = gradleVersion,
        ) {
            val targetsWithResources = listOf("jvm6", "nodeJs", "linux64")
            val processResourcesTasks = targetsWithResources.map { ":${it}ProcessResources" }

            build(
                buildArguments = processResourcesTasks.toTypedArray()
            ) {
                assertTasksExecuted(processResourcesTasks)

                targetsWithResources.forEach {
                    assertFileInProjectExists("build/processedResources/$it/main/commonMainResource.txt")
                    assertFileInProjectExists("build/processedResources/$it/main/${it}MainResource.txt")
                }
            }
        }
    }
}

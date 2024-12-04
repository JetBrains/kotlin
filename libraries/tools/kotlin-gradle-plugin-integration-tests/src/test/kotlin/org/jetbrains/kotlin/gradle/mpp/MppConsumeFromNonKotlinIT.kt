/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata

@MppGradlePluginTests
class MppConsumeFromNonKotlinIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-and-app")
    fun testConsumeMppLibraryFromNonKotlinProject(gradleVersion: GradleVersion) {

        nativeProject(
            projectName = "new-mpp-lib-and-app/sample-lib",
            gradleVersion = gradleVersion,
            localRepoDir = defaultLocalRepo(gradleVersion),
        ) {
            build("publish")
        }

        project(
            projectName = "new-mpp-lib-and-app/sample-app-without-kotlin",
            gradleVersion = gradleVersion,
            localRepoDir = defaultLocalRepo(gradleVersion),
        ) {
            build("assemble") {
                assertTasksExecuted(":compileJava")
                assertFileInProjectExists("build/classes/java/main/A.class")
            }
        }
    }
}

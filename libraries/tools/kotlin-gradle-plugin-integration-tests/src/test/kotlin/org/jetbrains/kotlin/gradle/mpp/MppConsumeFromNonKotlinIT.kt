/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.mpp

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import kotlin.io.path.appendText
import kotlin.io.path.invariantSeparatorsPathString

@MppGradlePluginTests
class MppConsumeFromNonKotlinIT : KGPBaseTest() {

    @GradleTest
    @TestMetadata(value = "new-mpp-lib-and-app")
    fun testConsumeMppLibraryFromNonKotlinProject(gradleVersion: GradleVersion) {

        val localRepoDir = defaultLocalRepo(gradleVersion)

        nativeProject(
            projectName = "new-mpp-lib-and-app/sample-lib",
            gradleVersion = gradleVersion,
            localRepoDir = localRepoDir,
        ) {
            // TODO KT-67566 once all MppDslPublishedMetadataIT test are updated to new test DSL, update the projects to use `<localRepo>`
            buildGradle.appendText(
                """
                publishing {
                  repositories {
                    maven {
                      name = "LocalRepo"
                      url = uri("${localRepoDir.invariantSeparatorsPathString}")
                    }
                  }
                }
                """.trimIndent()
            )

            build("publish")
        }

        project(
            projectName = "new-mpp-lib-and-app/sample-app-without-kotlin",
            gradleVersion = gradleVersion,
            localRepoDir = localRepoDir,
        ) {
            build("assemble") {
                assertTasksExecuted(":compileJava")
                assertFileInProjectExists("build/classes/java/main/A.class")
            }
        }
    }
}

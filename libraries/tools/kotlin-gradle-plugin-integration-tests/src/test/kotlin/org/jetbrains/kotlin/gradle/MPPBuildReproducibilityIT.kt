/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.testkit.runner.TaskOutcome
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.appendText
import kotlin.test.assertTrue

/* Only supported on platforms that support the 'diff' util */
@OsCondition(supportedOn = [OS.LINUX, OS.MAC])
@MppGradlePluginTests
@DisplayName("Multiplatform Build Reproducibility")
class MPPBuildReproducibilityIT : KGPBaseTest() {

    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
    @DisplayName("Check if consecutive builds produce same binaries: Simple Multiplatform project")
    fun `test simpleMultiplatformProject`(gradleVersion: GradleVersion) {
        project("MPPBuildReproducibility/simpleMultiplatformProject", gradleVersion) {
            assertConsecutiveBuildsProduceSameBinaries()
        }
    }
}

/**
 * This will test if building a project consecutively will result in the same binaries.
 * This method will inject code into the build.gradle.kts file to perform the following:
 *
 * - publish the project into a local repository (repo1)
 * - publish the project into a local repository (repo2) (whilst --rerun-tasks guarantees everything is re-built)
 * - Run the 'diff' tool to ensure that the published artifacts are equal in repo1 & repo2
 */
private fun TestProject.assertConsecutiveBuildsProduceSameBinaries() {
    buildGradleKts.appendText(
        """
        val repo1 = buildDir.resolve("repo1")
        val repo2 = buildDir.resolve("repo2")

        publishing {
            repositories {
                maven(repo1) { name = "repo1" }
                maven(repo2) { name = "repo2" }
            }
        }

        tasks.register<Exec>("checkBuildConsistency") {
            doFirst {
                if (!repo1.exists()) {
                    throw IllegalStateException("Missing repo1")
                }

                if (!repo2.exists()) {
                    throw IllegalStateException("Missing repo2")
                }
            }

            commandLine(
                "diff", repo1.absolutePath, repo2.absolutePath, "-r", "-x", "maven-metadata**"
            )
        }
        """.trimIndent()
    )

    build("publishAllPublicationsToRepo1Repository")
    build("publishAllPublicationsToRepo2Repository", "--rerun-tasks") {
        tasks.forEach { task ->
            assertTrue(
                task.outcome in setOf(TaskOutcome.SUCCESS, TaskOutcome.NO_SOURCE),
                "Expected all tasks to be re-executed. Task ${task.path} was ${task.outcome}"
            )
        }
    }

    build("checkBuildConsistency", forceOutput = true)
}

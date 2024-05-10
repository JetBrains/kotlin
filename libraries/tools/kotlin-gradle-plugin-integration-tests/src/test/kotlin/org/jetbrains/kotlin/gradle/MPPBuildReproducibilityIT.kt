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
import java.io.File
import kotlin.io.path.appendText
import kotlin.test.assertTrue
import kotlin.test.fail

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
    fun File.relativeToProject() = this.relativeTo(projectPath.toFile())

    fun Diff.reportString(): String = when (this) {
        is Diff.DifferentContent -> "Different File Contents: ${first.relativeToProject()} <-> ${second.relativeToProject()}"
        is Diff.MissingFile -> "Missing File: ${file.relativeToProject()}"
        is Diff.TypeMismatch -> "Expected directory, but is file: ${file.relativeToProject()}"
    }

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
        """.trimIndent()
    )

    build("publishAllPublicationsToRepo1Repository")
    build("publishAllPublicationsToRepo2Repository", "--rerun-tasks") {
        tasks.forEach { task ->
            assertTrue(
                task.outcome in setOf(TaskOutcome.SUCCESS, TaskOutcome.SKIPPED, TaskOutcome.NO_SOURCE),
                "Expected all tasks to be re-executed. Task ${task.path} was ${task.outcome}"
            )
        }
    }

    val repo1 = projectPath.resolve("build/repo1").toFile()
    val repo2 = projectPath.resolve("build/repo2").toFile()

    val diffs = diff(repo1, repo2).filter { diff -> "maven-metadata" !in diff.fileName }
    if (diffs.isNotEmpty()) {
        fail(
            buildString {
                appendLine("${repo1.relativeToProject()} <-> ${repo2.relativeToProject()} are inconsistent!")
                diffs.forEach { diff ->
                    appendLine(diff.reportString())
                }
            }
        )
    }
}

private sealed class Diff {
    data class MissingFile(val file: File) : Diff()
    data class DifferentContent(val first: File, val second: File) : Diff()
    data class TypeMismatch(val directory: File, val file: File) : Diff()

    val fileName: String
        get() = when (this) {
            is DifferentContent -> first.name
            is MissingFile -> file.name
            is TypeMismatch -> directory.name
        }
}

private fun diff(firstRoot: File, secondRoot: File): Set<Diff> {
    return firstRoot.walkTopDown().flatMap { firstFile ->
        val secondFile = secondRoot.resolve(firstFile.relativeTo(firstRoot))

        if (!secondFile.exists()) {
            return@flatMap listOf(Diff.MissingFile(secondFile))
        }

        /* File is directory */
        if (firstFile.isDirectory) {
            if (!secondFile.isDirectory) {
                return@flatMap listOf(Diff.TypeMismatch(directory = firstFile, file = secondFile))
            }

            /* Check if all children in the second directory are also present in firstFile */
            return@flatMap secondFile.listFiles().orEmpty().mapNotNull { secondFileChild ->
                val firstFileChild = firstRoot.resolve(secondFileChild.relativeTo(secondRoot))
                if (!firstFileChild.exists()) Diff.MissingFile(firstFileChild)
                else null
            }
        }

        /* File is not a directory */
        if (firstFile.length() != secondFile.length()) {
            return@flatMap listOf(Diff.DifferentContent(firstFile, secondFile))
        }

        if (!firstFile.readBytes().contentEquals(secondFile.readBytes())) {
            return@flatMap listOf(Diff.DifferentContent(firstFile, secondFile))
        }

        emptyList()
    }.toSet()
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.tasks.withType
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class RequiresNpmDependenciesTaskTest {

    @Test
    fun `rerun token input changes between builds - legacy configuration-based npm dependency`() {
        val token1 = setupProject(useNpmDsl = false).webpackTask().rerunToken()
        val token2 = setupProject(useNpmDsl = false).webpackTask().rerunToken()

        assertNotEquals(token1, token2, "The rerun token input must change between builds.")
    }

    @Test
    fun `rerun token input is set - legacy configuration-based npm dependency`() {
        val project = setupProject(useNpmDsl = false)
        val task = project.webpackTask()

        assertEquals(true, task.rerunToken()?.isNotEmpty(), "Task ${task.path} must have a rerun token.")
    }

    @Test
    fun `rerun token input is empty - npm DSL dependency`() {
        val project = setupProject(useNpmDsl = true)
        val task = project.webpackTask()

        assertEquals("", task.rerunToken(), "Task ${task.path} must not have a rerun token.")
    }

    companion object {
        private fun setupProject(useNpmDsl: Boolean): Project {
            val project = buildProjectWithMPP()
            val fileBasedNpmDep = project.projectDir.resolve("fileBasedNpmDep").apply { mkdirs() }

            project.configureRepositoriesForTests()

            project.kotlin {
                js {
                    browser()
                    binaries.executable()
                }

                sourceSets.commonMain {
                    dependencies {
                        if (useNpmDsl) {
                            npm("fileBasedNpmDep", fileBasedNpmDep)
                        } else {
                            implementation(npm("fileBasedNpmDep", fileBasedNpmDep))
                        }
                    }
                }
            }

            return project.evaluate()
        }

        private fun Project.webpackTask(): RequiresNpmDependenciesTask =
            tasks.withType<RequiresNpmDependenciesTask>().getByName("jsBrowserProductionWebpack")

        private fun RequiresNpmDependenciesTask.rerunToken(): String? =
            inputs.properties.getValue("fileBasedNpmDependencyLocationsRerunToken") as? String
    }
}

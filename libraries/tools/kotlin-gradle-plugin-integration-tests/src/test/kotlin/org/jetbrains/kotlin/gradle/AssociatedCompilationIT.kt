/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceFirst
import org.junit.jupiter.api.DisplayName

@DisplayName("Incremental scenarios with associated compilation")
@JvmGradlePluginTests
class AssociatedCompilationIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.copy(logLevel = LogLevel.DEBUG)

    @DisplayName("Rebuild forced if associated compilation changes")
    @GradleTest
    fun testRebuildOnAssociatedCompilationChange(gradleVersion: GradleVersion) {
        val project = getBaseProject(gradleVersion)

        val testSrc = project.kotlinSourcesDir("integrationTest").resolve("test.kt")
        val utilSrc = project.kotlinSourcesDir("main").resolve("util.kt")

        project.build("compileIntegrationTestKotlin") {
            assertCompiledKotlinSources(listOf(testSrc, utilSrc).relativizeTo(project.projectPath), output)
        }

        project.buildGradle.replaceFirst("associateWith(getByName(\"main\"))", "")

        project.buildAndFail("compileIntegrationTestKotlin") {
            assertTasksFailed(":compileIntegrationTestKotlin")
            assertOutputContains("Cannot access 'fun doWork(): Int': it is internal in file")
        }
    }

    @DisplayName("Rebuild after used internal ABI change in associated compilation")
    @GradleTest
    fun testRebuildOnUsedApiChangeInAssociatedCompilation(gradleVersion: GradleVersion) {
        val project = getBaseProject(gradleVersion)

        val testSrc = project.kotlinSourcesDir("integrationTest").resolve("test.kt")
        val utilSrc = project.kotlinSourcesDir("main").resolve("util.kt")

        project.build("compileIntegrationTestKotlin")
        utilSrc.replaceFirst("fun doWork() = 4", "fun doWork() = \"okay\"")

        project.build("compileIntegrationTestKotlin") {
            assertCompiledKotlinSources(listOf(testSrc, utilSrc).relativizeTo(project.projectPath), output)
        }
    }

    @DisplayName("No rebuild after private ABI change in associated compilation")
    @GradleTest
    fun testNoRebuildOnPrivateChangeInAssociatedCompilation(gradleVersion: GradleVersion) {
        val project = getBaseProject(gradleVersion)

        val utilSrc = project.kotlinSourcesDir("main").resolve("util.kt")

        project.build("compileIntegrationTestKotlin")
        utilSrc.append("private val bar = 2")

        project.build("compileIntegrationTestKotlin") {
            assertCompiledKotlinSources(listOf(utilSrc).relativizeTo(project.projectPath), output)
        }
    }

    @DisplayName("No rebuild after unused ABI change in associated compilation")
    @GradleTest
    fun testNoRebuildOnUnusedInternalChangeInAssociatedCompilation(gradleVersion: GradleVersion) {
        val project = getBaseProject(gradleVersion)

        val utilSrc = project.kotlinSourcesDir("main").resolve("util.kt")

        project.build("compileIntegrationTestKotlin")
        utilSrc.append("internal val bar = 2")

        project.build("compileIntegrationTestKotlin") {
            assertCompiledKotlinSources(listOf(utilSrc).relativizeTo(project.projectPath), output)
        }
    }

    private fun getBaseProject(gradleVersion: GradleVersion): TestProject {
        return project("kt-61918-associate-with-incremental", gradleVersion)
    }
}

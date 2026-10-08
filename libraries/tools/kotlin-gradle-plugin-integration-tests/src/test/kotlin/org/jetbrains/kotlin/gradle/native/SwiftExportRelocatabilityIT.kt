/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.kotlin.dsl.kotlin
import org.gradle.testkit.runner.BuildResult
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.util.swiftExportEmbedAndSignEnvVariables
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.test.assertEquals

/**
 * The Swift Export counterpart of `BuildCacheRelocationIT`. The Xcode integration registers its tasks from the Xcode
 * environment, so the builds need the environment variables and can't go through the shared helper there.
 */
@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Build cache relocation for Swift Export")
@SwiftExportGradlePluginTests
class SwiftExportRelocatabilityIT : KGPBaseTest() {

    override val defaultBuildOptions = super.defaultBuildOptions.copy(buildCacheEnabled = true)

    private val localBuildCacheDir get() = workingDir.resolve("swift-export-build-cache")

    @DisplayName("works for the Swift Export task")
    @GradleTest
    fun testRelocationSwiftExport(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        val (firstProject, secondProject) = prepareTestProjects(gradleVersion)

        firstProject.buildWithXcodeEnvironment(SWIFT_EXPORT_TASK, testBuildDir) {
            assertTasksPackedToCache(SWIFT_EXPORT_TASK)
        }
        // Checked on the first build. The second project restores the files, so it has no new content to look at.
        assertOutputHasNoProjectPaths(firstProject)

        firstProject.build("clean")

        // The whole Xcode flow, so the package generation reads the restored modules file at the new location.
        secondProject.buildWithXcodeEnvironment(EMBED_TASK, testBuildDir) {
            assertTasksFromCache(SWIFT_EXPORT_TASK)
            assertTasksExecuted(GENERATE_PACKAGE_TASK)
        }
    }

    private fun TestProject.buildWithXcodeEnvironment(task: String, testBuildDir: Path, assertions: BuildResult.() -> Unit) = build(
        task,
        environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir),
        assertions = assertions,
    )

    private fun prepareTestProjects(gradleVersion: GradleVersion): Pair<TestProject, TestProject> {
        val firstProject = projectWithDependency(gradleVersion, "first")
        val secondProject = projectWithDependency(gradleVersion, "second")
        return firstProject to secondProject
    }

    private fun projectWithDependency(gradleVersion: GradleVersion, suffix: String): TestProject =
        project("empty", gradleVersion, projectPathAdditionalSuffix = suffix) {
            enableLocalBuildCache(localBuildCacheDir)
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain {
                        compileSource(
                            """
                            import org.foo.One
                            fun foo(): One = One()
                            class Shared(val one: One)
                            """.trimIndent()
                        )
                        dependencies {
                            api(project(":dep"))
                        }
                    }
                }
            }
            val dependency = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        iosArm64()
                        sourceSets.commonMain.get().compileSource(
                            """
                            package org.foo
                            class One {
                                fun two(): Int = 2
                            }
                            """.trimIndent()
                        )
                    }
                }
            }
            include(dependency, "dep")
        }

    /** Same check as in `BuildCacheRelocationIT`: nothing in the output may mention where the project is. */
    private fun assertOutputHasNoProjectPaths(project: TestProject) {
        val projectPath = project.projectPath.toAbsolutePath().toString()
        val outputDirectory = project.projectPath.resolve("build/SwiftExport/iosArm64")
        val filesWithProjectPath = outputDirectory.walk()
            .filter { it.isRegularFile() && it.readText().contains(projectPath) }
            .map { it.relativeTo(outputDirectory).toString() }
            .sorted()
            .toList()
        assertEquals(emptyList(), filesWithProjectPath, "Output files that mention the project path")
    }

    companion object {
        private const val SWIFT_EXPORT_TASK = ":iosArm64SwiftExport"
        private const val GENERATE_PACKAGE_TASK = ":iosArm64DebugGenerateSPMPackage"
        private const val EMBED_TASK = ":embedSwiftExportForXcode"
    }
}

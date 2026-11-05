/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.api.Task
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ecosystem.KotlinEcosystemExtension
import org.jetbrains.kotlin.gradle.native.KotlinNativeToolParallelismTestProbeService.Companion.observedMaxParallelWorkers
import org.jetbrains.kotlin.gradle.native.KotlinNativeToolParallelismTestProbeService.Companion.registerParallelismProbeServiceIfAbsent
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.kotlin.gradle.testbase.BuildOptions.IsolatedProjectsMode
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.NativeGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.addEcosystemPluginToBuildScriptCompilationClasspath
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.buildScriptReturn
import org.jetbrains.kotlin.gradle.testbase.compileStubSourceWithSourceSetName
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.testbase.settingsBuildScriptInjection
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.internal.compilerRunner.native.parallelismLimitServiceName
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.test.assertEquals

@DisplayName("Kotlin/Native max workers configured in the settings plugin")
@NativeGradlePluginTests
class KotlinNativeSettingsDslIT : KGPBaseTest() {

    @DisplayName("Settings DSL values override Gradle properties, and Gradle properties apply to values the DSL does not set")
    @GradleTest
    fun testSettingsDslOverridesGradlePropertiesAndFallsBackForUnsetValues(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            applySettingsDsl(linkMaxWorkers = 4)
            applyLinuxExecutable()

            assertEquals(
                listOf(1, 4),
                registeredMaxParallelUsages("-Pkotlin.native.compileMaxWorkers=1", "-Pkotlin.native.linkMaxWorkers=2"),
            )
        }
    }

    @DisplayName("Link tasks run in parallel up to the settings DSL limit, not the Gradle property limit")
    @GradleTest
    @OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
    fun testLinkTasksRespectSettingsDslLimitAtExecution(gradleVersion: GradleVersion) {
        val linkMaxWorkers = 2
        val buildOptions = defaultBuildOptions.copy(
            isolatedProjects = IsolatedProjectsMode.DISABLED,
            maxWorkers = linkMaxWorkers + 1,
        )
        project("empty", gradleVersion, buildOptions) {
            applySettingsDsl(linkMaxWorkers = linkMaxWorkers)
            plugins {
                kotlin("multiplatform")
            }
            val reportDir = projectPath.resolve("build/parallelismReports").toFile()
            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach {
                        it.binaries.framework { baseName = "libFramework" }
                    }
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
                project.registerParallelismProbeServiceIfAbsent<KotlinNativeLink>(reportDir, expectedRunning = linkMaxWorkers)
            }

            build("assemble", "-Pkotlin.native.linkMaxWorkers=1")

            assertEquals(linkMaxWorkers, reportDir.observedMaxParallelWorkers<KotlinNativeLink>())
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    private fun TestProject.applySettingsDsl(linkMaxWorkers: Int) {
        addEcosystemPluginToBuildScriptCompilationClasspath()
        settingsBuildScriptInjection {
            settings.plugins.apply("org.jetbrains.kotlin.ecosystem")
            settings.extensions.getByType(KotlinEcosystemExtension::class.java).native {
                this.linkMaxWorkers.set(linkMaxWorkers)
            }
        }
    }

    private fun TestProject.applyLinuxExecutable() {
        plugins {
            kotlin("multiplatform")
        }
        buildScriptInjection {
            project.applyMultiplatform {
                linuxX64 {
                    binaries.executable()
                }
            }
        }
    }

    private fun TestProject.registeredMaxParallelUsages(vararg gradleProperties: String): List<Int?> =
        buildScriptReturn {
            fun limit(taskType: Class<out Task>): Int? = project.gradle.sharedServices.registrations
                .findByName(parallelismLimitServiceName(taskType))
                ?.maxParallelUsages?.orNull
            listOf(limit(KotlinNativeCompile::class.java), limit(KotlinNativeLink::class.java))
        }.buildAndReturn("linkDebugExecutableLinuxX64", "--dry-run", *gradleProperties)
}

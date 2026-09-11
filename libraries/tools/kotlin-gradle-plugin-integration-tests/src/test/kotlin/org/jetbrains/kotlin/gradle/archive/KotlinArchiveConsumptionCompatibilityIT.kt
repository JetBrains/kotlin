/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.archive

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.PublishedProject
import org.jetbrains.kotlin.gradle.uklibs.addPublishedProjectToRepositories
import org.jetbrains.kotlin.gradle.uklibs.applyJvm
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.publish
import org.junit.jupiter.api.DisplayName

@DisplayName("Consumption of a project published in the Kotlin Archive format by the previous Kotlin release")
@MppGradlePluginTests
class KotlinArchiveConsumptionCompatibilityIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @GradleTest
    fun jvmProjectConsumptionTest(gradleVersion: GradleVersion) {
        val publishedProject = publishKotlinArchive(gradleVersion)

        compatibleKotlinConsumer(gradleVersion, publishedProject) {
            addSourceFile("main", consumerCallingJvmDeclarations)
            buildScriptInjection {
                project.applyJvm { }
                project.dependencies.add("implementation", publishedProject.rootCoordinate)
            }
        }.build("compileKotlin") {
            assertTasksExecuted(":compileKotlin")
        }
    }

    @GradleTest
    fun jvmOnlyMultiplatformProjectConsumptionTest(gradleVersion: GradleVersion) {
        val publishedProject = publishKotlinArchive(gradleVersion)

        compatibleKotlinConsumer(gradleVersion, publishedProject) {
            addSourceFile("jvmMain", consumerCallingJvmDeclarations)
            buildScriptInjection {
                project.applyMultiplatform {
                    jvm()

                    sourceSets.commonMain.dependencies {
                        implementation(publishedProject.rootCoordinate)
                    }
                }
            }
        }.build("compileKotlinJvm") {
            assertTasksExecuted(":compileKotlinJvm")
        }
    }

    @GradleTest
    fun otherTargetsConsumptionReportsUnsupportedArchiveTest(gradleVersion: GradleVersion) {
        val publishedProject = publishKotlinArchive(gradleVersion)

        val consumer = compatibleKotlinConsumer(gradleVersion, publishedProject) {
            addSourceFile("commonMain", "fun consume() {\n    commonMain()\n}")
            buildScriptInjection {
                project.applyMultiplatform {
                    js()
                    wasmJs()
                    linuxX64()

                    sourceSets.commonMain.dependencies {
                        implementation(publishedProject.rootCoordinate)
                    }
                }
            }
        }

        unsupportedCompilationTasks.forEach { compilationTask ->
            consumer.buildAndFail(compilationTask) {
                assertTasksFailed(compilationTask)
                // The severity can't be checked here: this diagnostic is reported with an id suffix,
                // and the assertion matches the id and the severity as a single string
                assertHasDiagnostic(KotlinToolingDiagnostics.UnsupportedKotlinArchiveUsage)
            }
        }
    }

    @GradleAndroidTest
    @AndroidGradlePluginTests
    @AndroidTestVersions(minVersion = TestVersions.AGP.MAX_SUPPORTED)
    fun jvmAndAndroidProjectConsumptionTest(
        gradleVersion: GradleVersion,
        androidVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk,
    ) {
        val publishedProject = publishKotlinArchive(gradleVersion)

        project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(
                kotlinVersion = TESTED_KOTLIN_VERSION,
                androidVersion = androidVersion,
                // `com.android.kotlin.multiplatform.library` requires the new AGP DSL
                enableLegacyAgpDsl = false,
            ),
            buildJdk = jdkVersion.location,
        ) {
            plugins {
                kotlin("multiplatform")
                id("com.android.kotlin.multiplatform.library")
            }
            addPublishedProjectToRepositories(publishedProject)
            settingsBuildScriptInjection {
                settings.rootProject.name = "consumer"
            }
            addSourceFile("jvmMain", consumerCallingJvmDeclarations)
            addSourceFile("androidMain", consumerCallingJvmDeclarations)
            buildScriptInjection {
                kotlinMultiplatform.apply {
                    jvm()
                    targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java).configureEach { target ->
                        target.compileSdk = 34
                        target.namespace = "com.example.consumer"
                    }

                    sourceSets.commonMain.dependencies {
                        implementation(publishedProject.rootCoordinate)
                    }
                }
            }
        }.build(":compileKotlinJvm", ":compileAndroidMain") {
            assertTasksExecuted(":compileKotlinJvm", ":compileAndroidMain")
        }
    }

    /**
     * The archive is published by the Kotlin version under test, and consumed by [TESTED_KOTLIN_VERSION].
     */
    private fun publishKotlinArchive(gradleVersion: GradleVersion): PublishedProject =
        kotlinArchiveProducer(gradleVersion).publish()

    private fun compatibleKotlinConsumer(
        gradleVersion: GradleVersion,
        publishedProject: PublishedProject,
        configure: TestProject.() -> Unit,
    ): TestProject = project(
        "empty",
        gradleVersion,
        buildOptions = defaultBuildOptions.copy(kotlinVersion = TESTED_KOTLIN_VERSION),
    ) {
        addKgpToBuildScriptCompilationClasspath()
        addPublishedProjectToRepositories(publishedProject)
        settingsBuildScriptInjection {
            settings.rootProject.name = "consumer"
        }
        configure()
    }

    companion object {
        // TODO: change to 2.4.20, once it is released
        private const val TESTED_KOTLIN_VERSION = "2.4.20-RC"

        private val consumerCallingJvmDeclarations = "fun consume() {\n    commonMain()\n    jvmMain()\n}"

        private val unsupportedCompilationTasks = listOf(
            ":compileKotlinJs",
            ":compileKotlinWasmJs",
            ":compileKotlinLinuxX64",
        )
    }
}

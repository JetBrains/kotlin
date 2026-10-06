/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.web.npm.KotlinSharedNpmProjectPlugin
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText

class ProjectIsolationIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.enableIsolatedProjects()

    @DisplayName("JVM project should be compatible with project isolation")
    @JvmGradlePluginTests
    @GradleTest
    fun testProjectIsolationInJvmSimple(gradleVersion: GradleVersion) {
        project(
            projectName = "instantExecution",
            gradleVersion = gradleVersion,
            // we can remove this line, when the min version of Gradle be at least 8.1
            dependencyManagement = DependencyManagement.DisabledDependencyManagement
        ) {
            build(":main-project:compileKotlin")
        }
    }

    @DisplayName("project with buildSrc should be compatible with project isolation")
    @JvmGradlePluginTests
    @GradleTestVersions
    @GradleTest
    fun testProjectIsolationWithBuildSrc(gradleVersion: GradleVersion) {
        project(
            projectName = "kt-63990-buildSrcWithKotlinJvmPlugin",
            gradleVersion = gradleVersion,
        ) {
            build("tasks")
        }
    }

    @DisplayName("Multi-module Android project")
    @GradleAndroidTest
    @AndroidGradlePluginTests
    fun testProjectIsolationAndroid(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk
    ) {
        project(
            projectName = "AndroidIncrementalMultiModule",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion).suppressAgpWarningIsProperty(gradleVersion),
            buildJdk = jdkVersion.location
        ) {
            build("assembleDebug")
        }
    }

    @DisplayName("Kapt multi-module project")
    @OtherGradlePluginTests
    @TestMetadata("kapt/javacIsLoadedOnce")
    @GradleTest
    fun testProjectIsolationKapt(
        gradleVersion: GradleVersion
    ) {
        project("kapt/javacIsLoadedOnce", gradleVersion) {
            build("assemble")
        }
    }

    @DisplayName("Kapt with Android multi-module project")
    @AndroidGradlePluginTests
    @AndroidTestVersions(maxVersion = TestVersions.AGP.AGP_813)
    @TestMetadata("kapt/android-databinding")
    @GradleAndroidTest
    fun testProjectIsolationAndroidWithKapt(
        gradleVersion: GradleVersion,
        agpVersion: String,
        jdkVersion: JdkVersions.ProvidedJdk
    ) {
        project(
            projectName = "kapt/android-databinding",
            gradleVersion = gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion).suppressAgpWarningIsProperty(gradleVersion),
            buildJdk = jdkVersion.location
        ) {
            gradleProperties.appendText(
                """
                |kotlin.jvm.target.validation.mode=warning
                """.trimMargin()
            )

            build("assembleDebug")
        }
    }

    @DisplayName("Shared npm project should be compatible with project isolation")
    @JsGradlePluginTests
    @GradleTest
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    fun testProjectIsolationSharedNpmProject(gradleVersion: GradleVersion) {
        project("emptyKts", gradleVersion) {
            plugins {
                kotlin("multiplatform").apply(false)
            }
            buildScriptInjection {
                project.plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
                project.dependencies.add("kotlinNpmSharedDependencies", project.dependencies.project(mapOf("path" to ":lib-a")))
                project.dependencies.add("kotlinNpmSharedDependencies", project.dependencies.project(mapOf("path" to ":lib-b")))
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib-a") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        js { nodejs() }
                        sourceSets.getByName("jsMain").dependencies {
                            npm("is-even", "1.0.0")
                        }
                    }
                }
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib-b") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        js { nodejs() }
                        sourceSets.getByName("jsMain").dependencies {
                            implementation(project(":lib-a"))
                        }
                    }
                }
            }

            build("kotlinSetupSharedNpmProject") {
                assertTasksExecuted(
                    ":lib-a:kotlinSharedPackageJson",
                    ":lib-b:kotlinSharedPackageJson",
                    ":kotlinSetupSharedNpmProject",
                )
                assertFileExists(projectPath.resolve("build/js/shared-npm-project/package.json"))
            }

            build("kotlinSetupSharedNpmProject") {
                assertConfigurationCacheReused()
            }
        }
    }

    @DisplayName("Shared WasmJs npm project should be compatible with project isolation")
    @JsGradlePluginTests
    @GradleTest
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    fun testProjectIsolationWasmSharedNpmProject(gradleVersion: GradleVersion) {
        project("emptyKts", gradleVersion) {
            plugins {
                kotlin("multiplatform").apply(false)
            }
            buildScriptInjection {
                project.plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
                project.dependencies.add("kotlinWasmNpmSharedDependencies", project.dependencies.project(mapOf("path" to ":lib-a")))
                project.dependencies.add("kotlinWasmNpmSharedDependencies", project.dependencies.project(mapOf("path" to ":lib-b")))
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib-a") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        wasmJs { nodejs() }
                        sourceSets.getByName("wasmJsMain").dependencies {
                            npm("is-even", "1.0.0")
                        }
                    }
                }
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib-b") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        wasmJs { nodejs() }
                        sourceSets.getByName("wasmJsMain").dependencies {
                            implementation(project(":lib-a"))
                        }
                    }
                }
            }

            build("kotlinWasmSetupSharedNpmProject") {
                assertTasksExecuted(
                    ":lib-a:kotlinWasmSharedPackageJson",
                    ":lib-b:kotlinWasmSharedPackageJson",
                    ":kotlinWasmSetupSharedNpmProject",
                )
                assertFileExists(projectPath.resolve("build/wasm/shared-npm-project/package.json"))
            }

            build("kotlinWasmSetupSharedNpmProject") {
                assertConfigurationCacheReused()
            }
        }
    }
}

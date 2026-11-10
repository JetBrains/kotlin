/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import org.jetbrains.kotlin.gradle.targets.web.npm.KotlinSharedNpmProjectPlugin
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText
import kotlin.test.assertEquals

@MustRunOnChangesInGradle
class ProjectIsolationIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.enableIsolatedProjects()

    @TestMetadata("instantExecution")
    @DisplayName("JVM project should be compatible with project isolation")
    @JvmGradlePluginTests
    @GradleTest
    fun testProjectIsolationInJvmSimple(gradleVersion: GradleVersion) {
        project(
            projectName = "instantExecution",
            gradleVersion = gradleVersion,
        ) {
            build(":main-project:compileKotlin")
        }
    }

    @TestMetadata("kt-63990-buildSrcWithKotlinJvmPlugin")
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

    @TestMetadata("AndroidIncrementalMultiModule")
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
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
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
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
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

    @DisplayName("Shared JS and WasmJs npm projects should be compatible with project isolation")
    @JsGradlePluginTests
    @GradleTest
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    fun testProjectIsolationSharedNpmProject(gradleVersion: GradleVersion) {
        project("emptyKts", gradleVersion) {
            gradleProperties.appendText(
                """
                |
                |kotlin.internal.npm.sharedNpmDependenciesProjectMode=PER-PROJECT
                """.trimMargin()
            )
            plugins {
                kotlin("multiplatform").apply(false)
            }
            buildScriptInjection {
                project.plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
                for (configuration in listOf("kotlinNpmSharedDependencies", "kotlinWasmNpmSharedDependencies")) {
                    project.dependencies.add(configuration, project.dependencies.project(mapOf("path" to ":lib-a")))
                    project.dependencies.add(configuration, project.dependencies.project(mapOf("path" to ":lib-b")))
                }
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "lib-a") {
                buildScriptInjection {
                    project.plugins.apply("org.jetbrains.kotlin.multiplatform")
                    kotlinMultiplatform.apply {
                        js { nodejs() }
                        wasmJs { nodejs() }
                        sourceSets.getByName("commonMain").dependencies {
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
                        wasmJs { nodejs() }
                        sourceSets.getByName("commonMain").dependencies {
                            implementation(project(":lib-a"))
                        }
                    }
                }
            }

            build("kotlinSetupSharedNpmProject", "kotlinWasmSetupSharedNpmProject") {
                assertTasksExecuted(
                    ":lib-a:jsNpmDependenciesReport",
                    ":lib-a:jsTestNpmDependenciesReport",
                    ":lib-b:jsNpmDependenciesReport",
                    ":lib-b:jsTestNpmDependenciesReport",
                    ":lib-a:wasmJsNpmDependenciesReport",
                    ":lib-a:wasmJsTestNpmDependenciesReport",
                    ":lib-b:wasmJsNpmDependenciesReport",
                    ":lib-b:wasmJsTestNpmDependenciesReport",
                )
                for (platform in listOf("js", "wasm")) {
                    val libB = projectPath.resolve("build/$platform/shared-npm-project/packages/emptyKts-lib-b/package.json").toFile()
                    assertEquals("1.0.0", fromSrcPackageJson(libB)?.dependencies?.get("is-even"), "is-even from :lib-a in $libB")
                }
            }

            build("kotlinSetupSharedNpmProject", "kotlinWasmSetupSharedNpmProject") {
                assertConfigurationCacheReused()
            }
        }
    }
}

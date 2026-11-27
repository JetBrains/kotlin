/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Action
import org.gradle.api.initialization.resolve.RepositoriesMode
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.PreInstalledNodeJsToolchainService
import org.jetbrains.kotlin.gradle.ecosystem.KotlinEcosystemExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.kotlin.testFederation.MustRunOnChangesInGradle
import org.junit.jupiter.api.DisplayName
import java.io.File
import kotlin.io.path.appendText


@MppGradlePluginTests
@MustRunOnChangesInGradle
class NodeJsGradlePluginIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        // KT-75899 Support Gradle Project Isolation in KGP JS & Wasm
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @DisplayName("Set different Node.js versions in different subprojects")
    @GradleTest
    @TestMetadata("subprojects-nodejs-setup")
    fun testDifferentVersionInSubprojects(gradleVersion: GradleVersion) {
        project(
            "subprojects-nodejs-setup",
            gradleVersion
        ) {
            build(":app1:jsNodeDevelopmentRun") {
                assertOutputContains("Hello with version: v22.2.0")
            }

            build(":app2:jsNodeDevelopmentRun") {
                assertOutputContains("Hello with version: v22.1.0")
            }
        }
    }

    @DisplayName("Check that changing granularity doesn't break incremental compilation")
    @GradleTest
    @TestMetadata("subprojects-nodejs-setup")
    fun testIncrementalCompilationWithChangingGranularity(gradleVersion: GradleVersion) {
        project("subprojects-nodejs-setup", gradleVersion) {
            gradleProperties.appendText(
                """
                |
                |kotlin.incremental.js.ir=true
                |kotlin.js.ir.output.granularity=whole-program
                """.trimMargin()
            )

            build(":app1:jsNodeDevelopmentRun") {
                assertOutputContains("Hello with version: v22.2.0")
            }

            gradleProperties.appendText(
                """
                |
                |kotlin.js.ir.output.granularity=per-file
                """.trimMargin()
            )

            build(":app1:jsNodeDevelopmentRun") {
                assertOutputContains("Hello with version: v22.2.0")
            }
        }
    }

    @DisplayName("Set different Node.js versions in root project and subprojects")
    @GradleTest
    @TestMetadata("subprojects-nodejs-setup")
    fun testDifferentVersionInRootProjectAndSubprojects(gradleVersion: GradleVersion) {
        project(
            "subprojects-nodejs-setup",
            gradleVersion
        ) {
            @Suppress("DEPRECATION", "UPPER_BOUND_VIOLATED_BASED_ON_JAVA_ANNOTATIONS")
            buildScriptInjection {
                project.rootProject.plugins.withType(NodeJsPlugin::class.java, Action {
                    project.rootProject.extensions.getByType(NodeJsEnvSpec::class.java).version.set("22.3.0")
                })
            }

            build(":app1:jsNodeDevelopmentRun") {
                assertOutputContains("Hello with version: v22.2.0")
            }

            build(":app2:jsNodeDevelopmentRun") {
                assertOutputContains("Hello with version: v22.1.0")
            }
        }
    }

    @DisplayName("Node.js setup test with downloadBaseUrl = null")
    @GradleTest
    @TestMetadata("nodejs-setup-with-user-repositories")
    fun testSetupWithoutDownloadBaseUrl(gradleVersion: GradleVersion) {
        project("nodejs-setup-with-user-repositories", gradleVersion) {
            build(":kotlinNodeJsSetup")
        }
    }

    @DisplayName("KT-55620: Node.js toolchain with FAIL_ON_PROJECT_REPOS")
    @GradleTest
    @OptIn(ExperimentalNodeJsToolchainDsl::class)
    fun testNodeToolchainWithFailOnProjectRepos(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            addEcosystemPluginToBuildScriptCompilationClasspath()
            settingsBuildScriptInjection {
                settings.dependencyResolutionManagement {
                    it.repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                }
            }
            settingsBuildScriptInjection {
                settings.plugins.apply("org.jetbrains.kotlin.ecosystem")
                settings.extensions.getByType(KotlinEcosystemExtension::class.java).toolchainManagement {
                    nodeJs {
                        toolchainService(PreInstalledNodeJsToolchainService::class) {
                            nodeJsExecutable.set("node")
                        }
                    }
                }
            }

            buildScriptInjection {
                project.applyMultiplatform {
                    js {
                        browser()
                        nodejs()
                    }
                }
            }

            // The old setup must not add a project repository when a toolchain service provisions Node.js
            build(":kotlinNodeJsSetup") {
                assertTasksSkipped(":kotlinNodeJsSetup")
            }
        }
    }

    @DisplayName("KT-91711: npm install with the downloading Node.js toolchain does not need npm on PATH")
    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.MAX_SUPPORTED)
    fun testNpmInstallWithDownloadingNodeJsToolchainWithoutHostNpm(gradleVersion: GradleVersion) {
        testPackageManagerInstallWithDownloadingNodeJsToolchainWithoutHostNodeJs(gradleVersion, useYarn = false)
    }

    @DisplayName("KT-91711: yarn install with the downloading Node.js toolchain does not need Node.js on PATH")
    @GradleTest
    @GradleTestVersions(minVersion = TestVersions.Gradle.MAX_SUPPORTED)
    fun testYarnInstallWithDownloadingNodeJsToolchainWithoutHostNodeJs(gradleVersion: GradleVersion) {
        testPackageManagerInstallWithDownloadingNodeJsToolchainWithoutHostNodeJs(gradleVersion, useYarn = true)
    }

    @OptIn(EnvironmentalVariablesOverride::class)
    private fun testPackageManagerInstallWithDownloadingNodeJsToolchainWithoutHostNodeJs(
        gradleVersion: GradleVersion,
        useYarn: Boolean,
    ) {
        project(
            "empty",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(jsOptions = defaultBuildOptions.jsOptions?.copy(yarn = useYarn)),
        ) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    js { nodejs() }
                    sourceSets.jsMain.dependencies { implementation(npm("is-number", "7.0.0")) }
                }
            }

            build(
                ":kotlinNpmInstall",
                "-Pkotlin.js.nodeJsToolchainMode=download",
                environmentVariables = EnvironmentalVariables { it + ("PATH" to pathWithoutNodeJs(it)) },
            ) {
                assertTasksSkipped(":kotlinNodeJsSetup")
                assertTasksExecuted(":kotlinNpmInstall")
            }
        }
    }
}

private fun pathWithoutNodeJs(environment: Map<String, String>): String =
    environment.getValue("PATH").split(File.pathSeparator)
        .filterNot { dir -> listOf("node", "node.exe", "npm", "npm.cmd").any { File(dir, it).exists() } }
        .joinToString(File.pathSeparator)

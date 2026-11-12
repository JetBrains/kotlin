/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Action
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.dsl.toolchain.nodejs.PreInstalledNodeJsToolchainService
import org.jetbrains.kotlin.gradle.ecosystem.KotlinEcosystemExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText


@MppGradlePluginTests
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
    @TestMetadata("nodejs-setup-default-repositories-mode")
    @OptIn(ExperimentalNodeJsToolchainDsl::class)
    fun testNodeToolchainWithFailOnProjectRepos(gradleVersion: GradleVersion) {
        project("nodejs-setup-default-repositories-mode", gradleVersion) {
            addEcosystemPluginToBuildScriptCompilationClasspath()
            settingsGradleKts.appendText(
                """
                |
                |dependencyResolutionManagement {
                |    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                |}
                """.trimMargin()
            )
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
            // The old setup must not add a project repository when a toolchain service provisions Node.js
            build(":kotlinNodeJsSetup") {
                assertTasksSkipped(":kotlinNodeJsSetup")
            }
        }
    }
}

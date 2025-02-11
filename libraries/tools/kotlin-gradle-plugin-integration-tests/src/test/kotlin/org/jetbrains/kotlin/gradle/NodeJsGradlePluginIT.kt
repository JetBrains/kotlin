/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Action
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.appendText


@MppGradlePluginTests
class NodeJsGradlePluginIT : KGPBaseTest() {
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

    @DisplayName("Set different Node.js versions in different subprojects configured with previous API")
    @GradleTest
    @TestMetadata("subprojects-nodejs-setup")
    fun testDifferentVersionInSubprojectsWithPreviousApi(gradleVersion: GradleVersion) {
        project(
            "subprojects-nodejs-setup",
            gradleVersion
        ) {
            listOf("app1", "app2").forEach { subProjectName ->
                subProject(subProjectName).buildGradleKts.modify {
                    it.replace("plugins.", "@Suppress(\"DEPRECATION_ERROR\") rootProject.plugins.")
                        .replace("the", "rootProject.the")
                        .replace("NodeJsPlugin", "NodeJsRootPlugin")
                        .replace("NodeJsEnvSpec", "NodeJsRootExtension")
                        .replace("""version\.set\(("\d+\.\d+.\d+")\)""".toRegex(), "version = \"22.3.0\"")
                }
            }

            build(":app1:jsNodeDevelopmentRun") {
                assertOutputContains("Hello with version: v22.3.0")
            }

            build(":app2:jsNodeDevelopmentRun") {
                assertOutputContains("Hello with version: v22.3.0")
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
            @Suppress("DEPRECATION")
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
    @TestMetadata("subprojects-nodejs-setup")
    fun testSetupWithoutDownloadBaseUrl(gradleVersion: GradleVersion) {
        project(
            "nodejs-setup-with-user-repositories",
            gradleVersion,
            // we can remove this line, when the min version of Gradle be at least 8.1
            dependencyManagement = DependencyManagement.DisabledDependencyManagement
        ) {
            build(":kotlinNodeJsSetup")
        }
    }
}

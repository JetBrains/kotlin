/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.Action
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName


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
                    it.replace("plugins.", "rootProject.plugins.")
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
                project.rootProject.plugins.withType(NodeJsRootPlugin::class.java, Action {
                    project.rootProject.extensions.getByType(NodeJsRootExtension::class.java).version = "22.3.0"
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
}

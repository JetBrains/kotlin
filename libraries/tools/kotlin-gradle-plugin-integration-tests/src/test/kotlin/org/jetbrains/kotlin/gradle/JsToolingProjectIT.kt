/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.targets.js.internal.jsToolingProject
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals

@JsGradlePluginTests
@GradleTestVersions(
    // Temp test until KT-80311. Only check the latest Gradle for faster CI and simpler tests.
    minVersion = TestVersions.Gradle.MAX_SUPPORTED
)
class JsToolingProjectIT : KGPBaseTest() {

    @DisplayName("KT-89532: using jsToolingProject() in a subproject is IP-safe")
    @GradleTest
    fun testResolveRootToolingProjectFromSubproject(gradleVersion: GradleVersion) {

        val jsToolingProjectMarker = "[jsToolingProject]"

        project("emptyKts", gradleVersion) {
            plugins {
                kotlin("multiplatform").apply(false)
            }

            includeOtherProjectAsSubmodule("emptyKts", newSubmoduleName = "sub") {
                buildScriptInjection {
                    val toolingProject = project.jsToolingProject()
                    val rootToolingProject = toolingProject.jsToolingProject()
                    println(
                        """
                        $jsToolingProjectMarker current = ${toolingProject.isolated.path}
                        $jsToolingProjectMarker root = ${rootToolingProject.isolated.path}
                        """.trimIndent()
                    )
                }
            }

            build("help") {
                assertConfigurationCacheStored()
                assertEquals(
                    """
                        $jsToolingProjectMarker current = :
                        $jsToolingProjectMarker root = :
                        """.trimIndent(),
                    outputReader.lineSequence().filter { it.startsWith(jsToolingProjectMarker) }.joinToString("\n")
                )
            }
        }
    }
}

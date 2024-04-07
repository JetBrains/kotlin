/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compose.compiler.gradle

import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import org.junit.jupiter.api.Test
import org.jetbrains.kotlin.compose.compiler.gradle.testUtils.buildProjectWithJvm
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExtensionConfigurationTest {
    @Test
    fun testDefaultExtensionConfiguration() {
        testComposeOptions { options, _ ->
            assertEquals(
                listOf(
                    "generateFunctionKeyMetaClasses" to "false",
                    "sourceInformation" to "false",
                    "intrinsicRemember" to "false",
                    "nonSkippingGroupOptimization" to "false",
                    "experimentalStrongSkipping" to "false",
                    "traceMarkersEnabled" to "false",
                ),
                options
            )
        }
    }

    @Test
    fun testIncludeMetricsDestination() {
        testComposeOptions(
            { extension, project ->
                extension.metricsDestination.value(
                    project.layout.buildDirectory.dir("composeMetrics")
                )
            }
        ) { options, project ->
            assertTrue(
                options.contains(
                    "metricsDestination" to project.layout.buildDirectory.dir("composeMetrics").get().asFile.path
                )
            )
        }
    }

    @Test
    fun testIncludeReportsDestination() {
        testComposeOptions(
            { extension, project ->
                extension.reportsDestination.value(
                    project.layout.buildDirectory.dir("reportsMetrics")
                )
            }
        ) { options, project ->
            assertTrue(
                options.contains(
                    "reportsDestination" to project.layout.buildDirectory.dir("reportsMetrics").get().asFile.path
                )
            )
        }
    }

    @Test
    fun testStabilityConfigurationFile() {
        testComposeOptions(
            { extension, project ->
                extension.stabilityConfigurationFile.value(
                    project.layout.projectDirectory.file("compose.conf")
                )
            }
        ) { options, project ->
            assertTrue(
                options.contains(
                    "stabilityConfigurationPath" to project.layout.projectDirectory.file("compose.conf").asFile.path
                )
            )
        }
    }

    @Test
    fun testSuppressKotlinVersionCompatibilityCheck() {
        testComposeOptions(
            { extension, _ ->
                extension.suppressKotlinVersionCompatibilityCheck.value("2.0.5")
            }
        ) { options, _ ->
            assertTrue(
                options.contains("suppressKotlinVersionCompatibilityCheck" to "2.0.5")
            )
        }
    }

    @Test
    fun notIncludeSourceInformationOnAgpPresence() {
        testComposeOptions(
            { _, project ->
                project.simulateAgpPresence()
            }
        ) { options, _ ->
            assertFalse(
                options.map { it.first }.contains("sourceInformation")
            )
        }
    }

    private fun testComposeOptions(
        configureExtension: (ComposeCompilerGradlePluginExtension, Project) -> Unit = { _, _ -> },
        assertions: (List<Pair<String, String>>, Project) -> Unit
    ) {
        val project = buildProjectWithJvm {
            val composeExtension = extensions.getByType<ComposeCompilerGradlePluginExtension>()
            configureExtension(composeExtension, this)
        }

        project.evaluate()

        val jvmTask = project.tasks.named<KotlinJvmCompile>("compileKotlin").get()
        val composeOptions = jvmTask.pluginOptions.get()
            .flatMap { compilerPluginConfig ->
                compilerPluginConfig.allOptions().filter { it.key == "androidx.compose.compiler.plugins.kotlin" }.values
            }
            .flatten()
            .map { it.key to it.value }

        assertions(composeOptions, project)
    }

    fun Project.simulateAgpPresence() = configurations.resolvable("kotlin-extension")
}
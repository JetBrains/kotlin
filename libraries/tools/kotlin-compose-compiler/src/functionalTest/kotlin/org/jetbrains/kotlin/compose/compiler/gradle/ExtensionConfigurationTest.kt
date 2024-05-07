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
import org.jetbrains.kotlin.compose.compiler.gradle.testUtils.composeOptions
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

    @Test
    fun disableIntrinsicRemember() {
        testComposeFeatureFlags(listOf("-IntrinsicRemember")) { extension ->
            extension.featureFlags.value(setOf(ComposeFeatureFlag.IntrinsicRemember.disable()))
        }
    }

    @Test
    fun enableStrongSkipping() {
        testComposeFeatureFlags(listOf("StrongSkipping")) { extension ->
            extension.featureFlags.value(setOf(ComposeFeatureFlag.StrongSkipping))
        }
    }

    @Test
    fun enableNonSkippingGroupOptimization() {
        testComposeFeatureFlags(listOf("OptimizeNonSkippingGroups")) { extension ->
            extension.featureFlags.value(setOf(ComposeFeatureFlag.OptimizeNonSkippingGroups))
        }
    }

    @Test
    fun disableIntrinsicRememberCompatibility() {
        testComposeFeatureFlags(listOf("-IntrinsicRemember")) { extension ->
            @Suppress("DEPRECATION")
            extension.enableIntrinsicRemember.value(false)
        }
    }

    @Test
    fun enableStrongSkippingCompatibility() {
        testComposeFeatureFlags(listOf("StrongSkipping")) { extension ->
            @Suppress("DEPRECATION")
            extension.enableStrongSkippingMode.value(true)
        }
    }

    @Test
    fun enableNonSkippingGroupOptimizationCompatibility() {
        testComposeFeatureFlags(listOf("OptimizeNonSkippingGroups")) { extension ->
            @Suppress("DEPRECATION")
            extension.enableNonSkippingGroupOptimization.value(true)
        }
    }

    @Test
    fun enableMultipleFlags() {
        testComposeFeatureFlags(listOf("OptimizeNonSkippingGroups", "StrongSkipping", "-IntrinsicRemember")) { extension ->
            extension.featureFlags.set(
                setOf(
                    ComposeFeatureFlag.StrongSkipping,
                    ComposeFeatureFlag.IntrinsicRemember.disable(),
                    ComposeFeatureFlag.OptimizeNonSkippingGroups
                )
            )
        }
    }

    @Test
    fun enableMultipleFlagsCompatibility() {
        @Suppress("DEPRECATION")
        testComposeFeatureFlags(listOf("OptimizeNonSkippingGroups", "StrongSkipping", "-IntrinsicRemember")) { extension ->
            extension.enableStrongSkippingMode.value(true)
            extension.enableNonSkippingGroupOptimization.value(true)
            extension.enableIntrinsicRemember.value(false)
        }
    }

    private fun testComposeFeatureFlags(flags: List<String>, configure: (ComposeCompilerGradlePluginExtension) -> Unit) {
        testComposeOptions({ extension, _ -> configure(extension) }) { options, _ ->
            for (flag in flags) options.assertFeature(flag)
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
        val composeOptions = jvmTask.composeOptions()

        assertions(composeOptions, project)
    }

    private fun Project.simulateAgpPresence() = configurations.resolvable("kotlin-extension")
}

fun List<Pair<String, String>>.assertFeature(featureName: String) =
    assertTrue(
        firstOrNull { it.first == "featureFlag" && it.second == featureName } != null,
        "Expected a feature flag $featureName to be set in $this")

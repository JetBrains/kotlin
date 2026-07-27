/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.android.externalAndroidTarget

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.setAndroidSdkDirProperty
import org.jetbrains.kotlin.gradle.utils.named
import kotlin.test.Test
import kotlin.test.assertEquals

class ComposeCompilerPluginExternalAndroidTargetTest {

    @Test
    fun `compose compiler flags are passed to androidLibrary compiler arguments`() {
        val project = externalAndroidLibraryProject(
            composeConfiguration = { composeCompiler ->
                composeCompiler.includeSourceInformation.set(false)
                composeCompiler.metricsDestination.set(project.layout.buildDirectory.dir("compose-metrics"))
                composeCompiler.reportsDestination.set(project.layout.buildDirectory.dir("compose-reports"))
                @Suppress("DEPRECATION")
                composeCompiler.featureFlags.set(setOf(ComposeFeatureFlag.OptimizeNonSkippingGroups))
            },
        )
        project.evaluate()

        val composeOptions = project.composeCompilerOptions("compileAndroidMain")

        assertEquals(
            listOf(
                "sourceInformation" to "false",
                "metricsDestination" to project.layout.buildDirectory.dir("compose-metrics").get().asFile
                    .resolve("android")
                    .resolve("main")
                    .path,
                "reportsDestination" to project.layout.buildDirectory.dir("compose-reports").get().asFile.path,
                "traceMarkersEnabled" to "true",
                "featureFlag" to "OptimizeNonSkippingGroups",
            ).prettyPrinted,
            composeOptions.prettyPrinted,
            "Unexpected Compose compiler options",
        )
    }

    private fun externalAndroidLibraryProject(
        kotlinConfiguration: KotlinMultiplatformExtension.() -> Unit = {},
        composeConfiguration: Project.(ComposeCompilerGradlePluginExtension) -> Unit = {},
    ): ProjectInternal = buildProject {
        setAndroidSdkDirProperty(project)
        plugins.apply("kotlin-multiplatform")
        plugins.apply("com.android.kotlin.multiplatform.library")
        plugins.apply("org.jetbrains.kotlin.plugin.compose")
        kotlin {
            targets.withType(KotlinMultiplatformAndroidLibraryTarget::class.java).configureEach { target ->
                target.compileSdk = 34
                target.namespace = "org.jetbrains.sample.compose"
            }
            kotlinConfiguration()
        }
        composeConfiguration(project.extensions.getByType())
    }

    private fun Project.composeCompilerOptions(taskName: String): List<Pair<String, String>> {
        val task = tasks.named<KotlinCompile>(taskName).get()
        return task.pluginOptions.get()
            .flatMap { compilerPluginConfig ->
                compilerPluginConfig.allOptions()
                    .filter { it.key == composeCompilerPluginId }
                    .values
            }
            .flatten()
            .map { it.key to it.value }
    }

    private companion object {
        const val composeCompilerPluginId = "androidx.compose.compiler.plugins.kotlin"
    }
}

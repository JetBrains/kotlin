/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.dependencyResolutionTests

import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.assertMatches
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.binaryCoordinates
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.util.applyMultiplatformPlugin
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.enableDefaultStdlibDependency
import org.jetbrains.kotlin.gradle.util.enableDependencyVerification
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ResolvableMetadataConfigurationTest {

    @Test
    fun `test - resolves consistent in project`() {
        val project = buildProject {
            enableDefaultStdlibDependency(false)
            enableDependencyVerification(false)
            applyMultiplatformPlugin()
            repositories.mavenCentralCacheRedirector()
        }

        val kotlin = project.multiplatformExtension

        /* Define simple targets */
        kotlin.jvm()
        kotlin.linuxArm64()
        kotlin.linuxX64()
        kotlin.targetHierarchy.default()

        val commonMain = kotlin.sourceSets.getByName("commonMain")
        val nativeMain = kotlin.sourceSets.getByName("nativeMain")

        commonMain.dependencies {
            implementation("com.squareup.okio:okio:3.2.0")
        }

        /* nativeMain explicitly using 3.3.0 (higher than 3.2.0 in commonMain) */
        nativeMain.dependencies {
            implementation("com.squareup.okio:okio:3.3.0")
            implementation("com.arkivanov.mvikotlin:mvikotlin:3.0.2")
        }

        project.evaluate()

        /* Check by resolving the 'resolvableMetadataConfigurations' directly */
        kotlin.sourceSets.forEach { sourceSet ->
            sourceSet.internal.resolvableMetadataConfiguration.incoming.resolutionResult.allDependencies
                .mapNotNull { result -> if (result is ResolvedDependencyResult) result.selected.id else null }
                .filterIsInstance<ModuleComponentIdentifier>()
                .filter { id -> id.group == "com.squareup.okio" }
                .ifEmpty { fail("Expected at least one okio dependency resolved") }
                .forEach { resolvedId ->
                    assertEquals(
                        "3.3.0", resolvedId.version,
                        "SourceSet: ${sourceSet.name} resolved $resolvedId, but expected consistent version 3.3.0"
                    )
                }
        }

        /* Check IDE resolution for commonMain */
        project.kotlinIdeMultiplatformImport.resolveDependencies("commonMain")
            .assertMatches(
                binaryCoordinates(Regex("com.squareup.okio:okio(-.*)?:3.3.0:.*")),
                binaryCoordinates(Regex("org.jetbrains.kotlin.*"))
            )

        /* Check IDE resolution for nativeMain */
        project.kotlinIdeMultiplatformImport.resolveDependencies("nativeMain")
            .filterIsInstance<IdeaKotlinBinaryDependency>()
            .filter { it.coordinates?.group.orEmpty() in setOf("com.squareup.okio", "com.arkivanov.mvikotlin") }
            .assertMatches(
                binaryCoordinates(Regex("com.squareup.okio:okio(-.*)?:3.3.0:.*")),
                binaryCoordinates(Regex("com.arkivanov.mvikotlin:mvikotlin(-*)?:3.0.2:.*")),
            )
    }
}

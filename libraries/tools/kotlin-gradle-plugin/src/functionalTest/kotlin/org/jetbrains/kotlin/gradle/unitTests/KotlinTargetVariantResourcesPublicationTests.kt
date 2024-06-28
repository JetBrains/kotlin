/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.resourcesPublicationExtension
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.enableMppResourcesPublication
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KotlinTargetVariantResourcesPublicationTests {

    @Test
    fun `test variant publication - doesn't happen - when resources publication is disabled`() {
        val enableResourcePublication = false
        testPublishedVariants(
            enableResourcePublication = enableResourcePublication,
            afterEvaluation = { target ->
                publishFakeResources(target)
            },
            shouldPublishVariant = enableResourcePublication,
            targetsToTest = listOf(
                { linuxX64() },
                { wasmWasi() },
                { wasmJs() },
                { js() },
            )
        )
    }

    @Test
    fun `test variant publication - doesn't happen - when publishing api is not called`() {
        testPublishedVariants(
            enableResourcePublication = true,
            afterEvaluation = { /* don't call publishing api */ },
            shouldPublishVariant = false,
            targetsToTest = listOf(
                { linuxX64() },
                { wasmWasi() },
                { wasmJs() },
                { js() },
            )
        )
    }

    @Test
    fun `test variant publication - creates a variant - when publishing api is called`() {
        testPublishedVariants(
            enableResourcePublication = true,
            afterEvaluation = { target ->
                publishFakeResources(target)
            },
            shouldPublishVariant = true,
            targetsToTest = listOf(
                { linuxX64() },
                { wasmWasi() },
                { wasmJs() },
                { js() },
            )
        )
    }

    @Test
    fun `test resources configuration - outputs a single zip file - in outgoing artifacts`() {
        val project = buildProjectWithMPP {
            kotlin {
                linuxX64()
            }
            enableMppResourcesPublication(true)
        }.evaluate()

        assertNull(
            project.configurations.findByName(
                project.multiplatformExtension.linuxX64().resourcesElementsConfigurationName
            )
        )

        project.publishFakeResources(project.multiplatformExtension.linuxX64())

        assertEquals(
            listOf(
                project.layout.buildDirectory.file(
                    "kotlin-multiplatform-resources/zip-for-publication/linuxX64/test.kotlin_resources.zip"
                ).get().asFile
            ),
            project.configurations.findByName(
                project.multiplatformExtension.linuxX64().resourcesElementsConfigurationName
            )?.outgoing?.artifacts?.map { it.file },
        )
    }

    private fun testPublishedVariants(
        enableResourcePublication: Boolean,
        afterEvaluation: Project.(KotlinTarget) -> Unit,
        shouldPublishVariant: Boolean,
        targetsToTest: List<KotlinMultiplatformExtension.() -> KotlinTarget>,
    ) {
        val project = buildProjectWithMPP(
            preApplyCode = {
                enableMppResourcesPublication(enableResourcePublication)
            }
        ) {
            kotlin {
                targetsToTest.forEach { createTarget ->
                    createTarget()
                }
            }
        }.evaluate()

        targetsToTest.forEach { targetToTest ->
            val target = project.multiplatformExtension.targetToTest()

            // Resources configuration should never be published before publishing API is called
            assert(target.internal.resourcesElementsConfigurationName !in configurationNamesUsedInVariantPublications(target))
            project.afterEvaluation(target)
            assert(
                shouldPublishVariant == target.internal.resourcesElementsConfigurationName in configurationNamesUsedInVariantPublications(target)
            )
        }
    }

    private fun configurationNamesUsedInVariantPublications(target: KotlinTarget): Set<String> {
        return target.internal.kotlinComponents.flatMap { component ->
            component.internal.usages.map { it.dependencyConfigurationName }
        }.toSet()
    }

    private fun Project.publishFakeResources(target: KotlinTarget) {
        project.multiplatformExtension.resourcesPublicationExtension?.publishResourcesAsKotlinComponent(
            target,
            resourcePathForSourceSet = {
                KotlinTargetResourcesPublication.ResourceRoot(
                    project.provider { File(it.name) },
                    emptyList(),
                    emptyList(),
                )
            },
            relativeResourcePlacement = project.provider { File("test") },
        )
    }

}
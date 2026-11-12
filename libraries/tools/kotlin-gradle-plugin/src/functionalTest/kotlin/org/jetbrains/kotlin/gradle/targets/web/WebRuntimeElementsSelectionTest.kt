/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web

import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging
import org.jetbrains.kotlin.gradle.targets.js.ir.KLIB_TYPE
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class WebRuntimeElementsSelectionTest {

    @Test
    fun `selecting runtimeElements by name and packed klib packaging resolves the klib`() {
        assertSelectingRuntimeElementsByNameResolvesTheKlib {
            js { nodejs() }
            wasmJs { nodejs() }
        }
    }

    private fun assertSelectingRuntimeElementsByNameResolvesTheKlib(configureTargets: KotlinMultiplatformExtension.() -> Unit) {
        val root = buildProject(projectBuilder = { withName("root") })
        val lib = buildProjectWithMPP(projectBuilder = { withParent(root).withName("lib") }) {
            kotlin(configureTargets)
        }
        val consumer = buildProject(projectBuilder = { withParent(root).withName("consumer") })
        lib.evaluate()

        lib.multiplatformExtension.targets
            .filter { it.platformType != KotlinPlatformType.common }
            .forEach { target ->
                val dependency = consumer.dependencies.project(
                    mapOf("path" to ":lib", "configuration" to target.runtimeElementsConfigurationName)
                )
                val configuration = consumer.configurations.detachedConfiguration(dependency).apply {
                    isTransitive = false
                    attributes.attribute(KlibPackaging.ATTRIBUTE, consumer.objects.named(KlibPackaging::class.java, KlibPackaging.PACKED))
                }

                assertEquals(
                    listOf(KLIB_TYPE),
                    configuration.incoming.artifacts.artifacts.map { it.variant.attributes.getAttribute(ARTIFACT_TYPE_ATTRIBUTE) },
                    target.runtimeElementsConfigurationName,
                )
            }
    }
}

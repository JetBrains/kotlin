/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle

import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ConfigurationsTest : MultiplatformExtensionTest() {

    @Test
    fun `consumable configurations with platform target are marked with Category LIBRARY`() {
        kotlin.linuxX64()
        kotlin.iosX64()
        kotlin.iosArm64()
        kotlin.jvm()
        kotlin.js()

        val nativeMain = kotlin.sourceSets.create("nativeMain")
        kotlin.targets.withType(KotlinNativeTarget::class.java).all { target ->
            target.compilations.getByName("main").defaultSourceSet.dependsOn(nativeMain)
        }

        project.evaluate()

        project.configurations
            .filter { configuration ->
                configuration.attributes.contains(KotlinPlatformType.attribute) ||
                        configuration.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.name in KotlinUsages.values
            }
            .forEach { configuration ->
                val category = configuration.attributes.getAttribute(Category.CATEGORY_ATTRIBUTE)
                assertNotNull(category, "Expected configuration ${configuration.name} to provide 'Category' attribute")
                assertEquals(Category.LIBRARY, category.name, "Expected configuration $configuration to be 'LIBRARY' Category")
            }
    }

    @Test
    fun `don't publish wasm targets with KotlinJsCompilerAttribute attribute`() {
        with(kotlin) {
            js("nodeJs", KotlinJsCompilerType.IR)
            js("browser", KotlinJsCompilerType.IR)
            wasm()

            val allJs = sourceSets.create("allJs")
            targets.getByName("nodeJs").compilations.getByName("main").defaultSourceSet.dependsOn(allJs)
            targets.getByName("browser").compilations.getByName("main").defaultSourceSet.dependsOn(allJs)
        }

        project.evaluate()

        val targetSpecificConfigurationsToCheck = listOf(
            "ApiElements",
            "RuntimeElements",

            "MainApiDependenciesMetadata",
            "MainCompileOnlyDependenciesMetadata",
            "MainImplementationDependenciesMetadata",
            "MainRuntimeOnlyDependenciesMetadata",

            "TestApiDependenciesMetadata",
            "TestCompileOnlyDependenciesMetadata",
            "TestImplementationDependenciesMetadata",
            "TestRuntimeOnlyDependenciesMetadata",
        )

        // WASM
        val actualWasmConfigurations = targetSpecificConfigurationsToCheck
            .map { project.configurations.getByName("wasm$it") }
            .filter { it.attributes.contains(KotlinJsCompilerAttribute.jsCompilerAttribute) }

        assertEquals(
            emptyList(),
            actualWasmConfigurations,
            "All WASM configurations should not contain KotlinJsCompilerAttribute"
        )

        val commonSourceSetsConfigurationsToCheck = listOf(
            "ApiDependenciesMetadata",
            "CompileOnlyDependenciesMetadata",
            "ImplementationDependenciesMetadata",
            "RuntimeOnlyDependenciesMetadata",
        )

        // allJs
        val expectedAllJsConfigurations = commonSourceSetsConfigurationsToCheck
            .map { project.configurations.getByName("allJs$it") }

        val actualAllJsConfigurations = expectedAllJsConfigurations
            .filter { it.attributes.contains(KotlinJsCompilerAttribute.jsCompilerAttribute) }

        assertEquals(
            expectedAllJsConfigurations,
            actualAllJsConfigurations,
            "JS-only configurations should contain KotlinJsCompilerAttribute"
        )


        // commonMain
        val actualCommonMainConfigurations = commonSourceSetsConfigurationsToCheck
            .map { project.configurations.getByName("commonMain$it") }
            .filter { it.attributes.contains(KotlinJsCompilerAttribute.jsCompilerAttribute) }

        assertEquals(
            emptyList(),
            actualCommonMainConfigurations,
            "commonMain configurations should not contain KotlinJsCompilerAttribute"
        )

    }
}
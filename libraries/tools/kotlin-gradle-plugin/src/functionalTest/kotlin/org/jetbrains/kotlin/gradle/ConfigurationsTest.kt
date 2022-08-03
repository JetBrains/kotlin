/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle

import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.mpp.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.mpp.kotlin
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.targets
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsCompilerAttribute
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
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
            @OptIn(ExperimentalWasmDsl::class)
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

    @Test
    fun `test js IR compilation dependencies`() {
        val project = buildProjectWithMPP {
            kotlin {
                js(BOTH)
                targets.withType<KotlinJsTarget> {
                    irTarget!!.compilations.getByName("main").dependencies {
                        api("test:compilation-dependency")
                    }
                }

                sourceSets.getByName("jsMain").apply {
                    dependencies {
                        api("test:source-set-dependency")
                    }
                }
            }
        }

        project.evaluate()

        with(project) {
            assertContainsDependencies("jsCompilationApi", "test:compilation-dependency", "test:source-set-dependency")
            assertContainsDependencies("jsMainApi", "test:source-set-dependency")
            assertNotContainsDependencies("jsMainApi", "test:compilation-dependency")
        }
    }

    @Test
    fun `test compilation and source set configurations don't clash`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
                js(BOTH)
                linuxX64("linux")
            }
        }

        project.evaluate()

        project.kotlinExtension.targets.flatMap { it.compilations }.forEach { compilation ->
            val compilationSourceSets = compilation.allKotlinSourceSets
            val compilationConfigurationNames = compilation.relatedConfigurationNames
            val sourceSetConfigurationNames = compilationSourceSets.flatMapTo(mutableSetOf()) { it.relatedConfigurationNames }

            assert(compilationConfigurationNames.none { it in sourceSetConfigurationNames }) {
                """A name clash between source set and compilation configurations detected for the following configurations:
                    |${compilationConfigurationNames.filter { it in sourceSetConfigurationNames }.joinToString()}
                """.trimMargin()
            }
        }
    }

    @Test
    fun `test scoped sourceSet's configurations don't extend other configurations`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
                js(BOTH)
                linuxX64("linux")
            }
        }

        project.evaluate()

        for (sourceSet in project.kotlinExtension.sourceSets) {
            val configurationNames = listOf(
                sourceSet.implementationConfigurationName,
                sourceSet.apiConfigurationName,
                sourceSet.compileOnlyConfigurationName,
                sourceSet.runtimeOnlyConfigurationName,
            )

            for (name in configurationNames) {
                val extendsFrom = project.configurations.getByName(name).extendsFrom
                assert(extendsFrom.isEmpty()) {
                    "Configuration $name is not expected to be extending anything, but it extends: ${
                        extendsFrom.joinToString(
                            prefix = "[",
                            postfix = "]"
                        ) { it.name }
                    }"
                }
            }
        }
    }
}
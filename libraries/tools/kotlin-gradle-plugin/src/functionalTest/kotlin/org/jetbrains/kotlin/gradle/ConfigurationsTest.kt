/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle

import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
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
            androidLibrary {
                compileSdk = 30
            }

            kotlin {
                jvm()
                js(BOTH)
                linuxX64("linux")
                android()
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

    class TestDisambiguationAttributePropagation {
        private val disambiguationAttribute = org.gradle.api.attributes.Attribute.of("disambiguationAttribute", String::class.java)

        private val mppProject get() = buildProjectWithMPP {
            kotlin {
                jvm("plainJvm") {
                    attributes { attribute(disambiguationAttribute, "plainJvm") }
                }

                jvm("jvmWithJava") {
                    withJava()
                    attributes { attribute(disambiguationAttribute, "jvmWithJava") }
                }
            }
        }

        private val javaProject get() = buildProject {
            project.plugins.apply("java-library")
        }

        //NB: There is no "api" configuration registered by Java Plugin
        private val javaConfigurations = listOf(
            "compileClasspath",
            "runtimeClasspath",
            "implementation",
            "compileOnly",
            "runtimeOnly"
        )

        @Test
        fun `test that jvm target attributes are propagated to java configurations`() {
            val kotlinJvmConfigurations = listOf(
                "jvmWithJavaCompileClasspath",
                "jvmWithJavaRuntimeClasspath",
                "jvmWithJavaCompilationApi",
                "jvmWithJavaCompilationImplementation",
                "jvmWithJavaCompilationCompileOnly",
                "jvmWithJavaCompilationRuntimeOnly",
            )

            val outgoingConfigurations = listOf(
                "jvmWithJavaApiElements",
                "jvmWithJavaRuntimeElements",
            )

            val testJavaConfigurations = listOf(
                "testCompileClasspath",
                "testCompileOnly",
                "testImplementation",
                "testRuntimeClasspath",
                "testRuntimeOnly"
            )

            val jvmWithJavaTestConfigurations = listOf(
                "jvmWithJavaTestCompileClasspath",
                "jvmWithJavaTestRuntimeClasspath",
                "jvmWithJavaTestCompilationApi",
                "jvmWithJavaTestCompilationCompileOnly",
                "jvmWithJavaTestCompilationImplementation",
                "jvmWithJavaTestCompilationRuntimeOnly"
            )

            val expectedConfigurationsWithDisambiguationAttribute = javaConfigurations +
                    kotlinJvmConfigurations +
                    outgoingConfigurations +
                    testJavaConfigurations +
                    jvmWithJavaTestConfigurations

            with(mppProject.evaluate()) {
                val actualConfigurationsWithDisambiguationAttribute = configurations
                    .filter { it.attributes.getAttribute(disambiguationAttribute) == "jvmWithJava" }
                    .map { it.name }

                assertEquals(
                    expectedConfigurationsWithDisambiguationAttribute.sorted(),
                    actualConfigurationsWithDisambiguationAttribute.sorted()
                )
            }
        }

        @Test
        fun `test that no new attributes are added to java configurations`() {
            val evaluatedJavaProject = javaProject.evaluate()
            val evaluatedMppProject = mppProject.evaluate()

            fun AttributeContainer.toStringMap(): Map<String, String> =
                keySet().associate { it.name to getAttribute(it).toString() }

            for (configurationName in javaConfigurations) {
                val expectedAttributes = evaluatedJavaProject
                    .configurations
                    .getByName(configurationName)
                    .attributes.toStringMap()

                val actualAttributes = evaluatedMppProject
                    .configurations
                    .getByName(configurationName)
                    .attributes.toStringMap()

                assertEquals(
                    expectedAttributes,
                    actualAttributes - disambiguationAttribute.name
                )
            }
        }
    }

    @Test
    fun `test platform notation for BOM is consumable in dependencies`() {
        val project = buildProjectWithMPP {
            kotlin {
                jvm()
                sourceSets.getByName("jvmMain").apply {
                    dependencies {
                        api(platform("test:platform-dependency:1.0.0"))
                    }
                }
            }
        }

        project.evaluate()

        project.assertContainsDependencies("jvmMainApi", project.dependencies.platform("test:platform-dependency:1.0.0"))
    }


    @Test
    fun `test enforcedPlatform notation for BOM is consumable in dependencies`() {
        val project = buildProjectWithMPP {
            kotlin {
                js("browser") {
                    browser {
                        binaries.executable()
                    }
                }
                sourceSets.getByName("browserMain").apply {
                    dependencies {
                        implementation(enforcedPlatform("test:enforced-platform-dependency"))
                    }
                }
            }
        }

        project.evaluate()

        project.assertContainsDependencies(
            "browserMainImplementation",
            project.dependencies.enforcedPlatform("test:enforced-platform-dependency")
        )
    }
}
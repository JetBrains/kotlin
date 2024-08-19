/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.artifacts.internal.KlibPackaging
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.tasks.configuration.BaseKotlinCompileConfig.Companion.CLASSES_SECONDARY_VARIANT_NAME
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.enableNonPackedKlibsUsage
import org.jetbrains.kotlin.gradle.util.enableSecondaryJvmClassesVariant
import org.jetbrains.kotlin.gradle.util.osVariantSeparatorsPathString
import org.jetbrains.kotlin.test.util.JUnit4Assertions.assertTrue
import kotlin.test.*

class MultiplatformSecondaryOutgoingVariantsTest {

    private fun buildProjectWithMPPAndJvmClassesVariant(
        code: Project.() -> Unit = {},
    ) = buildProjectWithMPP(
        preApplyCode = {
            project.enableSecondaryJvmClassesVariant()
        },
        code = code
    )

    private fun buildKmpProjectWithKlibTargets(preApplyCode: Project.() -> Unit = {}): ProjectInternal {
        val project = buildProjectWithMPP(preApplyCode = preApplyCode) {
            @OptIn(ExperimentalWasmDsl::class)
            with(multiplatformExtension) {
                linuxX64 {
                    compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).cinterops.create("dummy")
                }
                js()
                wasmJs()
                wasmWasi()
                applyDefaultHierarchyTemplate()
            }
        }
        project.evaluate()
        return project
    }

    @Test
    fun shouldAddSecondaryJvmClassesVariantForJvmApiConfigurations() {
        val project = buildProjectWithMPPAndJvmClassesVariant {
            with(multiplatformExtension) {
                jvm()
                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        project.assertJvmClassesVariants(
            expectedArtifactsSize = 1,
            isJavaCompilationEnabled = false,
        )
    }

    @Test
    fun shouldAddSecondaryJvmClassesVariantForJvmApiConfigurationsWithJavaEnabled() {
        val project = buildProjectWithMPPAndJvmClassesVariant {
            with(multiplatformExtension) {
                jvm {
                    withJava()
                }
                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        project.assertJvmClassesVariants(expectedArtifactsSize = 2, isJavaCompilationEnabled = true)
    }

    @Test
    fun shouldAddSecondaryJvmClassesVariantForJvmApiConfigurationsWithJavaEnabledAndJavaLibraryPluginApplied() {
        val project = buildProjectWithMPPAndJvmClassesVariant {
            plugins.apply("java-library")

            with(multiplatformExtension) {
                jvm {
                    withJava()
                }
                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        project.assertJvmClassesVariants(expectedArtifactsSize = 2, isJavaCompilationEnabled = true)
    }

    @Test
    fun shouldAddSecondaryJvmClassesVariantForJvmApiConfigurationsWithJavaTestFixturesApplied() {
        val project = buildProjectWithMPPAndJvmClassesVariant {
            plugins.apply("java-test-fixtures")

            with(multiplatformExtension) {
                jvm {
                    withJava()
                }
                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        project.assertJvmClassesVariants(expectedArtifactsSize = 2, isJavaCompilationEnabled = true)
    }

    @Test
    fun shouldNotAddSecondaryJvmClassesVariantByDefault() {
        val project = buildProjectWithMPP {
            with(multiplatformExtension) {
                jvm()
                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        project.jvmApiConfigurations.forEach { configuration ->
            assertNull(configuration.outgoing.variants.findByName(CLASSES_SECONDARY_VARIANT_NAME))
        }
    }

    @Test
    fun nonPackedKlibsUsageMayBeDisabled() {
        val project = buildKmpProjectWithKlibTargets {
            project.enableNonPackedKlibsUsage(false)
        }
        val apiConfigurations = project.getKlibApiConfigurations()
        project.assertKlibWithoutNonPackedVariant(apiConfigurations)
    }

    @Test
    fun shouldAddNonPackedKlibVariantByDefaultForKlibTargets() {
        val project = buildKmpProjectWithKlibTargets()
        val apiConfigurations = project.getKlibApiConfigurations()
        project.assertKlibWithNonPackedVariants(apiConfigurations)
    }

    @Test
    fun klibPackagingAttributeIsNotPublished() {
        val project = buildKmpProjectWithKlibTargets {
            plugins.apply("maven-publish")
        }
        project.assertKlibPackagingAttributeNotPublished()
    }

    @Test
    fun klibPackagingAttributeIsNotPublishedWhenNonPackedKlibsAreNotUsed() {
        val project = buildKmpProjectWithKlibTargets {
            project.enableNonPackedKlibsUsage(false)
            plugins.apply("maven-publish")
        }
        project.assertKlibPackagingAttributeNotPublished()
    }

    private fun Project.getKlibApiConfigurations(): List<Configuration> {
        val platformTypes = setOf(KotlinPlatformType.wasm, KotlinPlatformType.native, KotlinPlatformType.js)
        val usages = setOf(KotlinUsages.KOTLIN_API, KotlinUsages.KOTLIN_CINTEROP)
        val apiConfigurations = project.configurations.filter {
            it.isCanBeConsumed &&
                    it.attributes.getAttribute(KotlinPlatformType.attribute) in platformTypes &&
                    it.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.toString() in usages
        }
        val numberOfConfigurations =
            multiplatformExtension.targets.filter { it.platformType in platformTypes }.size + multiplatformExtension.targets.filterIsInstance<KotlinNativeTarget>()
                .sumOf { it.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).cinterops.size }
        assert(numberOfConfigurations == apiConfigurations.size) {
            """
                The number of consumable API configurations is unexpected. Expected to have 1 configuration per target + 1 per declared cinterop ($numberOfConfigurations in total). 
                Found: ${apiConfigurations.map { it.name }}.
                Targets: ${multiplatformExtension.targets.map { it.name }}
            """.trimIndent()
        }
        return apiConfigurations
    }

    private val Project.jvmApiConfigurations
        get() = configurations.filter {
            it.isCanBeConsumed &&
                    it.attributes.getAttribute(KotlinPlatformType.attribute) == KotlinPlatformType.jvm &&
                    it.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.toString() == Usage.JAVA_API
        }

    private fun Project.assertKlibWithoutNonPackedVariant(
        apiConfigurations: List<Configuration>,
    ) {
        for (configuration in apiConfigurations) {
            assertNull(
                configuration.attributes.getAttribute(KlibPackaging.attribute),
                "The primary variant of ${configuration.name} should not have attribute ${KlibPackaging.ATTRIBUTE_NAME} set to."
            )
            val nonPackedKlibVariant = configuration.outgoing.variants.findByName(KlibPackaging.NON_PACKED_KLIB_VARIANT_NAME)
            assertNull(
                nonPackedKlibVariant,
                "Expected non-packed KLIB variant to be absent in configuration ${configuration.name}"
            )
            for (packedArtifact in configuration.artifacts) {
                val packedArtifactPath = packedArtifact.file.invariantSeparatorsPath
                assert(
                    packedArtifactPath.endsWith("/build/classes/kotlin/linuxX64/main/klib/${project.name}.klib") ||
                            packedArtifactPath.matches(".*/build/classes/kotlin/linuxX64/main/cinterop/${project.name}-cinterop-[a-zA-Z0-9]+.klib".toRegex()) ||
                            packedArtifactPath.matches(".*/build/libs/${project.name}-(wasm-wasi|wasm-js|js).klib".toRegex())
                ) {
                    "Artifact $packedArtifactPath from $configuration does not match the expected patterns."
                }
            }
        }
    }

    private fun Project.assertKlibWithNonPackedVariants(
        apiConfigurations: List<Configuration>,
    ) {
        for (configuration in apiConfigurations) {
            assertEquals(
                KlibPackaging.PACKED,
                configuration.attributes.getAttribute(KlibPackaging.attribute),
                "The primary variant of ${configuration.name} should be packed."
            )
            val nonPackedKlibVariant = configuration.outgoing.variants.findByName(KlibPackaging.NON_PACKED_KLIB_VARIANT_NAME)
            assertNotNull(nonPackedKlibVariant) {
                "Expected non-packed KLIB variant to be present in configuration ${configuration.name}"
            }
            assertEquals(
                KlibPackaging.NON_PACKED,
                nonPackedKlibVariant.attributes.getAttribute(KlibPackaging.attribute),
                "The non-packed variant of ${configuration.name} should have proper attribute."
            )
            // cinterop also adds artifacts to main apiElements
            // also we add a fake artifact as a workaround for https://github.com/gradle/gradle/issues/29630
            for (packedArtifact in configuration.artifacts) {
                val packedArtifactPath = packedArtifact.file.invariantSeparatorsPath
                assert(
                    packedArtifactPath.matches(".*/build/libs/.*.klib".toRegex()) ||
                            packedArtifactPath.endsWith("non-existing-file-workaround-for-gradle-29630.txt")
                ) {
                    "Artifact $packedArtifactPath from $configuration does not match the expected patterns."
                }
            }
            for (nonPackedArtifact in nonPackedKlibVariant.artifacts) {
                val nonPackedArtifactPath = nonPackedArtifact.file.invariantSeparatorsPath
                assert(
                    nonPackedArtifactPath.matches(".*/build/classes/kotlin/(wasmWasi|wasmJs|linuxX64|js)/main(/klib/${project.name})?".toRegex()) ||
                            nonPackedArtifactPath.matches(".*/build/classes/kotlin/linuxX64/main/cinterop/${project.name}-cinterop-[a-zA-Z0-9]+".toRegex())
                ) {
                    "Artifact $nonPackedArtifactPath from $nonPackedKlibVariant does not match the expected patterns."
                }
            }
        }
    }

    private fun Project.assertKlibPackagingAttributeNotPublished() {
        val kotlinComponent = project.components.getByName("kotlin") as ComponentWithVariants
        val usagesByVariant = kotlinComponent.variants.associateWith { variant -> (variant as SoftwareComponentInternal).usages }
        for ((variant, usages) in usagesByVariant) {
            for (usage in usages) {
                assertNull(
                    usage.attributes.getAttribute(KlibPackaging.attribute),
                    "Expected no attribute ${KlibPackaging.ATTRIBUTE_NAME} to be published for usage context ${usage.name} of variant ${variant.name}."
                )
            }
        }
    }

    private fun Project.assertJvmClassesVariants(
        expectedArtifactsSize: Int = 1,
        apiConfigurations: List<Configuration> = jvmApiConfigurations,
        isJavaCompilationEnabled: Boolean = false,
    ) {
        assertTrue(apiConfigurations.isNotEmpty())

        apiConfigurations.forEach { configuration ->
            val classesVariant = configuration.outgoing.variants.getByName(CLASSES_SECONDARY_VARIANT_NAME)
            assertNotNull(classesVariant)
            assertEquals(expectedArtifactsSize, classesVariant.artifacts.size)

            val kotlinClasses = classesVariant.artifacts.find {
                it.file.path.endsWith("build/classes/kotlin/jvm/main".osVariantSeparatorsPathString)
            }
            assertNotNull(kotlinClasses)
            assertTrue(kotlinClasses.buildDependencies.getDependencies(null).size >= 1)

            if (isJavaCompilationEnabled) {
                val javaClasses = classesVariant.artifacts.find {
                    it.file.path.endsWith("build/classes/java/main".osVariantSeparatorsPathString)
                }
                assertNotNull(javaClasses)
                assertTrue(javaClasses.buildDependencies.getDependencies(null).size >= 1)
            }
        }
    }
}
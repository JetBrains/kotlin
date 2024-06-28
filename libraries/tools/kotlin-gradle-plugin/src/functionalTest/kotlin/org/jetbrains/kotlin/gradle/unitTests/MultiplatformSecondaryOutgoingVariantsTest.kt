/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.configuration.BaseKotlinCompileConfig.Companion.CLASSES_SECONDARY_VARIANT_NAME
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.enableSecondaryJvmClassesVariant
import org.jetbrains.kotlin.gradle.util.osVariantSeparatorsPathString
import org.jetbrains.kotlin.test.util.JUnit4Assertions.assertTrue
import kotlin.test.*

class MultiplatformSecondaryOutgoingVariantsTest {

    private fun buildProjectWithMPPAndJvmClassesVariant(
        code: Project.() -> Unit = {}
    ) = buildProjectWithMPP(
        preApplyCode = {
            project.enableSecondaryJvmClassesVariant()
        },
        code = code
    )


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

    private val Project.jvmApiConfigurations
        get() = configurations.filter {
            it.isCanBeConsumed &&
                    it.attributes.getAttribute(KotlinPlatformType.attribute) == KotlinPlatformType.jvm &&
                    it.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.toString() == Usage.JAVA_API
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
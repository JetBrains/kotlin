/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.javaSourceSets
import org.jetbrains.kotlin.gradle.tasks.configuration.BaseKotlinCompileConfig.Companion.CLASSES_SECONDARY_VARIANT_NAME
import org.jetbrains.kotlin.gradle.util.buildProjectWithJvm
import org.jetbrains.kotlin.gradle.util.osVariantSeparatorsPathString
import kotlin.test.*

class JvmSecondaryOutgoingVariantsTest {

    @Test
    fun shouldAddJvmSecondaryClassesVariantForDefaultApiConfigurations() {
        val project = buildProjectWithJvm()

        project.evaluate()

        project.assertDefaultClassesVariants()
    }

    @Test
    fun shouldWorkWithJavaLibraryPluginApplied() {
        val project = buildProjectWithJvm {
            plugins.apply("java-library")
        }

        project.evaluate()

        project.assertDefaultClassesVariants()
    }

    @Test
    fun shouldAddJvmSecondaryClassesVariantForAllApiConfigurations() {
        val project = buildProjectWithJvm {
            plugins.apply("java-test-fixtures") // creates additional apiElements configuration for fixtures

            val featureSourceSet = javaSourceSets.create("someNewFeature") {
                it.java.srcDir("src/feature/java")
            }

            extensions.getByType(JavaPluginExtension::class.java).registerFeature("someNewFeature") {
                it.usingSourceSet(featureSourceSet)
            }
        }

        project.evaluate()

        val apiConfigurations = project.apiConfigurations
        assertEquals(
            apiConfigurations.map { it.name },
            listOf("apiElements", "someNewFeatureApiElements", "testFixturesApiElements")
        )

        apiConfigurations.forEach { configuration ->
            val classesVariant = configuration.outgoing.variants.getByName(CLASSES_SECONDARY_VARIANT_NAME)
            assertNotNull(classesVariant)
            assertTrue(classesVariant.artifacts.size == 2)
        }
    }

    private val Project.apiConfigurations
        get() = configurations.filter {
            it.isCanBeConsumed && it.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.toString() == Usage.JAVA_API
        }

    private fun Project.assertDefaultClassesVariants(
        expectedArtifactsSize: Int = 2
    ) {
        val apiConfigurations = project.apiConfigurations
        assertTrue(apiConfigurations.isNotEmpty())

        apiConfigurations.forEach { configuration ->
            val classesVariant = configuration.outgoing.variants.getByName(CLASSES_SECONDARY_VARIANT_NAME)
            assertNotNull(classesVariant)
            assertEquals(expectedArtifactsSize, classesVariant.artifacts.size)

            val kotlinClasses = classesVariant.artifacts.find {
                it.file.path.endsWith("build/classes/kotlin/main".osVariantSeparatorsPathString)
            }
            assertNotNull(kotlinClasses)
            assertTrue(kotlinClasses.buildDependencies.getDependencies(null).size >= 1)

            val javaClasses = classesVariant.artifacts.find {
                it.file.path.endsWith("build/classes/java/main".osVariantSeparatorsPathString)
            }
            assertNotNull(javaClasses)
            assertTrue(javaClasses.buildDependencies.getDependencies(null).size >= 1)
        }
    }
}
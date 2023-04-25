/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import com.android.build.gradle.LibraryExtension
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.kotlinProjectStructureMetadata
import org.jetbrains.kotlin.gradle.util.addBuildEventsListenerRegistryMock
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import kotlin.test.*

class JvmAndAndroidIntermediateSourceSetTest {

    private lateinit var project: ProjectInternal
    private lateinit var kotlin: KotlinMultiplatformExtension
    private lateinit var jvmAndAndroidMain: KotlinSourceSet

    @BeforeTest
    fun setup() {
        project = ProjectBuilder.builder().build() as ProjectInternal
        addBuildEventsListenerRegistryMock(project)
        project.extensions.getByType(ExtraPropertiesExtension::class.java).set("kotlin.mpp.enableGranularSourceSetsMetadata", "true")

        project.plugins.apply("kotlin-multiplatform")
        project.plugins.apply("android-library")

        /* Arbitrary minimal Android setup */
        val android = project.extensions.getByName("android") as LibraryExtension
        android.compileSdk = 31

        /* Kotlin Setup */
        kotlin = project.multiplatformExtension
        kotlin.jvm()
        kotlin.androidTarget()
        jvmAndAndroidMain = kotlin.sourceSets.create("jvmAndAndroidMain")
        kotlin.sourceSets.run {
            jvmAndAndroidMain.dependsOn(getByName("commonMain"))

            getByName("jvmMain") {
                it.dependsOn(jvmAndAndroidMain)
            }
            getByName("androidMain") {
                it.dependsOn(jvmAndAndroidMain)
            }
        }
    }

    @Test
    fun `metadata compilation is created and disabled`() {
        /* evaluate */
        project.evaluate()

        /* Check if compilation is created correctly */
        val jvmAndAndroidMainMetadataCompilations = kotlin.targets.flatMap { it.compilations }
            .filterIsInstance<KotlinMetadataCompilation<*>>()
            .filter { it.name == jvmAndAndroidMain.name }

        assertEquals(
            1, jvmAndAndroidMainMetadataCompilations.size,
            "Expected exactly one metadata compilation created for jvmAndAndroidMain source set"
        )

        val compilation = jvmAndAndroidMainMetadataCompilations.single()
        assertFalse(
            compilation.compileTaskProvider.get().enabled,
            "Expected compilation task to be disabled, because not supported yet"
        )
    }

    @Test
    fun `KotlinProjectStructureMetadata jvmAndAndroidMain exists in jvm variants`() {
        project.evaluate()
        val metadata = kotlin.kotlinProjectStructureMetadata
        assertTrue("jvmAndAndroidMain" in metadata.sourceSetNamesByVariantName["jvmApiElements"].orEmpty())
        assertTrue("jvmAndAndroidMain" in metadata.sourceSetNamesByVariantName["jvmRuntimeElements"].orEmpty())
    }

    @Test
    fun `KotlinProjectStructureMetadata jvmAndAndroidMain exists in android variants`() {
        project.evaluate()
        val metadata = kotlin.kotlinProjectStructureMetadata
        assertTrue("jvmAndAndroidMain" in metadata.sourceSetNamesByVariantName["debugApiElements"].orEmpty())
        assertTrue("jvmAndAndroidMain" in metadata.sourceSetNamesByVariantName["debugRuntimeElements"].orEmpty())
        assertTrue("jvmAndAndroidMain" in metadata.sourceSetNamesByVariantName["releaseApiElements"].orEmpty())
        assertTrue("jvmAndAndroidMain" in metadata.sourceSetNamesByVariantName["releaseRuntimeElements"].orEmpty())
    }

    @Test
    fun `Android Kotlin Components are marked as not publishable when variant is not published`() {
        val target = kotlin.targets.getByName("android") as KotlinAndroidTarget
        target.publishLibraryVariants = emptyList()
        project.evaluate()
        val kotlinComponents = target.kotlinComponents
        assertTrue(kotlinComponents.isNotEmpty(), "Expected at least one KotlinComponent to be present")

        kotlinComponents.forEach { component ->
            assertFalse(component.publishable, "Expected component to not publishable, because no publication is configured")
        }
    }

    @Test
    fun `Android Kotlin Components are marked as publishable when variant is published`() {
        val target = kotlin.targets.getByName("android") as KotlinAndroidTarget
        target.publishLibraryVariants = listOf("release")
        project.evaluate()
        val kotlinComponents = target.kotlinComponents
        assertTrue(kotlinComponents.isNotEmpty(), "Expected at least one KotlinComponent to be present")

        kotlinComponents.forEach { component ->
            val isReleaseComponent = "release" in component.name.toLowerCaseAsciiOnly()
            if (isReleaseComponent) {
                assertTrue(component.publishable, "Expected release component to be marked as publishable")
            } else {
                assertFalse(component.publishable, "Expected non-release component to be marked as not publishable")
            }
        }
    }
}

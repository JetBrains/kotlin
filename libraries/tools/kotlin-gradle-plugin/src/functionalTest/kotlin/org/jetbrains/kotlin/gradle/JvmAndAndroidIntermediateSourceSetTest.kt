/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/* Associate compilations are not yet supported by the IDE. KT-34102 */
@file:Suppress("invisible_reference", "invisible_member", "FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle

import com.android.build.gradle.LibraryExtension
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder
import org.hamcrest.MatcherAssert.assertThat
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.buildKotlinProjectStructureMetadata
import org.junit.Before
import org.junit.Test


class JvmAndAndroidIntermediateSourceSetTest {

    private lateinit var project: ProjectInternal
    private lateinit var kotlin: KotlinMultiplatformExtension
    private lateinit var jvmAndAndroidMain: KotlinSourceSet

    @Before
    fun setup() {
        project = ProjectBuilder.builder().build() as ProjectInternal
        project.extensions.getByType(ExtraPropertiesExtension::class.java).set("kotlin.mpp.enableGranularSourceSetsMetadata", "true")

        project.plugins.apply("kotlin-multiplatform")
        project.plugins.apply("android-library")

        /* Arbitrary minimal Android setup */
        val android = project.extensions.getByName("android") as LibraryExtension
        android.compileSdkVersion(30)

        /* Kotlin Setup */
        kotlin = project.multiplatformExtension
        kotlin.jvm()
        kotlin.android()
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

        assertThat(
            "Expected exactly one metadata compilation created for jvmAndAndroidMain source set",
            1 == jvmAndAndroidMainMetadataCompilations.size
        )

        val compilation = jvmAndAndroidMainMetadataCompilations.single()
        assertThat(
            "Expected compilation task to be disabled, because not supported yet",
            !compilation.compileKotlinTaskProvider.get().enabled
        )
    }

    @Test
    fun `KotlinProjectStructureMetadata jvmAndAndroidMain exists in jvm variants`() {
        project.evaluate()
        val metadata = buildKotlinProjectStructureMetadata(project)
        assertThat(
            "Kotlin project structure metadata is null",
            metadata != null
        )
        assertThat(
            "'jvmAndAndroidMain' source set is not included into 'jvmApiElements' variant metadata",
            "jvmAndAndroidMain" in metadata!!.sourceSetNamesByVariantName["jvmApiElements"].orEmpty()
        )
        assertThat(
            "'jvmAndAndroidName' source set is not included into 'jvmRuntimeElements' variant metadata",
            "jvmAndAndroidMain" in metadata.sourceSetNamesByVariantName["jvmRuntimeElements"].orEmpty()
        )
    }

    @Test
    fun `KotlinProjectStructureMetadata jvmAndAndroidMain exists in android variants`() {
        project.evaluate()
        val metadata = buildKotlinProjectStructureMetadata(project)
        assertThat(
            "Kotlin project structure metadata is null",
            metadata != null
        )
        assertThat(
            "'jvmAndAndroidMain' source set is not included into 'debugApiElements' variant metadata",
            "jvmAndAndroidMain" in metadata!!.sourceSetNamesByVariantName["debugApiElements"].orEmpty()
        )
        assertThat(
            "'jvmAndAndroidMain' source set is not included into 'debugRuntimeElements' variant metadata",
            "jvmAndAndroidMain" in metadata.sourceSetNamesByVariantName["debugRuntimeElements"].orEmpty()
        )
        assertThat(
            "'jvmAndAndroidMain' source set is not included into 'releaseApiElements' variant metadata",
            "jvmAndAndroidMain" in metadata.sourceSetNamesByVariantName["releaseApiElements"].orEmpty()
        )
        assertThat(
            "'jvmAndAndroidMain' source set is not included into 'releaseRuntimeElements' variant metadata",
            "jvmAndAndroidMain" in metadata.sourceSetNamesByVariantName["releaseRuntimeElements"].orEmpty()
        )
    }

    @Test
    fun `Android Kotlin Components are marked as not publishable when variant is not published`() {
        val target = kotlin.targets.getByName("android") as KotlinAndroidTarget
        target.publishLibraryVariants = emptyList()
        project.evaluate()
        val kotlinComponents = target.kotlinComponents
        assertThat(
            "Expected at least one KotlinComponent to be present",
            kotlinComponents.isNotEmpty()
        )

        kotlinComponents.forEach { component ->
            assertThat(
                "Expected component to not publishable, because no publication is configured",
                !component.publishable
            )
        }
    }

    @Test
    fun `Android Kotlin Components are marked as publishable when variant is published`() {
        val target = kotlin.targets.getByName("android") as KotlinAndroidTarget
        target.publishLibraryVariants = listOf("release")
        project.evaluate()
        val kotlinComponents = target.kotlinComponents
        assertThat(
            "Expected at least one KotlinComponent to be present",
            kotlinComponents.isNotEmpty()
        )

        kotlinComponents.forEach { component ->
            val isReleaseComponent = "release" in component.name.toLowerCase()
            if (isReleaseComponent) {
                assertThat(
                    "Expected release component to be marked as publishable",
                    component.publishable
                )
            } else {
                assertThat(
                    "Expected non-release component to be marked as not publishable",
                    !component.publishable
                )
            }
        }
    }
}

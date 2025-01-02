/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.regressionTests

import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.HasAttributes
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.internal.extensions.core.extra
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.attributes.KlibPackaging
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.targets.NON_PACKED_KLIB_VARIANT_NAME
import org.jetbrains.kotlin.gradle.targets.native.internal.CInteropKlibLibraryElements.cinteropKlibLibraryElements
import org.jetbrains.kotlin.gradle.targets.native.internal.locateOrCreateCInteropApiElementsConfiguration
import org.jetbrains.kotlin.gradle.targets.native.internal.locateOrCreateCInteropDependencyConfiguration
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.main
import org.jetbrains.kotlin.test.MuteableTestRule
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KT56143CinteropConfigurationAttributes {
    @get:Rule val muteableTestRule = MuteableTestRule()

    private val targetAttribute = Attribute.of("for.target", String::class.java)
    private val compilationAttribute = Attribute.of("for.compilation", String::class.java)

    private fun buildProject(preApplyCode: Project.() -> Unit = {}) = buildProjectWithMPP(
        preApplyCode = preApplyCode
    ) {
        kotlin {
            linuxX64("variantA") {
                attributes.attributeProvider(targetAttribute, provider { "a" })
                compilations.main.attributes.attributeProvider(compilationAttribute, provider { "compilation:a" })
            }

            linuxX64("variantB") {
                attributes.attributeProvider(targetAttribute, provider { "b" })
                compilations.main.attributes.attributeProvider(compilationAttribute, provider { "compilation:b" })
            }
        }
    }

    private val Project.variantA
        get() = multiplatformExtension.linuxX64("variantA")

    private val Project.variantB
        get() = multiplatformExtension.linuxX64("variantB")

    @Test
    fun `test - cinteropApiElements - contains target attributes`() {
        val project = buildProject()
        project.evaluate()

        val variantAElements = project.locateOrCreateCInteropApiElementsConfiguration(project.variantA)
        assertEquals("a", variantAElements.attributes.getAttribute(targetAttribute))

        val variantBElements = project.locateOrCreateCInteropApiElementsConfiguration(project.variantB)
        assertEquals("b", variantBElements.attributes.getAttribute(targetAttribute))
    }

    @Test
    fun `test - cinteropDependencies - contains target and compilation attributes`() {
        val project = buildProject()
        project.evaluate()

        val variantADependencies = project.locateOrCreateCInteropDependencyConfiguration(
            project.variantA.compilations.main as KotlinNativeCompilation
        )
        assertEquals("a", variantADependencies.attributes.getAttribute(targetAttribute))
        assertEquals("compilation:a", variantADependencies.attributes.getAttribute(compilationAttribute))

        val variantBDependencies = project.locateOrCreateCInteropDependencyConfiguration(
            project.variantB.compilations.main as KotlinNativeCompilation
        )
        assertEquals("b", variantBDependencies.attributes.getAttribute(targetAttribute))
        assertEquals("compilation:b", variantBDependencies.attributes.getAttribute(compilationAttribute))
    }

    @Test
    fun `test - all cinterop configurations contain default attributes`() {
        testCinteropDefaultAttributes(true)
    }

    @Test
    fun `test - all cinterop configurations contain default attributes (with disabled non-packed klibs)`() {
        testCinteropDefaultAttributes(false)
    }

    private fun testCinteropDefaultAttributes(nonPackedKlibsEnabled: Boolean) {
        val project = buildProject {
            if (!nonPackedKlibsEnabled) {
                project.extra.set(PropertiesProvider.PropertyNames.KOTLIN_USE_NON_PACKED_KLIBS, "false")
            }
        }
        project.evaluate()

        fun checkConfigurationAttributes(configuration: HasAttributes, usesNonPackedKlib: Boolean, nonPackedKlibsEnabled: Boolean) {
            assertEquals(
                project.cinteropKlibLibraryElements(),
                configuration.attributes.getAttribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE),
                "Expected cinterop klib library elements on $configuration"
            )

            assertEquals(
                project.objects.named(Usage::class.java, KotlinUsages.KOTLIN_CINTEROP),
                configuration.attributes.getAttribute(Usage.USAGE_ATTRIBUTE),
                "Expected kotlin cinterop usage on $configuration"
            )

            assertEquals(
                project.categoryByName(Category.LIBRARY),
                configuration.attributes.getAttribute(Category.CATEGORY_ATTRIBUTE),
                "Expected library category on $configuration"
            )

            assertEquals(
                when {
                    nonPackedKlibsEnabled && usesNonPackedKlib -> project.objects.named(KlibPackaging::class.java, KlibPackaging.NON_PACKED)
                    nonPackedKlibsEnabled && !usesNonPackedKlib -> project.objects.named(KlibPackaging::class.java, KlibPackaging.PACKED)
                    else -> null
                },
                configuration.attributes.getAttribute(KlibPackaging.ATTRIBUTE),
                "Unexpected Klib packaging on $configuration"
            )
        }

        project.multiplatformExtension.targets.forEach { target ->
            val nonPackedKlibsAffectThisConfiguration = nonPackedKlibsEnabled && target is KotlinNativeTarget
            val cinteropApiElements = project.locateOrCreateCInteropApiElementsConfiguration(target)
            checkConfigurationAttributes(cinteropApiElements, false, nonPackedKlibsAffectThisConfiguration)
            val nonPackedVariant = cinteropApiElements.outgoing.variants.findByName(NON_PACKED_KLIB_VARIANT_NAME)
            if (!nonPackedKlibsAffectThisConfiguration) {
                assertEquals(
                    null, nonPackedVariant,
                    "Expected no non-packed variant on $cinteropApiElements"
                )
            } else {
                assertNotNull(nonPackedVariant) {
                    "Expected non-packed variant on $cinteropApiElements"
                }
                checkConfigurationAttributes(nonPackedVariant, true, true)
            }

            target.compilations.forEach { compilation ->
                if (compilation is KotlinNativeCompilation) {
                    val resolvableConfiguration = project.locateOrCreateCInteropDependencyConfiguration(compilation)
                    checkConfigurationAttributes(resolvableConfiguration, nonPackedKlibsAffectThisConfiguration, nonPackedKlibsAffectThisConfiguration)
                }
            }
        }
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.hierarchy.KotlinSourceSetTreeClassifier
import org.jetbrains.kotlin.gradle.plugin.hierarchy.orNull
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinCompilationDescriptor.CompilationFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createExternalKotlinTarget
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.gradle.utils.toMap
import kotlin.test.*

class ExternalKotlinTargetApiTests {

    val project = buildProjectWithMPP()
    val kotlin = project.multiplatformExtension


    @Test
    fun `test - sourceSetClassifier - default`() = buildProjectWithMPP().runLifecycleAwareTest {
        val target = kotlin.createExternalKotlinTarget<FakeTarget> { defaults() }
        val compilation = target.createCompilation<FakeCompilation> { defaults(kotlin) }

        assertEquals(KotlinSourceSetTree("fake"), KotlinSourceSetTree.orNull(compilation))
    }

    @Test
    fun `test - sourceSetClassifier - custom name`() = buildProjectWithMPP().runLifecycleAwareTest {
        val target = kotlin.createExternalKotlinTarget<FakeTarget> { defaults() }
        val compilation = target.createCompilation<FakeCompilation> {
            defaults(kotlin)
            sourceSetTreeClassifierV2 = KotlinSourceSetTreeClassifier.Name("mySourceSetTree")
        }

        assertEquals(KotlinSourceSetTree("mySourceSetTree"), KotlinSourceSetTree.orNull(compilation))
    }

    @Test
    fun `test - sourceSetClassifier - custom property`() = buildProjectWithMPP().runLifecycleAwareTest {
        val myProperty = project.objects.property<KotlinSourceSetTree>()
        val nullProperty = project.objects.property<KotlinSourceSetTree>()

        val target = kotlin.createExternalKotlinTarget<FakeTarget> { defaults() }

        val mainCompilation = target.createCompilation<FakeCompilation> {
            defaults(kotlin)
            sourceSetTreeClassifierV2 = KotlinSourceSetTreeClassifier.Property(myProperty)
        }

        val auxCompilation = target.createCompilation<FakeCompilation>() {
            compilationName = "aux"
            compilationFactory = CompilationFactory(::FakeCompilation)
            defaultSourceSet = kotlin.sourceSets.create("aux")
            sourceSetTreeClassifierV2 = KotlinSourceSetTreeClassifier.Property(nullProperty)
        }

        launchInStage(KotlinPluginLifecycle.Stage.FinaliseDsl) {
            myProperty.set(KotlinSourceSetTree.main)
        }

        assertEquals(KotlinSourceSetTree.main, KotlinSourceSetTree.orNull(mainCompilation))
        assertNull(KotlinSourceSetTree.orNull(auxCompilation))
    }

    @Test
    fun `test - sourcesElements - default configuration`() = buildProjectWithMPP().runLifecycleAwareTest {
        val target = kotlin.createExternalKotlinTarget<FakeTarget> { defaults() }

        assertNotEquals(target.sourcesElementsConfiguration, target.sourcesElementsPublishedConfiguration)

        assertEquals(
            target.sourcesElementsConfigurationName,
            target.sourcesElementsConfiguration.name
        )

        assertEquals(
            target.sourcesElementsConfiguration.attributes.toMap(),
            target.sourcesElementsPublishedConfiguration.attributes.toMap(),
            "Expected sourcesElements and sourcesElementsPublished to contain the same attributes"
        )

        assertEquals(
            KotlinPlatformType.jvm, target.sourcesElementsPublishedConfiguration.attributes.getAttribute(KotlinPlatformType.attribute),
            "Expected KotlinPlatformType attribute to be present"
        )

        assertEquals(
            Category.DOCUMENTATION, target.sourcesElementsPublishedConfiguration.attributes.getAttribute(Category.CATEGORY_ATTRIBUTE)?.name,
            "Expected 'Category.DOCUMENTATION' as category attribute"
        )

        assertEquals(
            DocsType.SOURCES, target.sourcesElementsPublishedConfiguration.attributes.getAttribute(DocsType.DOCS_TYPE_ATTRIBUTE)?.name,
            "Expected 'DocsType.SOURCES' attribute"
        )
    }

    @Test
    fun `test - sourcesElements - configure`() = buildProjectWithMPP().runLifecycleAwareTest {
        val testAttribute = Attribute.of("for.test", String::class.java)

        val target = kotlin.createExternalKotlinTarget<FakeTarget> {
            defaults()
            sourcesElements.configure { target, configuration ->
                assertEquals(target.sourcesElementsConfiguration, configuration)
                configuration.attributes.attribute(testAttribute, "sourcesElements")
            }

            sourcesElementsPublished.configure { target, configuration ->
                assertEquals(target.sourcesElementsPublishedConfiguration, configuration)
                configuration.attributes.attribute(testAttribute, "sourcesElements-published")
            }
        }

        /* Check traces left before */
        assertEquals("sourcesElements", target.sourcesElementsConfiguration.attributes.getAttribute(testAttribute))
        assertEquals("sourcesElements-published", target.sourcesElementsPublishedConfiguration.attributes.getAttribute(testAttribute))
    }

    @Test
    fun `test - sourcesElements - publication`() = buildProjectWithMPP().runLifecycleAwareTest {
        val target = kotlin.createExternalKotlinTarget<FakeTarget> { defaults() }
        val component = target.delegate.components.singleOrNull() ?: fail("Expected single 'component' for external target")

        component.usages.find { it.name == target.sourcesElementsPublishedConfiguration.name }
            ?: fail("Missing sourcesElements usage")
    }

    @Test
    fun `test - sourcesElements - publication - withSourcesJar set to false`() = buildProjectWithMPP().runLifecycleAwareTest {
        val target = kotlin.createExternalKotlinTarget<FakeTarget> { defaults() }
        target.withSourcesJar(false)
        val component = target.delegate.components.singleOrNull() ?: fail("Expected single 'component' for external target")
        val sourcesUsage = component.usages.find { it.name.contains("sources", true) }
        if (sourcesUsage != null) {
            fail("Unexpected usage '${sourcesUsage.name} in target publication")
        }
    }
}

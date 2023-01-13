/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests.kpm

import org.gradle.api.attributes.Attribute
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.FragmentNameDisambiguation
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.fail

class KotlinFragmentConfigurationDefinitionTest : AbstractKpmExtensionTest() {

    private val testContext by lazy {
        val names = FragmentNameDisambiguation(kotlin.main, "test")
        GradleKpmFragmentConfigureContextImpl(
            kotlin.main, GradleKpmDefaultFragmentDependencyConfigurationsFactory.create(kotlin.main, names), names
        )
    }

    private val dummyFragment by lazy {
        GradleKpmCommonFragmentFactory(kotlin.main).create("testFragment")
    }

    @Test
    fun `test withProvider`() {
        val testDefinition = GradleKpmConfigurationSetup<GradleKpmFragment>(
            provider = GradleKpmConfigurationProvider { project.configurations.create("a") }
        ).withConfigurationProvider { project.configurations.create("b") }

        assertEquals(
            "b", testDefinition.provider.getConfiguration(testContext).name,
            "Expected configuration 'b' to be provided"
        )

        assertNull(
            project.configurations.findByName("a"),
            "Expected configuration 'a' never being created"
        )

        assertEquals(
            "c", testDefinition.withConfigurationProvider { project.configurations.create("c") }
                .provider.getConfiguration(testContext).name,
            "Expected configuration 'c' to be provided"
        )
    }

    @Test
    fun `test attributes + attributes`() {
        val testAttribute1 = Attribute.of("testAttribute1", String::class.java)
        val testAttribute2 = Attribute.of("testAttribute2", String::class.java)
        val testConfiguration = project.configurations.create("dummy")

        val fragmentAttributes1 = GradleKpmConfigurationAttributesSetup<GradleKpmFragment> {
            assertSame(testConfiguration.attributes, attributes)
            assertNull(attributes.getAttribute(testAttribute1))
            assertNull(attributes.getAttribute(testAttribute2))
            attribute(testAttribute1, "value1")
        }

        val fragmentAttributes2 = GradleKpmConfigurationAttributesSetup<GradleKpmFragment> {
            assertSame(testConfiguration.attributes, attributes)
            assertEquals(attributes.getAttribute(testAttribute1), "value1")
            assertNull(attributes.getAttribute(testAttribute2))
            attribute(testAttribute2, "value2")
        }

        val composite = fragmentAttributes1 + fragmentAttributes2
        if (composite !is GradleKpmCompositeConfigurationAttributesSetup)
            fail("Expected ${GradleKpmCompositeConfigurationAttributesSetup::class} but found $composite")

        assertEquals(
            listOf(fragmentAttributes1, fragmentAttributes2), composite.children
        )

        composite.setupAttributes(testConfiguration.attributes, dummyFragment)

        assertEquals(
            "value1", testConfiguration.attributes.getAttribute(testAttribute1),
            "Expected 'testAttribute1' to be set"
        )

        assertEquals(
            "value2", testConfiguration.attributes.getAttribute(testAttribute2),
            "Expected 'testAttribute2' to be set"
        )
    }

    @Test
    fun `test definition + attributes`() {
        val definition = GradleKpmConfigurationSetup<GradleKpmFragment>(
            provider = GradleKpmConfigurationProvider { project.configurations.create("a") }
        )

        val attributes1 = GradleKpmConfigurationAttributesSetup<GradleKpmFragment> { }
        val attributes2 = GradleKpmConfigurationAttributesSetup<GradleKpmFragment> { }

        val newDefinition = definition + attributes1 + attributes2
        assertEquals(
            listOf(attributes1, attributes2), (newDefinition.attributes as? GradleKpmCompositeConfigurationAttributesSetup)?.children,
            "Expected 'newDefinition' to list the two added attributes"
        )
    }

    @Test
    fun `test artifacts + artifacts`() {
        val artifacts1 = GradleKpmConfigurationArtifactsSetup<GradleKpmFragment> {
            artifact(project.file("artifact1.jar"))
        }

        val artifacts2 = GradleKpmConfigurationArtifactsSetup<GradleKpmFragment> {
            artifact(project.file("artifact2-a.jar"))
            artifact(project.file("artifact2-b.jar"))
        }

        val composite = artifacts1 + artifacts2
        if (composite !is CompositeFragmentConfigurationArtifactsSetup) {
            fail("Expected ${CompositeFragmentConfigurationArtifactsSetup::class.java}, but found $composite")
        }

        assertEquals(
            listOf(artifacts1, artifacts2), composite.children
        )

        project.configurations.create("dummy").apply {
            composite.setupArtifacts(outgoing, dummyFragment)
            assertEquals(
                project.files("artifact1.jar", "artifact2-a.jar", "artifact2-b.jar").toSet(),
                outgoing.artifacts.files.toSet()
            )
        }
    }

    @Test
    fun `test definition + artifacts`() {
        val definition = GradleKpmConfigurationSetup<GradleKpmFragment>(
            provider = GradleKpmConfigurationProvider { throw NotImplementedError() }
        )

        val artifacts1 = GradleKpmConfigurationArtifactsSetup<GradleKpmFragment> {}
        val artifacts2 = GradleKpmConfigurationArtifactsSetup<GradleKpmFragment> {}

        val newDefinition = definition + artifacts1 + artifacts2
        assertEquals(
            listOf(artifacts1, artifacts2), (newDefinition.artifacts as? CompositeFragmentConfigurationArtifactsSetup)?.children,
            "Expected 'newDefinition' to list new added artifacts"
        )
    }

    @Test
    fun `test capability + capability`() {
        val capabilities1 = GradleKpmConfigurationCapabilitiesSetup<GradleKpmFragment> {
            capability("capability1")
        }

        val capabilities2 = GradleKpmConfigurationCapabilitiesSetup<GradleKpmFragment> {
            capability("capability2")
        }

        val composite = capabilities1 + capabilities2
        if (composite !is CompositeKpmCapabilitiesSetup) {
            fail("Expected ${CompositeKpmCapabilitiesSetup::class}, but found $composite")
        }

        assertEquals(
            listOf(capabilities1, capabilities2), composite.children
        )

        val testCapabilitiesContainer = object : GradleKpmConfigurationCapabilitiesSetup.CapabilitiesContainer {
            val setCapabilities = mutableListOf<Any>()
            override fun capability(notation: Any) {
                setCapabilities.add(notation)
            }
        }

        composite.setCapabilities(testCapabilitiesContainer, dummyFragment)

        assertEquals(
            listOf<Any>("capability1", "capability2"), testCapabilitiesContainer.setCapabilities,
            "Expected 'dummyContext' to receive all capabilities"
        )
    }

    @Test
    fun `test definition + capability`() {
        val definition = GradleKpmConfigurationSetup<GradleKpmFragment>(
            provider = GradleKpmConfigurationProvider { throw NotImplementedError() }
        )

        val capabilities1 = GradleKpmConfigurationCapabilitiesSetup<GradleKpmFragment> {}
        val capabilities2 = GradleKpmConfigurationCapabilitiesSetup<GradleKpmFragment> {}
        val newDefinition = definition + capabilities1 + capabilities2

        assertEquals(
            listOf(capabilities1, capabilities2), (newDefinition.capabilities as? CompositeKpmCapabilitiesSetup)?.children,
            "Expected 'newDefinition' to list all added capabilities"
        )
    }
}

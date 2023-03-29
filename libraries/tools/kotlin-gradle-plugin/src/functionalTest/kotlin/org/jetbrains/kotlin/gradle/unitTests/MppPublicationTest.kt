/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.util.addBuildEventsListenerRegistryMock
import org.jetbrains.kotlin.gradle.util.disableLegacyWarning
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import kotlin.test.*

class MppPublicationTest {

    private val project = ProjectBuilder.builder().build().also {
        addBuildEventsListenerRegistryMock(it)
        disableLegacyWarning(it)
    } as ProjectInternal

    init {
        project.plugins.apply("kotlin-multiplatform")
        project.plugins.apply("maven-publish")
    }

    private val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)

    init {
        kotlin.jvm()
        kotlin.js().nodejs()
    }

    @Test
    fun `contains kotlinMultiplatform publication`() {
        project.evaluate()
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        publishing.publications
            .withType(MavenPublication::class.java)
            .findByName("kotlinMultiplatform") ?: fail("Missing 'kotlinMultiplatform' publication")
    }


    @Test
    fun `all publication contains sourcesJar`() {
        project.evaluate()
        val publishing = project.extensions.getByType(PublishingExtension::class.java)
        publishing.publications
            .filterIsInstance<MavenPublication>()
            .forEach { publication ->
                val sources = publication.artifacts.filter { artifact -> artifact.classifier == "sources" }
                assertTrue(sources.isNotEmpty(), "Expected at least one sources artifact for ${publication.name}")
            }
    }

    @Test
    fun `jvm sources elements has same attributes as java sources elements from default java gradle plugin`() {
        val javaProject = ProjectBuilder.builder().build().run {
            // Given
            plugins.apply("java")
            plugins.apply("maven-publish")

            extensions.getByType(JavaPluginExtension::class.java).run {
                withSourcesJar()
            }

            // When
            this as ProjectInternal
            this.evaluate()
        }

        val javaSourcesElementsAttributes = javaProject
            .components
            .getByName("java")
            .attributesOfUsageContext("sourcesElements")

        project.evaluate()

        val kotlinComponent = project.components.getByName("kotlin") as ComponentWithVariants
        val jvmComponent = kotlinComponent.variants.first { it.name == "jvm" }
        val jvmSourcesElementsAttributes = jvmComponent.attributesOfUsageContext("jvmSourcesElements-published")

        val extraExpectedAttributes = mapOf(               // target disambiguation attribute
            "org.jetbrains.kotlin.platform.type" to "jvm", // is set by default by kgp, when it sets usage as `java-runtime-jars`
            "org.gradle.libraryelements" to "jar"          // see [KotlinUsages.producerApiUsage]
        )

        val expectedAttributes = javaSourcesElementsAttributes.toMapOfStrings() + extraExpectedAttributes
        val actualAttributes = jvmSourcesElementsAttributes.toMapOfStrings()
        assertEquals(expectedAttributes, actualAttributes)
    }

    @Test
    fun `sources elements includes user-defined attributes`() {
        val userAttribute = Attribute.of("userAttribute", String::class.java)
        kotlin.run {
            targets.all { target -> target.attributes { attribute(userAttribute, target.name) } }
            js(IR)
            linuxX64()
            iosX64()
        }

        project.evaluate()

        val kotlinComponent = project.components.getByName("kotlin") as ComponentWithVariants
        val sourcesElements = listOf("jvm", "js", "linuxX64", "iosX64")
            .map { targetName ->
                targetName to kotlinComponent.variants
                    .first { it.name == targetName }
                    .attributesOfUsageContext("${targetName}SourcesElements-published")
            }

        for ((targetName, sourceElements) in sourcesElements) {
            assertTrue(
                message = "Sources Elements of target $targetName doesn't have 'userAttribute'"
            ) { sourceElements.attributes.toMapOfStrings().containsKey("userAttribute") }
        }
    }

    @Test
    fun `sources elements should not have any dependencies`() {
        project.evaluate()
        project.configurations
            .filter { it.name.toLowerCaseAsciiOnly().contains("sourceselements") }
            .forEach { configuration ->
                if (configuration.dependencies.isNotEmpty()) fail("Configuration $configuration should not have dependencies")
            }
    }


    @Test
    fun `sourcesJar task should be available during configuration time`() {
        kotlin.linuxX64("linux")

        val sourcesJars = listOf(
            "sourcesJar", // sources of common source sets i.e. root module
            "jvmSourcesJar",
            "jsSourcesJar",
            "linuxSourcesJar"
        )

        for (sourcesJarTaskName in sourcesJars) {
            val sourcesJar = project.tasks.findByName(sourcesJarTaskName)
            assertNotNull(sourcesJar, "Task '$sourcesJarTaskName' should exist during project configuration time")
        }
    }

    @Test
    fun `test withSourcesJar DSL on extension level`() {
        kotlin.linuxX64()

        kotlin.withSourcesJar(publish = false)

        for (target in kotlin.targets) {
            assertFalse(target.internal.isSourcesPublishable)
        }
    }

    @Test
    fun `test withSourcesJar DSL on target level`() {
        kotlin.linuxX64("linux") {
            withSourcesJar(publish = false)
        }

        for (target in kotlin.targets) {
            if (target.name == "linux") {
                assertFalse(target.internal.isSourcesPublishable)
            } else {
                assertTrue(target.internal.isSourcesPublishable)
            }
        }
    }

    @Test
    fun `test that sourcesJar tasks still exist even if sources should not be published`() {
        kotlin.linuxX64("linux")
        kotlin.withSourcesJar(publish = false)

        val sourcesJars = listOf(
            "sourcesJar", // sources of common source sets i.e. root module
            "jvmSourcesJar",
            "jsSourcesJar",
            "linuxSourcesJar"
        )

        for (sourcesJarTaskName in sourcesJars) {
            val sourcesJar = project.tasks.findByName(sourcesJarTaskName)
            assertNotNull(sourcesJar, "Task '$sourcesJarTaskName' should exist during project configuration time")
        }
    }

    @Test
    fun `test that no sourcesElements should be consumable when sources are published`() {
        kotlin.linuxX64("linux")
        kotlin.withSourcesJar(publish = true)

        project.evaluate()
        kotlin.targets.forEach {
            val sourcesElements = project.configurations.getByName(it.sourcesElementsConfigurationName)
            if (!sourcesElements.isCanBeConsumed) fail("Configuration '${it.sourcesElementsConfigurationName}' should be consumable")
        }
    }

    @Test
    fun `test that no sourcesElements should be consumable when sources are not published`() {
        kotlin.linuxX64("linux")
        kotlin.withSourcesJar(publish = false)

        project.evaluate()
        kotlin.targets.forEach {
            val sourcesElements = project.configurations.getByName(it.sourcesElementsConfigurationName)
            if (sourcesElements.isCanBeConsumed) fail("Configuration '${it.sourcesElementsConfigurationName}' should not be consumable")
        }
    }

    private fun SoftwareComponent.attributesOfUsageContext(usageContextName: String): AttributeContainer {
        this as SoftwareComponentInternal
        return usages.first { it.name == usageContextName }.attributes
    }

    private fun AttributeContainer.toMapOfStrings(): Map<String, String> = keySet()
        .associate { key -> key.name to getAttribute(key).toString() }

}

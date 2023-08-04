/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.DocsType
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.hierarchy.KotlinSourceSetTreeClassifier
import org.jetbrains.kotlin.gradle.plugin.hierarchy.orNull
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinCompilationDescriptor.CompilationAssociator
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinCompilationDescriptor.CompilationFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.external.ExternalKotlinCompilationDescriptorBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.external.createExternalKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.kotlinProjectStructureMetadata
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.utils.property
import org.jetbrains.kotlin.gradle.utils.toMap
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import kotlin.test.*

class ExternalKotlinTargetApiTests {

    val project = buildProjectWithMPP()
    val kotlin = project.multiplatformExtension


    @Test
    fun `test - sourceSetClassifier - default`() = project.runLifecycleAwareTest {
        val target = kotlin.createExternalKotlinTarget<FakeTarget> { defaults() }
        val mainCompilation = target.createCompilation<FakeCompilation> { defaults(kotlin) }
        val fakeCompilation = target.createCompilation<FakeCompilation> {
            defaults(kotlin)
            compilationName = "fake"
            defaultSourceSet = kotlin.sourceSets.create("fake")
        }

        assertEquals(KotlinSourceSetTree.main, KotlinSourceSetTree.orNull(mainCompilation))
        assertEquals(KotlinSourceSetTree("fake"), KotlinSourceSetTree.orNull(fakeCompilation))
    }

    @Test
    fun `test - sourceSetClassifier - custom name`() = project.runLifecycleAwareTest {
        val target = kotlin.createExternalKotlinTarget<FakeTarget> { defaults() }
        val compilation = target.createCompilation<FakeCompilation> {
            defaults(kotlin)
            sourceSetTreeClassifierV2 = KotlinSourceSetTreeClassifier.Name("mySourceSetTree")
        }

        assertEquals(KotlinSourceSetTree("mySourceSetTree"), KotlinSourceSetTree.orNull(compilation))
    }

    @Test
    fun `test - sourceSetClassifier - custom property`() = project.runLifecycleAwareTest {
        val kotlin = multiplatformExtension
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
    fun `test - compilation associator - default`() {
        val target = kotlin.createExternalKotlinTarget<FakeTarget> { defaults() }

        val mainCompilation = target.createCompilation<FakeCompilation> {
            assertEquals(
                CompilationAssociator.default, compilationAssociator,
                "Expected CompilationAssociator.default being set in ${ExternalKotlinCompilationDescriptorBuilder::class.simpleName}"
            )

            defaults(kotlin, "fakeMain")
            compileTaskName = "main"
        }

        val auxCompilation = target.createCompilation<FakeCompilation> {
            defaults(kotlin, "fakeAux")
            compilationName = "aux"
        }

        auxCompilation.associateWith(mainCompilation)
        assertEquals(setOf(mainCompilation), auxCompilation.associatedCompilations.toSet())
    }

    @Test
    fun `test - compilation associator - custom`() {
        val target = kotlin.createExternalKotlinTarget<FakeTarget> { defaults() }
        val testTraceKey = extrasKeyOf<String>("testTrace")

        val compilationAssociator = CompilationAssociator<FakeCompilation> { auxiliary, main ->
            auxiliary.extras[testTraceKey] = "aux"
            main.extras[testTraceKey] = "main"
        }

        val mainCompilation = target.createCompilation<FakeCompilation> {
            defaults(kotlin, "fakeMain")
            this.compilationAssociator = compilationAssociator
            this.compileTaskName = "main"
        }

        val auxCompilation = target.createCompilation<FakeCompilation> {
            defaults(kotlin, "fakeAux")
            this.compilationName = "aux"
            this.compilationAssociator = compilationAssociator
        }

        auxCompilation.associateWith(mainCompilation)

        assertEquals("aux", auxCompilation.extras[testTraceKey])
        assertEquals("main", mainCompilation.extras[testTraceKey])
    }

    @Test
    fun `test - sourcesElements - default configuration`() = project.runLifecycleAwareTest {
        val target = kotlin.createExternalKotlinTarget<FakeTarget> { defaults() }
        target.createCompilation<FakeCompilation> { defaults(kotlin) }

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
    fun `test - sourcesElements - configure`() = project.runLifecycleAwareTest {
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
        target.createCompilation<FakeCompilation> { defaults(kotlin) }

        /* Check traces left before */
        assertEquals("sourcesElements", target.sourcesElementsConfiguration.attributes.getAttribute(testAttribute))
        assertEquals("sourcesElements-published", target.sourcesElementsPublishedConfiguration.attributes.getAttribute(testAttribute))
    }

    @Test
    fun `test - sourcesElements - publication`() = project.runLifecycleAwareTest {
        val target = kotlin.createExternalKotlinTarget<FakeTarget> { defaults() }

        // Main/publishable compilation is required to have usages
        target.createCompilation<FakeCompilation> { defaults(kotlin) }

        KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()
        val component = target.delegate.kotlinComponents.singleOrNull() ?: fail("Expected single 'component' for external target")

        component.internal.usages.find { it.dependencyConfigurationName == target.sourcesElementsPublishedConfiguration.name }
            ?: fail("Missing sourcesElements usage")
    }

    @Test
    fun `test - sourcesElements - publication - withSourcesJar set to false`() = project.runLifecycleAwareTest {
        val target = kotlin.createExternalKotlinTarget<FakeTarget> { defaults() }
        target.withSourcesJar(false)
        target.createCompilation<FakeCompilation> { defaults(kotlin) }

        val component = target.delegate.components.singleOrNull() ?: fail("Expected single 'component' for external target")
        val sourcesUsage = component.usages.find { it.name.contains("sources", true) }
        if (sourcesUsage != null) {
            fail("Unexpected usage '${sourcesUsage.name} in target publication")
        }
    }

    @Test
    fun `test - gradle usage component contains the same usages as kotlin component`() = project.runLifecycleAwareTest {
        val target = kotlin.createExternalKotlinTarget<FakeTarget> { defaults() }
        target.createCompilation<FakeCompilation> { defaults(kotlin) }

        val kotlinComponent = target.delegate.kotlinComponents.singleOrNull()
            ?: fail("Expected single 'kotlinComponent' for external target")

        val gradleComponent = target.delegate.components.singleOrNull()
            ?: fail("Expected single 'component' for external target")

        configurationResult.await()

        val kotlinUsagesNames = kotlinComponent.internal.usages.map { it.dependencyConfigurationName }
        val gradleUsagesNames = (gradleComponent as SoftwareComponentInternal).usages.map { it.name }

        if (kotlinUsagesNames.toSet() != gradleUsagesNames.toSet())
            fail("Kotlin Usages ($kotlinUsagesNames) from Kotlin Component didn't match Usages from Gradle Component ($gradleUsagesNames)")
    }

    @Test
    fun `test - gradle usage component contains the same usages as kotlin component after project evaluation`() {
        val target = kotlin.createExternalKotlinTarget<FakeTarget> { defaults() }
        target.createCompilation<FakeCompilation> { defaults(kotlin) }
        project.evaluate()

        val kotlinComponent = target.kotlinComponents.singleOrNull() ?: fail("Expected single 'kotlinComponent' for external target")
        val gradleComponent = target.components.singleOrNull() ?: fail("Expected single 'component' for external target")

        if (kotlinComponent !is SoftwareComponentInternal) error("Expected 'kotlinComponent' to be 'SoftwareComponentInternal'")

        // When kotlin usages is available corresponding gradle component usages should be also available
        val kotlinUsagesNames = kotlinComponent.usages.map { it.name }
        val gradleUsagesNames = gradleComponent.usages.map { it.name }

        if (kotlinUsagesNames.toSet() != gradleUsagesNames.toSet())
            fail("Kotlin Usages ($kotlinUsagesNames) from Kotlin Component didn't match Usages from Gradle Component ($gradleUsagesNames)")
    }

    @Test
    fun `test - project structure metadata contains external target variants`() {
        val target = kotlin.createExternalKotlinTarget<FakeTarget> { defaults() }
        val mainCompilation = target.createCompilation<FakeCompilation> { defaults(kotlin) }
        val testCompilation = target.createCompilation<FakeCompilation> {
            defaults(kotlin)
            compilationName = "test"
            defaultSourceSet = kotlin.sourceSets.create("fakeTest")
        }

        kotlin.linuxX64()
        kotlin.macosX64()

        fun KotlinHierarchyBuilder.withFakeTarget() = withCompilations { it == mainCompilation || it == testCompilation }
        kotlin.applyHierarchyTemplate {
            sourceSetTrees(KotlinSourceSetTree.main, KotlinSourceSetTree.test)
            common {
                group("linuxAndFake") {
                    withLinux()
                    withFakeTarget()
                }

                withMacos()
            }
        }

        project.evaluate()

        val projectStructureMetadata = kotlin.kotlinProjectStructureMetadata

        val fakeApiElementsSourceSets = projectStructureMetadata.sourceSetNamesByVariantName["fakeApiElements"]
            ?: fail("Expected 'fakeApiElements' variant")
        assertEquals(setOf("commonMain", "linuxAndFakeMain"), fakeApiElementsSourceSets)

        val fakeRuntimeElementsSourceSets = projectStructureMetadata.sourceSetNamesByVariantName["fakeRuntimeElements"]
            ?: fail("Expected 'fakeRuntimeElements' variant")
        assertEquals(setOf("commonMain", "linuxAndFakeMain"), fakeRuntimeElementsSourceSets)
    }

    @Test
    fun `test - published component contain user defined attributes`() {
        val userAttribute = Attribute.of("userAttribute", String::class.java)

        val target = kotlin.createExternalKotlinTarget<FakeTarget> { defaults() }
        target.createCompilation<FakeCompilation> { defaults(kotlin) }
        target.attributes.attribute(userAttribute, "foo")

        project.evaluate()

        val component = target.internal.components.singleOrNull() ?: fail("Expected single component")
        if (component.usages.isEmpty()) fail("Expected at least one usage")
        component.usages.forEach { usage ->
            if (!usage.attributes.contains(userAttribute)) fail("Usage '${usage.name}' does not contain user attribute")
            assertEquals("foo", usage.attributes.getAttribute(userAttribute))
        }
    }
}

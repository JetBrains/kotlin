/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.copy.SingleParentCopySpec
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.configurationResult
import org.jetbrains.kotlin.gradle.plugin.hierarchy.KotlinSourceSetTreeClassifier
import org.jetbrains.kotlin.gradle.plugin.mpp.external.*
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.tooling.core.withClosure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.fail

class ExternalKotlinTargetSourcesJarUtilsTest {
    @Test
    fun `test - includeSources`() = buildProjectWithMPP().runLifecycleAwareTest {
        val target = multiplatformExtension.createExternalKotlinTarget<FakeTarget> { defaults() }
        val compilation = target.createCompilation<FakeCompilation> {
            defaults(multiplatformExtension)
            defaultSourceSet = multiplatformExtension.sourceSets.maybeCreate("testSourceSet")
            sourceSetTreeClassifierV2 = KotlinSourceSetTreeClassifier.Value(KotlinSourceSetTree.main)
        }

        val jar = tasks.create<Jar>("forTest")
        jar.includeSources(compilation)
        configurationResult.await()

        val allSpecs = jar.rootSpec.withClosure { it.children }

        val testSourceSetSpec = allSpecs.filterIsInstance<SingleParentCopySpec>().find { it.destPath == "testSourceSet" }
            ?: fail("Missing 'testSourceSet' in jar")

        assertEquals(
            setOf(file("src/testSourceSet/kotlin")),
            (testSourceSetSpec.sourcePaths.single() as SourceDirectorySet).srcDirs
        )

        val commonMainSpec = allSpecs.filterIsInstance<SingleParentCopySpec>().find { it.destPath == "commonMain" }
            ?: fail("Missing 'commonMain' in jar")

        assertEquals(
            setOf(file("src/commonMain/kotlin")),
            (commonMainSpec.sourcePaths.single() as SourceDirectorySet).srcDirs
        )
    }

    @Test
    fun `test - sourcesJarTask`() = buildProjectWithMPP().runLifecycleAwareTest {
        val target = multiplatformExtension.createExternalKotlinTarget<FakeTarget> { defaults() }

        val mainCompilation = target.createCompilation<FakeCompilation> {
            defaults(multiplatformExtension, "mainFake")
            compilationName = "main"
        }

        val auxCompilation = target.createCompilation<FakeCompilation> {
            defaults(multiplatformExtension, "auxFake")
            compilationName = "aux"
        }

        val mainJar = target.sourcesJarTask(mainCompilation)
        assertEquals(mainJar, target.sourcesJarTask(mainCompilation))
        assertEquals("fakeSourcesJar", mainJar.name)

        val auxJar = target.sourcesJarTask(auxCompilation)
        assertNotEquals(mainJar, auxJar)
        assertEquals(auxJar, target.sourcesJarTask(auxCompilation))
        assertEquals("fakeAuxSourcesJar", auxJar.name)
    }

    @Test
    fun `test - publishSources`() = buildProjectWithMPP().runLifecycleAwareTest {
        val target = multiplatformExtension.createExternalKotlinTarget<FakeTarget> { defaults() }

        val mainCompilation = target.createCompilation<FakeCompilation> {
            defaults(multiplatformExtension)
            compilationName = "main"
        }

        target.publishSources(mainCompilation)
        configurationResult.await()

        val sourcesArtifacts = target.sourcesElementsPublishedConfiguration.outgoing.artifacts.filter { artifact ->
            artifact.classifier == "sources"
        }

        if (sourcesArtifacts.isEmpty()) fail("Missing 'sources' artifacts")
        if (sourcesArtifacts.size > 1) fail("Expected single 'sources' artifact. Found $sourcesArtifacts")
        val sourcesArtifact = sourcesArtifacts.single()
        assertEquals(target.sourcesJarTask(mainCompilation).get().archiveFile.get().asFile, sourcesArtifact.file)
    }
}
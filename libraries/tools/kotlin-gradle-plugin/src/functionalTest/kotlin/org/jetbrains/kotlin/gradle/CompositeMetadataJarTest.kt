/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle

import org.gradle.kotlin.dsl.support.zipTo
import org.jetbrains.kotlin.gradle.plugin.mpp.CompositeMetadataJar
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinProjectStructureMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.ModuleDependencyIdentifier
import org.jetbrains.kotlin.gradle.plugin.mpp.SourceSetMetadataLayout
import org.jetbrains.kotlin.gradle.plugin.mpp.SourceSetMetadataLayout.KLIB
import org.jetbrains.kotlin.gradle.plugin.mpp.SourceSetMetadataLayout.METADATA
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class CompositeMetadataJarTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `empty jar - get metadata - returns file`() {
        val primaryArtifactContent = temporaryFolder.newFolder()
        val primaryArtifactFile = temporaryFolder.newFile("metadata.jar")
        zipTo(primaryArtifactFile, primaryArtifactContent)
        assertTrue(primaryArtifactFile.isFile, "Expected primaryArtifactFile.isFile")

        val metadataJar = CompositeMetadataJar(
            moduleIdentifier = "test-id",
            projectStructureMetadata = createProjectStructureMetadata(
                sourceSetBinaryLayout = mapOf("testSourceSetName" to KLIB),
                sourceSetCInteropMetadataDirectory = mapOf("testSourceSetName" to "cinterop/testSourceSetName")
            ),
            primaryArtifactFile = primaryArtifactFile,
            hostSpecificArtifactsBySourceSet = emptyMap()
        )

        val metadataOutputDirectory = temporaryFolder.newFolder()
        val metadataFile = metadataJar.getSourceSetCompiledMetadata("testSourceSetName", metadataOutputDirectory, true)

        assertEquals(
            File("test-id/test-id-testSourceSetName.klib"),
            metadataFile.relativeTo(metadataOutputDirectory)
        )
    }

    @Test
    fun `empty jar - no cinterop metadata directory - get metadata - returns file`() {
        val primaryArtifactContent = temporaryFolder.newFolder()
        val primaryArtifactFile = temporaryFolder.newFile("metadata.jar")
        zipTo(primaryArtifactFile, primaryArtifactContent)
        assertTrue(primaryArtifactFile.isFile, "Expected primaryArtifactFile.isFile")

        val metadataJar = CompositeMetadataJar(
            moduleIdentifier = "test-id",
            projectStructureMetadata = createProjectStructureMetadata(
                sourceSetBinaryLayout = mapOf("testSourceSetName" to KLIB),
                sourceSetCInteropMetadataDirectory = emptyMap(),
            ),
            primaryArtifactFile = primaryArtifactFile,
            hostSpecificArtifactsBySourceSet = emptyMap()
        )

        val metadataOutputDirectory = temporaryFolder.newFolder()
        val metadataFile = metadataJar.getSourceSetCompiledMetadata("testSourceSetName", metadataOutputDirectory, true)

        assertEquals(
            File("test-id/test-id-testSourceSetName.klib"),
            metadataFile.relativeTo(metadataOutputDirectory)
        )
    }

    @Test
    fun `empty jar - get cinterop metadata - returns emptySet`() {
        val primaryArtifactContent = temporaryFolder.newFolder()
        val primaryArtifactFile = temporaryFolder.newFile("metadata.jar")
        zipTo(primaryArtifactFile, primaryArtifactContent)
        assertTrue(primaryArtifactFile.isFile, "Expected primaryArtifactFile.isFile")

        val metadataJar = CompositeMetadataJar(
            moduleIdentifier = "test-id",
            projectStructureMetadata = createProjectStructureMetadata(),
            primaryArtifactFile = primaryArtifactFile,
            hostSpecificArtifactsBySourceSet = emptyMap()
        )

        val metadataOutputDirectory = temporaryFolder.newFolder()
        val metadataFiles = metadataJar.getSourceSetCInteropMetadata("testSourceSetName", metadataOutputDirectory, true)

        assertEquals(
            emptySet(), metadataFiles,
            "Expected no cinterop metadata files discovered in jar"
        )
    }

    @Test
    fun `get metadata`() {
        /* Setup Artifact content */
        val primaryArtifactContent = temporaryFolder.newFolder()

        primaryArtifactContent.resolve("sourceSetA/sourceSetAStub1.txt")
            .withParentDirectoriesCreated()
            .writeText("Content of sourceSetA stub1")

        primaryArtifactContent.resolve("sourceSetA/nested/sourceSetAStub2.txt")
            .withParentDirectoriesCreated()
            .writeText("Content of sourceSetB stub2")

        primaryArtifactContent.resolve("sourceSetB/sourceSetBStub1.txt")
            .withParentDirectoriesCreated()
            .writeText("Content of sourceSetB stub1")

        primaryArtifactContent.resolve("sourceSetB/nested/sourceSetBStub2.txt")
            .withParentDirectoriesCreated()
            .writeText("Content of sourceSetB stub2")

        /* Create metadata jar */
        val primaryArtifactFile = temporaryFolder.newFile("metadata.jar")
        zipTo(primaryArtifactFile, primaryArtifactContent)

        val metadataJar = CompositeMetadataJar(
            moduleIdentifier = "test-id",
            projectStructureMetadata = createProjectStructureMetadata(
                sourceSetBinaryLayout = mapOf("sourceSetA" to KLIB, "sourceSetB" to METADATA)
            ),
            primaryArtifactFile = primaryArtifactFile,
            hostSpecificArtifactsBySourceSet = emptyMap()
        )

        /* Extract and assert sourceSetA */
        val metadataOutputDirectory = temporaryFolder.newFolder()

        val sourceSetAMetadataFile = metadataJar.getSourceSetCompiledMetadata("sourceSetA", metadataOutputDirectory, true)
        assertTrue(sourceSetAMetadataFile.isFile, "Expected sourceSetAMetadataFile.isFile")
        assertEquals(
            KLIB.archiveExtension, sourceSetAMetadataFile.extension,
            "Expected correct archiveExtension for extracted sourceSetA"
        )
        assertZipContentEquals(
            temporaryFolder,
            primaryArtifactContent.resolve("sourceSetA"), sourceSetAMetadataFile,
            "Expected correct content of extracted 'sourceSetA'"
        )

        /* Extract and assert sourceSetB */
        val sourceSetBMetadataFile = metadataJar.getSourceSetCompiledMetadata("sourceSetB", metadataOutputDirectory, true)
        assertTrue(sourceSetAMetadataFile.isFile, "Expected sourceSetBMetadataFile.isFile")
        assertEquals(
            METADATA.archiveExtension, sourceSetBMetadataFile.extension,
            "Expected correct archiveExtension for extracted sourceSetB"
        )
        assertZipContentEquals(
            temporaryFolder,
            primaryArtifactContent.resolve("sourceSetB"), sourceSetBMetadataFile,
            "Expected correct content of extracted 'sourceSetA'"
        )
    }

    @Test
    fun `get cinterop metadata`() {
        /* Setup Artifact content */
        val primaryArtifactContent = temporaryFolder.newFolder()

        primaryArtifactContent.resolve("sourceSetA-cinterop/interopA0/stub0")
            .withParentDirectoriesCreated()
            .writeText("stub0 content")

        primaryArtifactContent.resolve("sourceSetA-cinterop/interopA0/nested/stub1")
            .withParentDirectoriesCreated()
            .writeText("stub1 content")

        primaryArtifactContent.resolve("sourceSetA-cinterop/interopA1/stub2")
            .withParentDirectoriesCreated()
            .writeText("stub2 content")

        primaryArtifactContent.resolve("nested/sourceSetB/interops/interopB0/stub3")
            .withParentDirectoriesCreated()
            .writeText("stub3 content")

        /* Create metadata jar */
        val primaryArtifactFile = temporaryFolder.newFile("metadata.jar")
        zipTo(primaryArtifactFile, primaryArtifactContent)

        val metadataJar = CompositeMetadataJar(
            moduleIdentifier = "test-id",
            projectStructureMetadata = createProjectStructureMetadata(
                sourceSetCInteropMetadataDirectory = mapOf(
                    "sourceSetA" to "sourceSetA-cinterop",
                    "sourceSetB" to "nested/sourceSetB/interops/"
                )
            ),
            primaryArtifactFile = primaryArtifactFile,
            hostSpecificArtifactsBySourceSet = emptyMap()
        )

        /* Assertions on sourceSetA */
        run {
            val sourceSetAOutputDirectory = temporaryFolder.newFolder()
            val sourceSetAInteropMetadataFiles = metadataJar.getSourceSetCInteropMetadata("sourceSetA", sourceSetAOutputDirectory, true)
            assertEquals(2, sourceSetAInteropMetadataFiles.size, "Expected 2 cinterops in sourceSetA")

            /* Assertions on interopA0 */
            run {
                val interopA0MetadataFile = sourceSetAInteropMetadataFiles.firstOrNull { it.name == "interopA0.klib" }
                    ?: fail("Failed to find 'interopA0.klib'")
                assertZipContentEquals(
                    temporaryFolder,
                    primaryArtifactContent.resolve("sourceSetA-cinterop/interopA0"), interopA0MetadataFile,
                    "Expected correct content for extracted 'interopA0'"
                )
            }

            /* Assertions on interopA1 */
            run {
                val interopA1MetadataFile = sourceSetAInteropMetadataFiles.firstOrNull { it.name == "interopA1.klib" }
                    ?: fail("Failed to find 'interopA1.klib")
                assertZipContentEquals(
                    temporaryFolder,
                    primaryArtifactContent.resolve("sourceSetA-cinterop/interopA1"), interopA1MetadataFile,
                    "Expected correct content for extracted 'interopA1'"
                )
            }
        }

        /* Assertions on sourceSetB */
        run {
            val sourceSetBOutputDirectory = temporaryFolder.newFolder()
            val sourceSetBInteropMetadataFiles = metadataJar.getSourceSetCInteropMetadata("sourceSetB", sourceSetBOutputDirectory, true)
            assertEquals(1, sourceSetBInteropMetadataFiles.size, "Expected only one cinterop in sourceSetB")

            val interopB0MetadataFile = sourceSetBInteropMetadataFiles.firstOrNull { it.name == "interopB0.klib" }
                ?: fail("Failed to find 'interopB0.klib'")
            assertZipContentEquals(
                temporaryFolder,
                primaryArtifactContent.resolve("nested/sourceSetB/interops/interopB0"), interopB0MetadataFile,
                "Expected correct content for extracted 'interopB0'"
            )
        }
    }
}

private fun createProjectStructureMetadata(
    sourceSetNamesByVariantName: Map<String, Set<String>> = emptyMap(),
    sourceSetsDependsOnRelation: Map<String, Set<String>> = emptyMap(),
    sourceSetCInteropMetadataDirectory: Map<String, String> = emptyMap(),
    sourceSetBinaryLayout: Map<String, SourceSetMetadataLayout> = emptyMap(),
    sourceSetModuleDependencies: Map<String, Set<ModuleDependencyIdentifier>> = emptyMap(),
    hostSpecificSourceSets: Set<String> = emptySet(),
    isPublishedAsRoot: Boolean = true,
): KotlinProjectStructureMetadata {
    return KotlinProjectStructureMetadata(
        sourceSetNamesByVariantName = sourceSetNamesByVariantName,
        sourceSetsDependsOnRelation = sourceSetsDependsOnRelation,
        sourceSetBinaryLayout = sourceSetBinaryLayout,
        sourceSetCInteropMetadataDirectory = sourceSetCInteropMetadataDirectory,
        sourceSetModuleDependencies = sourceSetModuleDependencies,
        hostSpecificSourceSets = hostSpecificSourceSets,
        isPublishedAsRoot = isPublishedAsRoot
    )
}

private fun File.withParentDirectoriesCreated(): File = apply { parentFile.mkdirs() }

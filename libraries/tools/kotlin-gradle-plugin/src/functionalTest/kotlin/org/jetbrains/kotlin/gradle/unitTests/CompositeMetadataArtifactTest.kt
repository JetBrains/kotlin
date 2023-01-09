/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.kotlin.dsl.support.unzipTo
import org.gradle.kotlin.dsl.support.zipTo
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.*

class CompositeMetadataArtifactTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `empty jar - contains no metadataBinary and no cinteropMetadataBinaries`() {
        val primaryArtifactContent = temporaryFolder.newFolder()
        val primaryArtifactFile = temporaryFolder.newFile("metadata.jar")
        zipTo(primaryArtifactFile, primaryArtifactContent)
        assertTrue(primaryArtifactFile.isFile, "Expected primaryArtifactFile.isFile")

        val metadataArtifact = CompositeMetadataArtifactImpl(
            moduleDependencyIdentifier = ModuleDependencyIdentifier("test-group", "test-module"),
            moduleDependencyVersion = "1.0.0",
            kotlinProjectStructureMetadata = createProjectStructureMetadata(
                sourceSetNames = mutableSetOf("testSourceSetName"),
                sourceSetBinaryLayout = mapOf("testSourceSetName" to SourceSetMetadataLayout.KLIB),
                sourceSetCInteropMetadataDirectory = mapOf("testSourceSetName" to "cinterop/testSourceSetName")
            ),
            primaryArtifactFile = primaryArtifactFile,
            hostSpecificArtifactFilesBySourceSetName = emptyMap()
        )


        metadataArtifact.read { artifactContent ->
            if (artifactContent.sourceSets.size != 1) fail("Expected one SourceSet in metadataArtifact")
            val sourceSetContent = artifactContent.sourceSets.first()
            assertEquals("testSourceSetName", sourceSetContent.sourceSetName)
            assertNull(sourceSetContent.metadataBinary, "Expected no 'metadataBinary' listed for SourceSet")

            if (sourceSetContent.cinteropMetadataBinaries.isNotEmpty()) {
                fail(
                    "Expected no 'cinteropMetadataBinaries' in 'testSourceSet'. " +
                            "Found ${sourceSetContent.cinteropMetadataBinaries.map { it.cinteropLibraryName }}"
                )
            }
        }
    }

    @Test
    fun `stub metadata library - can be unzipped`() {
        val testSourceSetName = "testSourceSetName"
        val primaryArtifactContent = temporaryFolder.newFolder()
        val stubFile = primaryArtifactContent.resolve(testSourceSetName).resolve("stub.txt")
        stubFile.parentFile.mkdirs()
        stubFile.writeText("stub!")

        val primaryArtifactFile = temporaryFolder.newFile("metadata.jar")
        zipTo(primaryArtifactFile, primaryArtifactContent)
        assertTrue(primaryArtifactFile.isFile, "Expected primaryArtifactFile.isFile")

        val metadataArtifact = CompositeMetadataArtifactImpl(
            moduleDependencyIdentifier = ModuleDependencyIdentifier("test-group", "test-module"),
            moduleDependencyVersion = "1.0.0",
            kotlinProjectStructureMetadata = createProjectStructureMetadata(
                sourceSetBinaryLayout = mapOf(testSourceSetName to SourceSetMetadataLayout.KLIB),
                sourceSetCInteropMetadataDirectory = emptyMap(),
            ),
            primaryArtifactFile = primaryArtifactFile,
            hostSpecificArtifactFilesBySourceSetName = emptyMap()
        )

        metadataArtifact.read { artifactContent ->
            if (artifactContent.sourceSets.size != 1)
                fail("Expected exactly one SourceSet in ${artifactContent.sourceSets.map { it.sourceSetName }}")

            val sourceSet = artifactContent.getSourceSet(testSourceSetName)
            val metadataOutputDirectory = temporaryFolder.newFolder()
            val metadataFile = metadataOutputDirectory.resolve("testSourceSet.klib")

            val metadataBinary = sourceSet.metadataBinary ?: fail("Missing metadataBinary for ${sourceSet.sourceSetName}")
            assertTrue(metadataBinary.copyTo(metadataFile), "Expected 'copyTo' to perform copy action")
            assertTrue(metadataFile.isFile)

            val unzippedMetadataFile = metadataOutputDirectory.resolve("unzipped")
            unzipTo(unzippedMetadataFile, metadataFile)

            assertEquals(setOf(unzippedMetadataFile.resolve("stub.txt")), unzippedMetadataFile.listFiles().orEmpty().toSet())
            assertEquals("stub!", unzippedMetadataFile.resolve("stub.txt").readText())
        }
    }

    @Test
    fun `empty jar - returns no cinteropMetadataBinaries`() {
        val primaryArtifactContent = temporaryFolder.newFolder()
        val primaryArtifactFile = temporaryFolder.newFile("metadata.jar")
        zipTo(primaryArtifactFile, primaryArtifactContent)
        assertTrue(primaryArtifactFile.isFile, "Expected primaryArtifactFile.isFile")

        val metadataArtifact = CompositeMetadataArtifactImpl(
            moduleDependencyIdentifier = ModuleDependencyIdentifier("test-group", "test-module"),
            moduleDependencyVersion = "1.0.0",
            kotlinProjectStructureMetadata = createProjectStructureMetadata(
                sourceSetNames = setOf("testSourceSetName")
            ),
            primaryArtifactFile = primaryArtifactFile,
            hostSpecificArtifactFilesBySourceSetName = emptyMap()
        )

        metadataArtifact.read { artifactContent ->
            val sourceSet = artifactContent.getSourceSet("testSourceSetName")
            assertEquals(listOf(sourceSet), artifactContent.sourceSets)

            assertEquals(emptyList(), sourceSet.cinteropMetadataBinaries, "Expected empty cinteropMetadataBinaries")
        }
    }

    @Test
    fun `copy metadataBinary`() {
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

        val metadataArtifact = CompositeMetadataArtifactImpl(
            moduleDependencyIdentifier = ModuleDependencyIdentifier("test-group", "test-module"),
            moduleDependencyVersion = "1.0.0",
            kotlinProjectStructureMetadata = createProjectStructureMetadata(
                sourceSetBinaryLayout = mapOf(
                    "sourceSetA" to SourceSetMetadataLayout.KLIB,
                    "sourceSetB" to SourceSetMetadataLayout.METADATA
                )
            ),
            primaryArtifactFile = primaryArtifactFile,
            hostSpecificArtifactFilesBySourceSetName = emptyMap()
        )

        metadataArtifact.read { artifactContent ->
            val metadataOutputDirectory = temporaryFolder.newFolder()

            /* Extract and assert sourceSetA */
            artifactContent.getSourceSet("sourceSetA").also { sourceSetA ->
                val sourceSetAMetadataFile = metadataOutputDirectory.resolve("sourceSetA.klib")
                assertNotNull(sourceSetA.metadataBinary).copyTo(sourceSetAMetadataFile)
                assertTrue(sourceSetAMetadataFile.isFile, "Expected sourceSetAMetadataFile.isFile")
                assertEquals(
                    SourceSetMetadataLayout.KLIB.archiveExtension, sourceSetAMetadataFile.extension,
                    "Expected correct archiveExtension for extracted sourceSetA"
                )
                assertZipContentEquals(
                    temporaryFolder,
                    primaryArtifactContent.resolve("sourceSetA"), sourceSetAMetadataFile,
                    "Expected correct content of extracted 'sourceSetA'"
                )
            }

            /* Extract and assert sourceSetB */
            artifactContent.getSourceSet("sourceSetB").also { sourceSetB ->
                val sourceSetBMetadataFile = metadataOutputDirectory.resolve("sourceSetB.jar")
                assertNotNull(sourceSetB.metadataBinary).copyTo(sourceSetBMetadataFile)
                assertTrue(sourceSetBMetadataFile.isFile, "Expected sourceSetBMetadataFile.isFile")
                assertEquals(
                    SourceSetMetadataLayout.METADATA.archiveExtension, sourceSetBMetadataFile.extension,
                    "Expected correct archiveExtension for extracted sourceSetB"
                )
                assertZipContentEquals(
                    temporaryFolder,
                    primaryArtifactContent.resolve("sourceSetB"), sourceSetBMetadataFile,
                    "Expected correct content of extracted 'sourceSetA'"
                )
            }
        }
    }

    @Test
    fun `copy cinteropMetadataBinaries`() {
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

        val metadataArtifact = CompositeMetadataArtifactImpl(
            moduleDependencyIdentifier = ModuleDependencyIdentifier("test-group", "test-module"),
            moduleDependencyVersion = "1.0.0",
            kotlinProjectStructureMetadata = createProjectStructureMetadata(
                sourceSetCInteropMetadataDirectory = mapOf(
                    "sourceSetA" to "sourceSetA-cinterop",
                    "sourceSetB" to "nested/sourceSetB/interops/"
                )
            ),
            primaryArtifactFile = primaryArtifactFile,
            hostSpecificArtifactFilesBySourceSetName = emptyMap()
        )

        metadataArtifact.read { artifactContent ->
            /* Assertions on sourceSetA */
            artifactContent.getSourceSet("sourceSetA").also { sourceSetA ->
                val sourceSetAOutputDirectory = temporaryFolder.newFolder()
                val sourceSetAInteropMetadataFiles = sourceSetA.cinteropMetadataBinaries.map { cinteropLibrary ->
                    sourceSetAOutputDirectory.resolve("${cinteropLibrary.cinteropLibraryName}.klib").also { file ->
                        cinteropLibrary.copyTo(file)
                    }
                }
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
            artifactContent.getSourceSet("sourceSetB").also { sourceSetB ->
                val sourceSetBOutputDirectory = temporaryFolder.newFolder()
                val sourceSetBInteropMetadataFiles = sourceSetB.cinteropMetadataBinaries.map { cinteropLibrary ->
                    sourceSetBOutputDirectory.resolve("${cinteropLibrary.cinteropLibraryName}.klib").also { file ->
                        cinteropLibrary.copyTo(file)
                    }
                }
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
}

private fun createProjectStructureMetadata(
    sourceSetNamesByVariantName: Map<String, Set<String>> = emptyMap(),
    sourceSetsDependsOnRelation: Map<String, Set<String>> = emptyMap(),
    sourceSetCInteropMetadataDirectory: Map<String, String> = emptyMap(),
    sourceSetBinaryLayout: Map<String, SourceSetMetadataLayout> = emptyMap(),
    sourceSetModuleDependencies: Map<String, Set<ModuleDependencyIdentifier>> = emptyMap(),
    hostSpecificSourceSets: Set<String> = emptySet(),
    sourceSetNames: Set<String> = hostSpecificSourceSets +
            sourceSetNamesByVariantName.values.flatten() +
            sourceSetsDependsOnRelation.keys +
            sourceSetCInteropMetadataDirectory.keys +
            sourceSetBinaryLayout.keys +
            sourceSetModuleDependencies.keys,
    isPublishedAsRoot: Boolean = true,
): KotlinProjectStructureMetadata {
    return KotlinProjectStructureMetadata(
        sourceSetNamesByVariantName = sourceSetNamesByVariantName,
        sourceSetsDependsOnRelation = sourceSetsDependsOnRelation,
        sourceSetBinaryLayout = sourceSetBinaryLayout,
        sourceSetCInteropMetadataDirectory = sourceSetCInteropMetadataDirectory,
        sourceSetModuleDependencies = sourceSetModuleDependencies,
        hostSpecificSourceSets = hostSpecificSourceSets,
        sourceSetNames = sourceSetNames,
        isPublishedAsRoot = isPublishedAsRoot
    )
}

private fun File.withParentDirectoriesCreated(): File = apply { parentFile.mkdirs() }
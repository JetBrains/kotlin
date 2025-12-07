/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.DATA_PATH
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.FILE_IDS_TO_PATHS_FILENAME
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.LOOKUPS_FILENAME
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.SUBTYPES_FILENAME
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.cri
import org.jetbrains.kotlin.buildtools.api.cri.FileIdToPathEntry
import org.jetbrains.kotlin.buildtools.api.cri.LookupEntry
import org.jetbrains.kotlin.buildtools.api.cri.SubtypeEntry
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.*
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalBuildToolsApi::class)
@DisplayName("Gradle / Compiler Reference Index")
class CompilerReferenceIndexIT : KGPDaemonsBaseTest() {

    override val defaultBuildOptions: BuildOptions = super.defaultBuildOptions.copy(
        runViaBuildToolsApi = true,
        generateCompilerRefIndex = true,
    )

    private val defaultInProcessBuildOptions: BuildOptions = defaultBuildOptions.copy(
        compilerExecutionStrategy = KotlinCompilerExecutionStrategy.IN_PROCESS,
    )

    private val defaultDaemonBuildOptions: BuildOptions = defaultBuildOptions.copy(
        compilerExecutionStrategy = KotlinCompilerExecutionStrategy.DAEMON,
    )

    @GradleTest
    @DisplayName("Smoke test for Gradle / CRI data generation and deserialization")
    @GradleTestExtraStringArguments("in-process", "daemon")
    fun smokeTestCriDataGeneration(gradleVersion: GradleVersion, strategy: String) {
        project(
            "kotlinProject",
            gradleVersion,
            buildOptions = when (strategy) {
                "in-process" -> defaultInProcessBuildOptions
                "daemon" -> defaultDaemonBuildOptions
                else -> return // `out-of-process` strategy is not supported by BTA
            },
        ) {
            kotlinSourcesDir().source("main.kt") {
                //language=kotlin
                """
                open class Base
                class Derived: Base()
                fun use(d: Derived) = d.toString()
                """.trimIndent()
            }

            build("assemble") {
                if (strategy == "in-process") assertOutputContains("Generating Compiler Reference Index...")
            }

            val (lookups, fileIdsToPaths, subtypes) = deserializeCriData()
            assertTrue(lookups.isNotEmpty(), "Expected non-empty CRI lookup entries")
            assertTrue(fileIdsToPaths.isNotEmpty(), "Expected non-empty CRI fileIdToPath entries")
            assertTrue(subtypes.isNotEmpty(), "Expected non-empty CRI subtype entries")
        }
    }

    @GradleTest
    @DisplayName("Incremental CRI data generation appends new entries without invalidation, rebuild removes stale entries")
    fun testIncrementalCriDataGeneration(gradleVersion: GradleVersion) {
        project(
            "kotlinProject",
            gradleVersion,
            buildOptions = defaultInProcessBuildOptions,
        ) {
            val source1Filename = "file1.kt"
            val source2Filename = "file2.kt"
            kotlinSourcesDir().source(source1Filename) {
                //language=kotlin
                """
                open class Base
                class Derived : Base()
                fun use(d: Derived) = d.toString()
                """.trimIndent()
            }

            build("assemble")

            val (initialLookups, initialFileIdsToPaths, initialSubtypes) = deserializeCriData()

            val requiredSource1Path = (kotlinSourcesDir() / source1Filename).relativeTo(projectPath).invariantSeparatorsPathString
            val source1FileIdToPath = assertNotNull(initialFileIdsToPaths.singleOrNull { it.path == requiredSource1Path })
            val baseHash = hashCode("Base")
            val derivedHash = hashCode("Derived")
            val derived2Hash = hashCode("Derived2")

            val derivedLookupEntry = assertNotNull(
                initialLookups.singleOrNull { it.fqNameHashCode == derivedHash }
            )
            assertContains(derivedLookupEntry.fileIds, source1FileIdToPath.fileId)

            val baseSubtypeEntry = assertNotNull(
                initialSubtypes.singleOrNull { it.fqNameHashCode == baseHash }
            )
            assertEquals(listOf("Derived"), baseSubtypeEntry.subtypes)

            // adding new subtype and lookup entries to another source file
            kotlinSourcesDir().source(source2Filename) {
                //language=kotlin
                """
                class Derived2 : Base()
                fun use(d: Derived2) = d.toString()
                """.trimIndent()
            }
            // and removing old subtype and lookup entries from the first source file
            kotlinSourcesDir().source(source1Filename) {
                //language=kotlin
                """
                open class Base
                """.trimIndent()
            }

            build("assemble")

            val (modifiedLookups, modifiedFileIdsToPaths, modifiedSubtypes) = deserializeCriData()

            // TODO KT-82000 Find better approach for generating CRI data with IC instead of appending new data
            // after the incremental compilation there will be 2 entries for the same source file
            // they should have the same (hash-based) id
            val source1FileIdsToPaths = modifiedFileIdsToPaths.filter { it.path == requiredSource1Path }
            assertEquals(2, source1FileIdsToPaths.size)
            assertEquals(source1FileIdsToPaths.first().fileId, source1FileIdsToPaths.last().fileId)

            val requiredSource2Path = (kotlinSourcesDir() / source2Filename).relativeTo(projectPath).invariantSeparatorsPathString
            val source2FileIdToPath = assertNotNull(modifiedFileIdsToPaths.singleOrNull { it.path == requiredSource2Path })

            val oldDerivedLookupEntry = assertNotNull(modifiedLookups.singleOrNull { it.fqNameHashCode == derivedHash })
            assertContains(oldDerivedLookupEntry.fileIds, source1FileIdToPath.fileId)
            val derived2LookupEntry = assertNotNull(modifiedLookups.singleOrNull { it.fqNameHashCode == derived2Hash })
            assertContains(derived2LookupEntry.fileIds, source2FileIdToPath.fileId)

            val baseSubtypeEntries = modifiedSubtypes.filter { it.fqNameHashCode == baseHash }
            assertEquals(2, baseSubtypeEntries.size)
            assertEquals(listOf("Derived"), baseSubtypeEntries.first().subtypes)
            assertEquals(listOf("Derived2"), baseSubtypeEntries.last().subtypes)

            // force rebuild to clean stale CRI data
            build("assemble", "--rerun-tasks")

            val (afterRebuildLookups, afterRebuildFileIdsToPaths, afterRebuildSubtypes) = deserializeCriData()

            assertNotNull(afterRebuildFileIdsToPaths.singleOrNull { it.path == requiredSource2Path })

            // make sure that the stale lookup entries are gone
            assertNull(afterRebuildLookups.firstOrNull { it.fqNameHashCode == derivedHash })
            val afterRebuildDerived2LookupEntry = assertNotNull(afterRebuildLookups.singleOrNull { it.fqNameHashCode == derived2Hash })
            assertContains(afterRebuildDerived2LookupEntry.fileIds, source2FileIdToPath.fileId)

            val afterRebuildBaseSubtypeEntry = assertNotNull(afterRebuildSubtypes.singleOrNull { it.fqNameHashCode == baseHash })
            // stale `Derived` entry should not be there anymore
            assertEquals(listOf("Derived2"), afterRebuildBaseSubtypeEntry.subtypes)
        }
    }

    private fun hashCode(fqName: String): Int = FqName(fqName).hashCode()

    private fun TestProject.deserializeCriData(): Triple<List<LookupEntry>, List<FileIdToPathEntry>, List<SubtypeEntry>> {
        val criDir = projectPath.resolve("build/kotlin/compileKotlin/cacheable").resolve(DATA_PATH)
        val lookups = criDir.resolve(LOOKUPS_FILENAME)
        val fileIdsToPaths = criDir.resolve(FILE_IDS_TO_PATHS_FILENAME)
        val subtypes = criDir.resolve(SUBTYPES_FILENAME)

        assertFilesExist(lookups, fileIdsToPaths, subtypes)

        val toolchain = KotlinToolchains.loadImplementation(this::class.java.classLoader)
        return toolchain.createBuildSession().use { session ->
            val lookupEntries = session.executeOperation(
                toolchain.cri.createCriLookupDataDeserializationOperation(lookups.readBytes())
            ).toList()

            val fileIdToPathEntries = session.executeOperation(
                toolchain.cri.createCriFileIdToPathDataDeserializationOperation(fileIdsToPaths.readBytes())
            ).toList()

            val subtypeEntries = session.executeOperation(
                toolchain.cri.createCriSubtypeDataDeserializationOperation(subtypes.readBytes())
            ).toList()

            Triple(lookupEntries, fileIdToPathEntries, subtypeEntries)
        }
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.backend.common.KlibSignatureIndexConstants.KLIB_SIGNATURE_INDEX_FILE_NAME
import org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.utils.filterToSetOrEmpty
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import kotlin.io.path.appendText
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.relativeTo
import kotlin.io.path.walk
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks that the Kotlin/Native link task generates external signature indices for "old" libraries,
 * i.e. libraries that were produced by pre-2.5.0 compilers and do not have their own signature indices.
 */
@OsCondition(
    // Native caches are not supported on Windows.
    supportedOn = [OS.LINUX, OS.MAC],
    enabledOnCI = [OS.LINUX, OS.MAC]
)
@DisplayName("Tests for K/N external signature indices")
@NativeGradlePluginTests
class NativeExternalSignatureIndicesIT : KGPBaseTest() {

    @DisplayName("External signature indices are generated for old index-less libraries")
    @GradleTest
    fun testExternalIndicesGeneratedForOldLibraries(gradleVersion: GradleVersion) {
        nativeProject(PROJECT_NAME, gradleVersion) {
            injectDependenciesOnOldLibraries()
            assertDirectoryDoesNotExist(externalIndicesDir)

            build(DEBUG_LINK_TASK_NAME, indicesDirProperty(externalIndicesDir)) {
                extractNativeTasksCommandLineArgumentsFromOutput(":$DEBUG_LINK_TASK_NAME") {
                    assertEquals(
                        setOf(
                            "$GENERATE_INDICES_FROM_ARG=${getGradleUserHome()}",
                            "$GENERATE_INDICES_DIR_ARG=$externalIndicesDir",
                        ),
                        args.filterToSetOrEmpty { line -> GENERATE_INDICES_FROM_ARG in line || GENERATE_INDICES_DIR_ARG in line }
                    )
                }
            }

            val indices: Map<TargetName, Map<LibraryName, List<Path>>> = collectExternalIndices(externalIndicesDir)
            assertEquals(setOf(HostManager.host.name), indices.keys)

            val indicesForSingleTarget: Map<LibraryName, List<Path>> = indices.values.single()
            assertEquals(OLD_LIBRARIES.keys, indicesForSingleTarget.keys)

            for (libraryName in OLD_LIBRARIES.keys) {
                val indexFiles = indicesForSingleTarget.getValue(libraryName)
                assertEquals(1, indexFiles.size, "Expected exactly one external index for $libraryName, found: $indexFiles")
                val indexFile = indexFiles.single()
                assertTrue(indexFile.fileSize() > 0, "External index file is empty: $indexFile")
            }
        }
    }

    @DisplayName("External signature indices are reused in subsequent builds")
    @GradleTest
    fun testExternalIndicesAreReused(gradleVersion: GradleVersion) {
        nativeProject(PROJECT_NAME, gradleVersion) {
            injectDependenciesOnOldLibraries()
            assertDirectoryDoesNotExist(externalIndicesDir)

            build(DEBUG_LINK_TASK_NAME, indicesDirProperty(externalIndicesDir))

            val indicesAfterFirstBuild = collectIndexFilesWithTimestamps(externalIndicesDir)
            assertEquals(OLD_LIBRARIES.size, indicesAfterFirstBuild.size, "Unexpected external indices: ${indicesAfterFirstBuild.keys}")

            build(DEBUG_LINK_TASK_NAME, indicesDirProperty(externalIndicesDir), "--rerun-tasks") {
                assertTasksExecuted(":$DEBUG_LINK_TASK_NAME")
            }

            assertEquals(indicesAfterFirstBuild, collectIndexFilesWithTimestamps(externalIndicesDir))
        }
    }

    @DisplayName("External signature indices are not generated when native caches are not used")
    @GradleTest
    fun testNoExternalIndicesWhenCachesAreNotUsed(gradleVersion: GradleVersion) {
        nativeProject(PROJECT_NAME, gradleVersion) {
            injectDependenciesOnOldLibraries()
            assertDirectoryDoesNotExist(externalIndicesDir)

            // Native caches are not used for optimized binaries by default.
            build(RELEASE_LINK_TASK_NAME, indicesDirProperty(externalIndicesDir)) {
                extractNativeTasksCommandLineArgumentsFromOutput(":$RELEASE_LINK_TASK_NAME") {
                    assertEquals(
                        emptyList(),
                        args.filter { line -> GENERATE_INDICES_FROM_ARG in line || GENERATE_INDICES_DIR_ARG in line }
                    )
                }
            }

            assertDirectoryDoesNotExist(externalIndicesDir)
        }
    }

    private fun TestProject.injectDependenciesOnOldLibraries() {
        buildGradleKts.appendText(
            buildString {
                appendLine()
                appendLine()
                appendLine("kotlin {")
                appendLine("    sourceSets.commonMain.dependencies {")
                for ([ga, v] in OLD_LIBRARIES) {
                    if (ga.endsWith("-cinterop-interop")) {
                        // C-interop libraries are always published as a supplementary artifacts to the main library,
                        // so no need to specify here their coordinates.
                        continue
                    }
                    appendLine("        implementation(\"$ga:$v\")")
                }
                appendLine("    }")
                appendLine("}")
            }
        )
    }

    private val TestProject.externalIndicesDir: Path
        get() = projectPath.resolve("external-signature-indices")

    private fun indicesDirProperty(indicesDir: Path): String = "-P$SIGNATURE_INDICES_DIR_PROPERTY=$indicesDir"

    /**
     * Returns external index files grouped by the library unique name.
     * The layout of the external indices directory is `<target name>/<library unique name>/<library fingerprint hash>/signatures.idx`.
     */
    private fun collectExternalIndices(indicesDir: Path): Map<TargetName, Map<LibraryName, List<Path>>> {
        assertDirectoryExists(indicesDir)

        val result = hashMapOf<TargetName, MutableMap<LibraryName, MutableList<Path>>>()

        indicesDir.walk()
            .filter { it.isRegularFile() && it.name == KLIB_SIGNATURE_INDEX_FILE_NAME }
            .forEach { path ->
                val relativePath = path.relativeTo(indicesDir)
                assertEquals(4, relativePath.nameCount, "Unexpected layout of the external index: $relativePath")

                val targetName: TargetName = relativePath.getName(0).toString()
                val libraryName: LibraryName = relativePath.getName(1).toString()

                result.getOrPut(targetName) { mutableMapOf() }.getOrPut(libraryName) { mutableListOf() }.add(path)
            }

        return result
    }

    private fun collectIndexFilesWithTimestamps(indicesDir: Path): Map<Path, Long> =
        collectExternalIndices(indicesDir).values.single().values.flatten().associateWith { it.getLastModifiedTime().toMillis() }

    companion object {
        private const val PROJECT_NAME = "native-external-signature-indices"
        private const val DEBUG_LINK_TASK_NAME = "linkDebugExecutableNative"
        private const val RELEASE_LINK_TASK_NAME = "linkReleaseExecutableNative"

        private const val SIGNATURE_INDICES_DIR_PROPERTY = "kotlin.internal.native.signatureIndicesDir"

        private val GENERATE_INDICES_FROM_ARG = K2NativeCompilerArguments::generateSignatureIndicesFrom.cliArgument
        private val GENERATE_INDICES_DIR_ARG = K2NativeCompilerArguments::generateSignatureIndicesDir.cliArgument

        /** Libraries from Maven Central published by pre-2.5.0 compilers (without own signature indices). */
        private val OLD_LIBRARIES = mapOf(
            "org.jetbrains.kotlinx:kotlinx-coroutines-core" to "1.7.2",
            "org.jetbrains.kotlinx:atomicfu" to "0.21.0",
            "org.jetbrains.kotlinx:atomicfu-cinterop-interop" to "0.21.0", // C-interop library bundled with AtomicFU
        )
    }
}

private typealias TargetName = String
private typealias LibraryName = String

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.archive

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.BuildOptions
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.MppGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.assertTasksExecuted
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.disableIsolatedProjectsBecauseOfJsAndWasmKT75899
import org.jetbrains.kotlin.gradle.testbase.nativeProject
import org.jetbrains.kotlin.gradle.testbase.project
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.inputStream
import kotlin.test.assertEquals

@MppGradlePluginTests
class PackKotlinArchiveTaskIT : KGPBaseTest() {
    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @GradleTest
    fun testSimpleProducer(gradleVersion: GradleVersion) {
        project("archive/simpleProducer", gradleVersion) {
            build("packKotlinArchive") {
                assertTasksExecuted(":packKotlinArchive")
            }

            val archiveEntries = projectPath.resolve("build/kar/simpleProducer.kar.xz")
                .normalizedArchiveEntries()

            assertEquals(simpleProducerArchiveEntries, archiveEntries)
        }
    }

    @GradleTest
    fun testTargetRenamesDoNotAffectArchiveLayout(gradleVersion: GradleVersion) {
        project("archive/simpleProducerWithRenames", gradleVersion) {
            build("packKotlinArchive") {
                assertTasksExecuted(":packKotlinArchive")
            }

            val archiveEntries = projectPath.resolve("build/kar/simpleProducerWithRenames.kar.xz")
                .normalizedArchiveEntries()

            assertEquals(simpleProducerArchiveEntries, archiveEntries)
        }
    }

    @GradleTest
    fun testPackedKlibsAreUnpackedInArchive(gradleVersion: GradleVersion) {
        project("archive/simpleProducer", gradleVersion) {
            build(
                "packKotlinArchive",
                "-Pkotlin.internal.klibs.non-packed=false",
            ) {
                assertTasksExecuted(":packKotlinArchive")
            }

            val archiveEntries = projectPath.resolve("build/kar/simpleProducer.kar.xz")
                .normalizedArchiveEntries()

            assertEquals(simpleProducerArchiveEntries, archiveEntries)
        }
    }

    @GradleTest
    fun testCommonizedCInteropsKeepPerLibraryDirectories(gradleVersion: GradleVersion) {
        nativeProject("archive/producerWithCommonizedCinterops", gradleVersion) {
            build("packKotlinArchive") {
                assertTasksExecuted(":packKotlinArchive")
            }

            val archiveEntries = projectPath.resolve("build/kar/producerWithCommonizedCinterops.kar.xz")
                .normalizedArchiveEntries()

            assertEquals(producerWithCommonizedCinteropsArchiveEntries, archiveEntries)
        }
    }

    private fun Path.normalizedArchiveEntries(): String =
        zipXzArchiveEntries()
            .map { entry -> entry.collapseKnownKlibContent() }
            .distinct()
            .sorted()
            .joinToString("\n")

    private fun String.collapseKnownKlibContent(): String {
        val pathComponents = trimEnd('/').split('/')
        val category = pathComponents.first()
        // Cinterop klibs are grouped one level deeper than the rest: by target in 'cinterop/<target>/<library>'
        // and by source set in 'metadata/<sourceSet>-cinterop/<library>'.
        val isCommonizedCinteropMetadata = category == METADATA_DIRECTORY &&
                pathComponents.getOrNull(1)?.endsWith(CINTEROP_SUFFIX) == true
        val klibRootDepth = if (category == CINTEROP_DIRECTORY || isCommonizedCinteropMetadata) 3 else 2
        if (pathComponents.size <= klibRootDepth) return this

        val klibPath = pathComponents.drop(klibRootDepth)
        val entryName = klibPath.last()
        val isKnownKlibContent = if (endsWith('/')) {
            entryName in knownKlibDirectoryNames ||
                    entryName.startsWith("package_") ||
                    klibPath.getOrNull(klibPath.lastIndex - 1) == "targets"
        } else {
            entryName in knownKlibFileNames || entryName.substringAfterLast('.', "") in knownKlibFileExtensions
        }

        if (!isKnownKlibContent) return this

        val klibRoot = pathComponents.take(klibRootDepth).joinToString("/")
        return when {
            category == "platform" -> "$klibRoot/<platform klib content>"
            category == CINTEROP_DIRECTORY -> "$klibRoot/<cinterop klib content>"
            isCommonizedCinteropMetadata -> "$klibRoot/<commonized cinterop klib content>"
            category == METADATA_DIRECTORY -> "$klibRoot/<metadata klib content>"
            else -> this
        }
    }

    private companion object {
        val simpleProducerArchiveEntries = """
            cinterop/
            manifest.json
            metadata/
            metadata/commonMain/
            metadata/commonMain/<metadata klib content>
            metadata/kotlin-project-structure-metadata.json
            platform/
            platform/js/
            platform/js/<platform klib content>
            platform/macosArm64/
            platform/macosArm64/<platform klib content>
            platform/wasmJs/
            platform/wasmJs/<platform klib content>
            resources/
        """.trimIndent()

        /**
         * Every cinterop klib, both the platform ones and the commonized ones, is stored in its own directory named
         * after the library. Without it, klibs of different interops are merged into a single directory.
         */
        val producerWithCommonizedCinteropsArchiveEntries = """
            cinterop/
            cinterop/linuxArm64/
            cinterop/linuxArm64/producerWithCommonizedCinterops-cinterop-first/
            cinterop/linuxArm64/producerWithCommonizedCinterops-cinterop-first/<cinterop klib content>
            cinterop/linuxArm64/producerWithCommonizedCinterops-cinterop-second/
            cinterop/linuxArm64/producerWithCommonizedCinterops-cinterop-second/<cinterop klib content>
            cinterop/linuxX64/
            cinterop/linuxX64/producerWithCommonizedCinterops-cinterop-first/
            cinterop/linuxX64/producerWithCommonizedCinterops-cinterop-first/<cinterop klib content>
            cinterop/linuxX64/producerWithCommonizedCinterops-cinterop-second/
            cinterop/linuxX64/producerWithCommonizedCinterops-cinterop-second/<cinterop klib content>
            manifest.json
            metadata/
            metadata/commonMain-cinterop/
            metadata/commonMain-cinterop/producerWithCommonizedCinterops-cinterop-first/
            metadata/commonMain-cinterop/producerWithCommonizedCinterops-cinterop-first/<commonized cinterop klib content>
            metadata/commonMain-cinterop/producerWithCommonizedCinterops-cinterop-second/
            metadata/commonMain-cinterop/producerWithCommonizedCinterops-cinterop-second/<commonized cinterop klib content>
            metadata/commonMain/
            metadata/commonMain/<metadata klib content>
            metadata/kotlin-project-structure-metadata.json
            metadata/linuxMain-cinterop/
            metadata/linuxMain-cinterop/producerWithCommonizedCinterops-cinterop-first/
            metadata/linuxMain-cinterop/producerWithCommonizedCinterops-cinterop-first/<commonized cinterop klib content>
            metadata/linuxMain-cinterop/producerWithCommonizedCinterops-cinterop-second/
            metadata/linuxMain-cinterop/producerWithCommonizedCinterops-cinterop-second/<commonized cinterop klib content>
            metadata/nativeMain-cinterop/
            metadata/nativeMain-cinterop/producerWithCommonizedCinterops-cinterop-first/
            metadata/nativeMain-cinterop/producerWithCommonizedCinterops-cinterop-first/<commonized cinterop klib content>
            metadata/nativeMain-cinterop/producerWithCommonizedCinterops-cinterop-second/
            metadata/nativeMain-cinterop/producerWithCommonizedCinterops-cinterop-second/<commonized cinterop klib content>
            metadata/nativeMain/
            metadata/nativeMain/<metadata klib content>
            platform/
            platform/linuxArm64/
            platform/linuxArm64/<platform klib content>
            platform/linuxX64/
            platform/linuxX64/<platform klib content>
            resources/
        """.trimIndent()

        const val CINTEROP_SUFFIX = "-cinterop"
        const val CINTEROP_DIRECTORY = "cinterop"
        const val METADATA_DIRECTORY = "metadata"

        val knownKlibDirectoryNames = setOf(
            "default",
            "included",
            "ir",
            "ir_inlinable_functions",
            "linkdata",
            "native",
            "resources",
            "targets",
        )
        val knownKlibFileNames = setOf("manifest", "module")
        val knownKlibFileExtensions = setOf("bc", "knb", "knd", "knf", "knm", "knt")
    }

    private fun Path.zipXzArchiveEntries(): List<String> =
        inputStream().buffered().use { fileInput ->
            XZCompressorInputStream(fileInput).use { xzInput ->
                ZipInputStream(xzInput).use { zipInput ->
                    generateSequence(zipInput::getNextEntry)
                        .map { entry -> entry.name }
                        .toList()
                }
            }
        }
}

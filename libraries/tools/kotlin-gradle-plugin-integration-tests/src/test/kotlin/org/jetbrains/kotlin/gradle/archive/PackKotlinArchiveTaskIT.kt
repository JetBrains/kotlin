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

    private fun Path.normalizedArchiveEntries(): String {
        val entries = zipXzArchiveEntries()
        val klibRoots = entries.mapNotNull { entry -> entry.klibRootOrNull() }
        return entries
            .map { entry -> entry.collapseKlibContent(klibRoots) }
            .distinct()
            .sorted()
            .joinToString("\n")
    }

    private fun String.klibRootOrNull(): String? =
        if (endsWith("/$KLIB_MANIFEST_PATH")) removeSuffix("/$KLIB_MANIFEST_PATH") else null

    private fun String.collapseKlibContent(klibRoots: List<String>): String {
        val klibRoot = klibRoots.firstOrNull { klibRoot -> startsWith("$klibRoot/") } ?: return this
        return "$klibRoot/$KLIB_CONTENT_PLACEHOLDER"
    }

    private companion object {
        val simpleProducerArchiveEntries = """
            cinterop/
            manifest.json
            metadata/
            metadata/commonMain/<klib content>
            metadata/kotlin-project-structure-metadata.json
            platform/
            platform/js/<klib content>
            platform/macosArm64/<klib content>
            platform/wasmJs/<klib content>
            resources/
        """.trimIndent()

        val producerWithCommonizedCinteropsArchiveEntries = """
            cinterop/
            cinterop/linuxArm64/
            cinterop/linuxArm64/producerWithCommonizedCinterops-cinterop-first/<klib content>
            cinterop/linuxArm64/producerWithCommonizedCinterops-cinterop-second/<klib content>
            cinterop/linuxX64/
            cinterop/linuxX64/producerWithCommonizedCinterops-cinterop-first/<klib content>
            cinterop/linuxX64/producerWithCommonizedCinterops-cinterop-second/<klib content>
            manifest.json
            metadata/
            metadata/commonMain-cinterop/
            metadata/commonMain-cinterop/producerWithCommonizedCinterops-cinterop-first/<klib content>
            metadata/commonMain-cinterop/producerWithCommonizedCinterops-cinterop-second/<klib content>
            metadata/commonMain/<klib content>
            metadata/kotlin-project-structure-metadata.json
            metadata/linuxMain-cinterop/
            metadata/linuxMain-cinterop/producerWithCommonizedCinterops-cinterop-first/<klib content>
            metadata/linuxMain-cinterop/producerWithCommonizedCinterops-cinterop-second/<klib content>
            metadata/nativeMain-cinterop/
            metadata/nativeMain-cinterop/producerWithCommonizedCinterops-cinterop-first/<klib content>
            metadata/nativeMain-cinterop/producerWithCommonizedCinterops-cinterop-second/<klib content>
            metadata/nativeMain/<klib content>
            platform/
            platform/linuxArm64/<klib content>
            platform/linuxX64/<klib content>
            resources/
        """.trimIndent()

        const val KLIB_MANIFEST_PATH = "default/manifest"
        const val KLIB_CONTENT_PLACEHOLDER = "<klib content>"
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

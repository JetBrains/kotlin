/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.archive

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPublicationFormat
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
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
        val archiveEntries = packKotlinArchive(
            gradleVersion,
            projectName = "simpleProducer",
        ) { simpleProducer() }

        assertEquals(simpleProducerArchiveEntries, archiveEntries)
    }

    @GradleTest
    fun testTargetRenamesDoNotAffectArchiveLayout(gradleVersion: GradleVersion) {
        val archiveEntries = packKotlinArchive(
            gradleVersion,
            projectName = "simpleProducerWithRenames",
        ) {
            jvm("renamedJvm")
            js("renamedJs")
            wasmJs("renamedWasmJs")
            macosArm64("renamedMacosArm64")

            sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
        }

        assertEquals(simpleProducerArchiveEntries, archiveEntries)
    }

    @GradleTest
    fun testPackedKlibsAreUnpackedInArchive(gradleVersion: GradleVersion) {
        val archiveEntries = packKotlinArchive(
            gradleVersion,
            projectName = "simpleProducer",
            "-Pkotlin.internal.klibs.non-packed=false",
        ) { simpleProducer() }

        assertEquals(simpleProducerArchiveEntries, archiveEntries)
    }

    @GradleTest
    fun testCommonizedCInteropsKeepPerLibraryDirectories(gradleVersion: GradleVersion) {
        val archiveEntries = packKotlinArchive(
            gradleVersion,
            projectName = "producerWithCommonizedCinterops",
            "-Pkotlin.mpp.enableCInteropCommonization=true",
        ) {
            listOf(linuxX64(), linuxArm64()).forEach { target ->
                target.createCInterop("first")
                target.createCInterop("second")
            }

            sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
            sourceSets.nativeMain.get().compileStubSourceWithSourceSetName()
        }

        assertEquals(producerWithCommonizedCinteropsArchiveEntries, archiveEntries)
    }

    private fun packKotlinArchive(
        gradleVersion: GradleVersion,
        projectName: String,
        vararg buildArguments: String,
        configure: KotlinMultiplatformExtension.() -> Unit,
    ): String = project("empty", gradleVersion) {
        addKgpToBuildScriptCompilationClasspath()
        settingsBuildScriptInjection {
            settings.rootProject.name = projectName
        }
        buildScriptInjection {
            project.applyMultiplatform {
                configure()
                publishing {
                    publicationFormat.set(KotlinPublicationFormat.KOTLIN_ARCHIVE)
                }
            }
        }
    }.run {
        build("packKotlinArchive", *buildArguments) {
            assertTasksExecuted(":packKotlinArchive")
        }

        projectPath.resolve("build/kar/$projectName.kar.xz").normalizedArchiveEntries()
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

private fun KotlinMultiplatformExtension.simpleProducer() {
    jvm()
    js()
    wasmJs()
    macosArm64()

    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
}

private fun KotlinNativeTarget.createCInterop(interopName: String) {
    val definitionFile = project.layout.projectDirectory.file("$interopName.def")
    definitionFile.asFile.writeText(
        """
        language = C
        ---
        void $interopName(void);
        """.trimIndent()
    )
    compilations.getByName("main").cinterops.create(interopName) {
        it.definitionFile.set(definitionFile)
    }
}

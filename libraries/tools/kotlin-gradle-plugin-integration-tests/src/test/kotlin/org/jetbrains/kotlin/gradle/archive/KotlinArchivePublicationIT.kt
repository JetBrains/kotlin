/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.gradle.archive

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.uklibs.ComponentPointer
import org.jetbrains.kotlin.gradle.uklibs.GradleMetadata
import org.jetbrains.kotlin.gradle.uklibs.PublishedProject
import org.jetbrains.kotlin.gradle.uklibs.Variant
import org.jetbrains.kotlin.gradle.uklibs.VariantFile
import org.jetbrains.kotlin.gradle.uklibs.publish
import org.junit.jupiter.api.DisplayName
import java.io.File
import java.util.zip.ZipFile
import kotlin.test.assertEquals

@MppGradlePluginTests
@DisplayName("Publication of a project in the Kotlin Archive format")
class KotlinArchivePublicationIT : KGPBaseTest() {

    override val defaultBuildOptions: BuildOptions
        get() = super.defaultBuildOptions.disableIsolatedProjectsBecauseOfJsAndWasmKT75899()

    @GradleTest
    fun publicationContentTest(gradleVersion: GradleVersion) {
        val publishedProject = kotlinArchiveProducer(gradleVersion).publish()

        assertEquals(
            listOf("producer", "producer-jvm").prettyPrinted,
            publishedProject.publishedModules().prettyPrinted,
        )

        assertEquals(
            listOf(
                "producer-1.0-kotlin-tooling-metadata.json",
                "producer-1.0-sources.jar",
                "producer-1.0.kar.xz",
                "producer-1.0.module",
                "producer-1.0.pom",
            ).prettyPrinted,
            publishedProject.publishedArtifacts(module = "producer").prettyPrinted,
        )

        assertEquals(
            expectedRootVariants.prettyPrinted,
            publishedProject.rootComponent.gradleMetadata.parseGradleMetadata().prettyPrinted,
        )

        assertEquals(
            expectedRootSourcesJarEntries.prettyPrinted,
            publishedProject.rootComponent.sourcesJar.zipEntries().prettyPrinted,
        )
    }

    private fun PublishedProject.publishedModules(): List<String> =
        groupDirectory().listFiles().orEmpty()
            .map { it.name }
            .sorted()

    private fun PublishedProject.publishedArtifacts(module: String): List<String> =
        groupDirectory().resolve(module).resolve(version)
            .listFiles().orEmpty()
            .map { it.name }
            .filterNot { it.substringAfterLast(".") in checksumExtensions }
            .sorted()

    private fun PublishedProject.groupDirectory(): File =
        repository.resolve(group.replace(".", File.separator))

    private fun File.parseGradleMetadata(): GradleMetadata =
        inputStream().use { input -> json.decodeFromStream<GradleMetadata>(input) }

    private fun File.zipEntries(): List<String> = ZipFile(this).use { zip ->
        zip.entries().asSequence()
            .filterNot { it.isDirectory }
            .map { it.name }
            .sorted()
            .toList()
    }

    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val checksumExtensions = setOf("md5", "sha1", "sha256", "sha512")

        private val expectedRootVariants = GradleMetadata(
            variants = setOf(
                Variant(
                    attributes = mapOf(
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "non-jvm",
                        "org.gradle.usage" to "kotlin-api",
                        "org.jetbrains.kotlin.kar.compression.method" to "XZ",
                        "org.jetbrains.kotlin.native.target" to "ios_arm64",
                        "org.jetbrains.kotlin.platform.type" to "native",
                    ),
                    availableAt = null,
                    files = listOf(
                        VariantFile(
                            name = "producer.kar.xz",
                            url = "producer-1.0.kar.xz",
                        ),
                    ),
                    name = "iosArm64ApiElements-published",
                ),
                Variant(
                    attributes = mapOf(
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "non-jvm",
                        "org.gradle.usage" to "kotlin-api",
                        "org.jetbrains.kotlin.kar.compression.method" to "XZ",
                        "org.jetbrains.kotlin.js.compiler" to "ir",
                        "org.jetbrains.kotlin.platform.type" to "js",
                    ),
                    availableAt = null,
                    files = listOf(
                        VariantFile(
                            name = "producer.kar.xz",
                            url = "producer-1.0.kar.xz",
                        ),
                    ),
                    name = "jsApiElements-published",
                ),
                Variant(
                    attributes = mapOf(
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "non-jvm",
                        "org.gradle.usage" to "kotlin-runtime",
                        "org.jetbrains.kotlin.kar.compression.method" to "XZ",
                        "org.jetbrains.kotlin.js.compiler" to "ir",
                        "org.jetbrains.kotlin.platform.type" to "js",
                    ),
                    availableAt = null,
                    files = listOf(
                        VariantFile(
                            name = "producer.kar.xz",
                            url = "producer-1.0.kar.xz",
                        ),
                    ),
                    name = "jsRuntimeElements-published",
                ),
                Variant(
                    attributes = mapOf(
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "standard-jvm",
                        "org.gradle.libraryelements" to "jar",
                        "org.gradle.usage" to "java-api",
                        "org.jetbrains.kotlin.platform.type" to "jvm",
                    ),
                    availableAt = ComponentPointer(
                        url = "../../producer-jvm/1.0/producer-jvm-1.0.module",
                    ),
                    files = listOf(
                    ),
                    name = "jvmApiElements-published",
                ),
                Variant(
                    attributes = mapOf(
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "standard-jvm",
                        "org.gradle.libraryelements" to "jar",
                        "org.gradle.usage" to "java-runtime",
                        "org.jetbrains.kotlin.platform.type" to "jvm",
                    ),
                    availableAt = ComponentPointer(
                        url = "../../producer-jvm/1.0/producer-jvm-1.0.module",
                    ),
                    files = listOf(
                    ),
                    name = "jvmRuntimeElements-published",
                ),
                Variant(
                    attributes = mapOf(
                        "org.gradle.category" to "documentation",
                        "org.gradle.dependency.bundling" to "external",
                        "org.gradle.docstype" to "sources",
                        "org.gradle.jvm.environment" to "standard-jvm",
                        "org.gradle.libraryelements" to "jar",
                        "org.gradle.usage" to "java-runtime",
                        "org.jetbrains.kotlin.platform.type" to "jvm",
                    ),
                    availableAt = ComponentPointer(
                        url = "../../producer-jvm/1.0/producer-jvm-1.0.module",
                    ),
                    files = listOf(
                    ),
                    name = "jvmSourcesElements-published",
                ),
                Variant(
                    attributes = mapOf(
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "non-jvm",
                        "org.gradle.usage" to "kotlin-api",
                        "org.jetbrains.kotlin.kar.compression.method" to "XZ",
                        "org.jetbrains.kotlin.native.target" to "linux_x64",
                        "org.jetbrains.kotlin.platform.type" to "native",
                    ),
                    availableAt = null,
                    files = listOf(
                        VariantFile(
                            name = "producer.kar.xz",
                            url = "producer-1.0.kar.xz",
                        ),
                    ),
                    name = "linuxX64ApiElements-published",
                ),
                Variant(
                    attributes = mapOf(
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "non-jvm",
                        "org.gradle.usage" to "kotlin-api",
                        "org.jetbrains.kotlin.kar.compression.method" to "XZ",
                        "org.jetbrains.kotlin.native.target" to "linux_arm64",
                        "org.jetbrains.kotlin.platform.type" to "native",
                    ),
                    availableAt = null,
                    files = listOf(
                        VariantFile(
                            name = "producer.kar.xz",
                            url = "producer-1.0.kar.xz",
                        ),
                    ),
                    name = "linuxArm64ApiElements-published",
                ),
                Variant(
                    attributes = mapOf(
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "non-jvm",
                        "org.gradle.usage" to "kotlin-api",
                        "org.jetbrains.kotlin.kar.compression.method" to "XZ",
                        "org.jetbrains.kotlin.native.target" to "macos_arm64",
                        "org.jetbrains.kotlin.platform.type" to "native",
                    ),
                    availableAt = null,
                    files = listOf(
                        VariantFile(
                            name = "producer.kar.xz",
                            url = "producer-1.0.kar.xz",
                        ),
                    ),
                    name = "macosArm64ApiElements-published",
                ),
                Variant(
                    attributes = mapOf(
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "non-jvm",
                        "org.gradle.usage" to "kotlin-metadata",
                        "org.jetbrains.kotlin.kar.compression.method" to "XZ",
                        "org.jetbrains.kotlin.platform.type" to "common",
                    ),
                    availableAt = null,
                    files = listOf(
                        VariantFile(
                            name = "producer.kar.xz",
                            url = "producer-1.0.kar.xz",
                        ),
                    ),
                    name = "metadataApiElements-published",
                ),
                Variant(
                    attributes = mapOf(
                        "org.gradle.category" to "documentation",
                        "org.gradle.dependency.bundling" to "external",
                        "org.gradle.docstype" to "sources",
                        "org.gradle.jvm.environment" to "non-jvm",
                        "org.gradle.usage" to "kotlin-runtime",
                        "org.jetbrains.kotlin.platform.type" to "common",
                    ),
                    availableAt = null,
                    files = listOf(
                        VariantFile(
                            name = "producer-kotlin-1.0-sources.jar",
                            url = "producer-1.0-sources.jar",
                        ),
                    ),
                    name = "metadataSourcesElements-published",
                ),
                Variant(
                    attributes = mapOf(
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "non-jvm",
                        "org.gradle.usage" to "kotlin-api",
                        "org.jetbrains.kotlin.kar.compression.method" to "XZ",
                        "org.jetbrains.kotlin.platform.type" to "wasm",
                        "org.jetbrains.kotlin.wasm.target" to "js",
                    ),
                    availableAt = null,
                    files = listOf(
                        VariantFile(
                            name = "producer.kar.xz",
                            url = "producer-1.0.kar.xz",
                        ),
                    ),
                    name = "wasmJsApiElements-published",
                ),
                Variant(
                    attributes = mapOf(
                        "org.gradle.category" to "library",
                        "org.gradle.jvm.environment" to "non-jvm",
                        "org.gradle.usage" to "kotlin-runtime",
                        "org.jetbrains.kotlin.kar.compression.method" to "XZ",
                        "org.jetbrains.kotlin.platform.type" to "wasm",
                        "org.jetbrains.kotlin.wasm.target" to "js",
                    ),
                    availableAt = null,
                    files = listOf(
                        VariantFile(
                            name = "producer.kar.xz",
                            url = "producer-1.0.kar.xz",
                        ),
                    ),
                    name = "wasmJsRuntimeElements-published",
                ),
            ),
        )

        private val expectedRootSourcesJarEntries = listOf(
            "META-INF/MANIFEST.MF",
            "appleMain/appleMain.kt",
            "commonMain/commonMain.kt",
            "iosArm64Main/iosArm64Main.kt",
            "jsMain/jsMain.kt",
            "linuxArm64Main/linuxArm64Main.kt",
            "linuxMain/linuxMain.kt",
            "linuxX64Main/linuxX64Main.kt",
            "macosArm64Main/macosArm64Main.kt",
            "nativeMain/nativeMain.kt",
            "wasmJsMain/wasmJsMain.kt",
            "webMain/webMain.kt",
        )
    }
}

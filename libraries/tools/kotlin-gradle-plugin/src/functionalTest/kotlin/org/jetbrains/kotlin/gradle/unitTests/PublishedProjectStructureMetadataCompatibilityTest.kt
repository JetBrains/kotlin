/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Libraries keep the project structure metadata they were published with, so the current plugin must read what every
 * Kotlin release since 1.8.20 wrote. All of them use format 0.3.3; older formats are covered in
 * [KotlinProjectStructureMetadataSerializationTest]. Each fixture is
 * `META-INF/kotlin-project-structure-metadata.json` taken unchanged from a library on Maven Central; the Kotlin version
 * is the `compiler_version` in the jar's `commonMain/default/manifest`.
 */
class PublishedProjectStructureMetadataCompatibilityTest {

    private class Published(val kotlinVersion: String, val library: String) {
        val file: File get() = File("src/functionalTest/resources/publishedProjectStructureMetadata/kotlin-$kotlinVersion.json").absoluteFile
        override fun toString() = "Kotlin $kotlinVersion ($library)"
    }

    private val published = listOf(
        Published("1.8.20", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3"),
        Published("1.9.0", "org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.0"),
        Published("1.9.21", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0"),
        Published("2.0.0", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0"),
        Published("2.0.20", "org.jetbrains.kotlinx:kotlinx-serialization-core:1.7.3"),
        Published("2.1.0", "org.jetbrains.kotlinx:kotlinx-io-core:0.7.0"),
        Published("2.1.20", "org.jetbrains.kotlinx:kotlinx-datetime:0.7.0"),
        Published("2.2.0", "org.jetbrains.kotlinx:kotlinx-serialization-core:1.9.0"),
        Published("2.2.20", "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0"),
        Published("2.3.0", "org.jetbrains.kotlinx:kotlinx-io-core:0.9.0"),
        Published("2.3.21", "io.ktor:ktor-network:3.5.0"),
        Published("2.4.10", "org.jetbrains.kotlinx:kotlinx-serialization-core:1.12.0"),
    )

    @TempDir
    lateinit var tempDir: File

    /** Reads the file from a metadata jar, the way a dependency is read, and checks every value against the raw JSON. */
    @TestFactory
    fun `published metadata is read completely`(): List<DynamicTest> = published.map { published ->
        dynamicTest(published.toString()) {
            val json = published.file.readText()
            val read = assertNotNull(JarMppDependencyProjectStructureMetadataExtractor(metadataJar(json)).getProjectStructureMetadata())
            assertEquals(expectedMetadata(json), read)
        }
    }

    /** Older plugins read what the current one publishes, so writing a published file back must not change it. */
    @TestFactory
    fun `published metadata is written back unchanged`(): List<DynamicTest> = published.map { published ->
        dynamicTest(published.toString()) {
            val json = published.file.readText()
            assertEquals(json, parseKotlinSourceSetMetadataFromJson(json).toJson())
        }
    }

    private fun metadataJar(json: String): File = File(tempDir, "metadata.jar").apply {
        ZipOutputStream(outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("META-INF/kotlin-project-structure-metadata.json"))
            zip.write(json.toByteArray())
            zip.closeEntry()
        }
    }

    /** Built from the generic JSON tree, independently of the serializers under test. */
    private fun expectedMetadata(json: String): KotlinProjectStructureMetadata {
        val projectStructure = Json.parseToJsonElement(json).jsonObject.getValue("projectStructure").jsonObject
        val sourceSets = projectStructure.getValue("sourceSets").jsonArray.map { it.jsonObject }
        fun JsonObject.string(key: String) = get(key)?.jsonPrimitive?.content
        fun JsonObject.strings(key: String) = get(key)?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty()

        return KotlinProjectStructureMetadata(
            sourceSetNamesByVariantName = projectStructure.getValue("variants").jsonArray.associate {
                it.jsonObject.string("name")!! to it.jsonObject.strings("sourceSet").toSet()
            },
            sourceSetsDependsOnRelation = sourceSets.associate { it.string("name")!! to it.strings("dependsOn").toSet() },
            sourceSetBinaryLayout = sourceSets.mapNotNull { sourceSet ->
                sourceSet.string("binaryLayout")?.let { sourceSet.string("name")!! to SourceSetMetadataLayout.byName(it)!! }
            }.toMap(),
            sourceSetModuleDependencies = sourceSets.associate { sourceSet ->
                sourceSet.string("name")!! to sourceSet.strings("moduleDependency").map {
                    val (groupId, moduleId) = it.split(":")
                    ModuleDependencyIdentifier(groupId, moduleId)
                }.toSet()
            },
            sourceSetCInteropMetadataDirectory = sourceSets.mapNotNull { sourceSet ->
                sourceSet.string("sourceSetCInteropMetadataDirectory")?.let { sourceSet.string("name")!! to it }
            }.toMap(),
            hostSpecificSourceSets = sourceSets.filter { it.string("hostSpecific") == "true" }.map { it.string("name")!! }.toSet(),
            isPublishedAsRoot = projectStructure.string("isPublishedAsRoot") == "true",
            sourceSetNames = sourceSets.map { it.string("name")!! }.toSet(),
            formatVersion = projectStructure.string("formatVersion")!!,
        )
    }
}

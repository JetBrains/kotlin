/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.SourceSetMetadataLayout.KLIB
import org.jetbrains.kotlin.gradle.plugin.mpp.SourceSetMetadataLayout.METADATA
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KotlinProjectStructureMetadataSerializationTest {

    private val sampleMetadata = KotlinProjectStructureMetadata(
        sourceSetNamesByVariantName = mapOf(
            "variant1" to setOf("commonMain", "sourceSetA", "sourceSetB"),
            "variant2" to setOf("commonMain", "sourceSetC")
        ),
        sourceSetsDependsOnRelation = mapOf(
            "commonMain" to emptySet(),
            "sourceSetA" to setOf("commonMain"),
            "sourceSetB" to setOf("commonMain", "sourceSetA"),
            "sourceSetC" to setOf("commonMain", "sourceSetB")
        ),
        sourceSetBinaryLayout = mapOf("sourceSetA" to METADATA, "sourceSetB" to KLIB, "sourceSetC" to KLIB),
        sourceSetModuleDependencies = mapOf(
            "commonMain" to emptySet(),
            "sourceSetA" to setOf(ModuleDependencyIdentifier("aa", "bb")),
            "sourceSetB" to setOf(ModuleDependencyIdentifier("cc", "dd"), ModuleDependencyIdentifier("ee", "ff")),
            "sourceSetC" to emptySet()
        ),
        sourceSetCInteropMetadataDirectory = mapOf("sourceSetB" to "xx/cinterop/", "sourceSetC" to "cinterops/C"),
        hostSpecificSourceSets = setOf("sourceSetC"),
        isPublishedAsRoot = true,
        sourceSetNames = setOf("commonMain", "sourceSetA", "sourceSetB", "sourceSetC"),
    )

    @Test
    fun `serialize and deserialize - json`() {
        val json = sampleMetadata.toJson()
        val deserialized = parseKotlinSourceSetMetadataFromJson(json)
        assertEquals(sampleMetadata, deserialized)
    }

    /** stdlib and kotlin.test compare this output with checked-in files, so the format must not drift. */
    @Test
    fun `json output format is stable`() {
        val expected = File("src/functionalTest/resources/kotlin-project-structure-metadata.golden.json")
            .absoluteFile.readText()
        assertEquals(expected.trim(), sampleMetadata.toJson().trim())
    }

    @Test
    fun `serialize and deserialize - xml`() {
        val xml = sampleMetadata.toXmlDocument()
        val deserialized = parseKotlinSourceSetMetadataFromXml(xml)
        assertEquals(sampleMetadata, deserialized)
    }

    @Test
    fun `deserialize 0_3_1 format version built from coroutines`() {
        val json = File("src/functionalTest/resources/coroutines-kotlin-project-structure-metadata.0_3_1.json").absoluteFile.readText()
        val deserialized = assertNotNull(parseKotlinSourceSetMetadataFromJson(json))
        assertEquals(KotlinProjectStructureMetadata.FORMAT_VERSION_0_3_1, deserialized.formatVersion)
        assertTrue(deserialized.isPublishedAsRoot)
        assertEquals(setOf("commonMain", "concurrentMain"), deserialized.sourceSetsDependsOnRelation["nativeMain"])

        /*
        We expect no 'cinterop metadata' in artifacts with older format versions
        */
        assertEquals(emptyMap(), deserialized.sourceSetCInteropMetadataDirectory)
        assertEquals(
            setOf("commonMain", "concurrentMain", "nativeDarwinMain", "nativeMain", "nativeOtherMain"),
            deserialized.sourceSetNames
        )
    }

    /** Files older than format 0.3.1 have no `isPublishedAsRoot`. */
    @Test
    fun `deserialize - missing isPublishedAsRoot defaults to false`() {
        val json = """
            {
              "projectStructure": {
                "formatVersion": "0.3.3",
                "variants": [],
                "sourceSets": [
                  {
                    "name": "commonMain",
                    "dependsOn": [],
                    "moduleDependency": []
                  }
                ]
              }
            }
        """.trimIndent()

        val deserialized = parseKotlinSourceSetMetadataFromJson(json)
        assertFalse(deserialized.isPublishedAsRoot)
    }

    @Test
    fun `deserialize - unquoted booleans are accepted`() {
        val json = """
            {
              "projectStructure": {
                "formatVersion": "0.3.3",
                "isPublishedAsRoot": true,
                "variants": [],
                "sourceSets": [
                  {
                    "name": "commonMain",
                    "dependsOn": [],
                    "moduleDependency": [],
                    "hostSpecific": true
                  }
                ]
              }
            }
        """.trimIndent()

        val deserialized = parseKotlinSourceSetMetadataFromJson(json)
        assertTrue(deserialized.isPublishedAsRoot)
        assertEquals(setOf("commonMain"), deserialized.hostSpecificSourceSets)
    }

    @Test
    fun `deserialize - lenient input and a leading BOM are accepted`() {
        val lenient = """
            {
              // comment
              projectStructure: {
                formatVersion: 0.3.3,
                isPublishedAsRoot: true,
                variants: [],
                sourceSets: [
                  {
                    name: commonMain,
                    dependsOn: [],
                    moduleDependency: []
                  }
                ]
              }
            }
        """.trimIndent()

        val deserialized = parseKotlinSourceSetMetadataFromJson("\uFEFF" + lenient)
        assertTrue(deserialized.isPublishedAsRoot)
        assertEquals(setOf("commonMain"), deserialized.sourceSetNames)
    }

    @Test
    fun `deserialize - malformed module dependency is reported`() {
        val json = """
            {
              "projectStructure": {
                "formatVersion": "0.3.3",
                "isPublishedAsRoot": "false",
                "variants": [],
                "sourceSets": [
                  {
                    "name": "commonMain",
                    "dependsOn": [],
                    "moduleDependency": ["no-separator-here"]
                  }
                ]
              }
            }
        """.trimIndent()

        val message = assertFailsWith<IllegalStateException> { parseKotlinSourceSetMetadataFromJson(json) }.message
        assertEquals(
            "Malformed module dependency 'no-separator-here' in project structure metadata, expected '<groupId>:<moduleId>'",
            message
        )
    }

    @Test
    fun `deserialize - unknown keys are ignored`() {
        val json = """
            {
              "projectStructure": {
                "formatVersion": "0.3.3",
                "isPublishedAsRoot": "true",
                "someFutureKey": { "nested": [1, 2] },
                "variants": [],
                "sourceSets": [
                  {
                    "name": "commonMain",
                    "dependsOn": [],
                    "moduleDependency": [],
                    "someFutureFlag": "true"
                  }
                ]
              }
            }
        """.trimIndent()

        val deserialized = parseKotlinSourceSetMetadataFromJson(json)
        assertTrue(deserialized.isPublishedAsRoot)
        assertEquals(setOf("commonMain"), deserialized.sourceSetNames)
    }
}

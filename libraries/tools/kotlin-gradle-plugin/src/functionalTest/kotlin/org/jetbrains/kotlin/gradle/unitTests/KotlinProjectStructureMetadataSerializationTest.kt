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

    /**
     * The emitted JSON is published inside metadata jars as `META-INF/kotlin-project-structure-metadata.json`
     * and is compared with exact string equality against checked-in expectations by the `libraries/stdlib` and
     * `libraries/kotlin.test` build scripts. Guard the exact bytes, not just the round trip.
     *
     * Caveat: this classpath resolves `kotlinx-serialization-json-jvm` to a newer version than the one embedded
     * into the shipped plugin (conflict resolution against `com.jetbrains.intellij.platform:util` wins over the
     * `strictly` constraint, which only pins the umbrella `-json` module). The two disagree on how empty arrays
     * are pretty-printed, so the golden file records this classpath's output, not the published bytes — see the
     * comment on `projectStructureMetadataJson` in `KotlinProjectStructureMetadataJson.kt`.
     */
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

    /**
     * The Gson implementation read this key as `valueNamed(...)?.toBoolean() ?: false`, so a file without it
     * parsed fine. Hand-patched files (this repo ships two) and pre-0.3.1 producers depend on that.
     */
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

    /**
     * KGP writes these booleans quoted, but Gson's `asString` also accepted the unquoted JSON form, so other
     * producers may emit it. Keep accepting both.
     */
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

}

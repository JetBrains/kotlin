/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.SourceSetMetadataLayout.KLIB
import org.jetbrains.kotlin.gradle.plugin.mpp.SourceSetMetadataLayout.METADATA
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class KotlinProjectStructureMetadataSerializationTest {

    private val sampleMetadata = KotlinProjectStructureMetadata(
        sourceSetNamesByVariantName = mapOf(
            "sourceSetA" to setOf("value1", "value2"), "sourceSetB" to setOf("value3", "value4"), "sourceSetC" to setOf("value5")
        ),
        sourceSetsDependsOnRelation = mapOf(
            "sourceSetA" to setOf("commonMain"),
            "sourceSetB" to setOf("commonMain", "sourceSetA"),
            "sourceSetC" to setOf("commonMain", "sourceSetB")
        ),
        sourceSetBinaryLayout = mapOf("sourceSetA" to METADATA, "sourceSetB" to KLIB, "sourceSetC" to KLIB),
        sourceSetModuleDependencies = mapOf(
            "sourceSetA" to setOf(ModuleDependencyIdentifier("aa", "bb")),
            "sourceSetB" to setOf(ModuleDependencyIdentifier("cc", "dd"), ModuleDependencyIdentifier("ee", "ff")),
            "sourceSetC" to emptySet()
        ),
        sourceSetCInteropMetadataDirectory = mapOf("sourceSetB" to "xx/cinterop/", "sourceSetC" to "cinterops/C"),
        hostSpecificSourceSets = setOf("sourceSetC"),
        isPublishedAsRoot = true
    )

    @Test
    fun `serialize and deserialize - json`() {
        val json = sampleMetadata.toJson()
        val deserialized = parseKotlinSourceSetMetadataFromJson(json)
        assertEquals(sampleMetadata, deserialized)
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
    }

}

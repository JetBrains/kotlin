/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.artifacts.component.ComponentIdentifier
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsSeverity
import org.jetbrains.kotlin.gradle.plugin.diagnostics.ToolingDiagnostic
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.metadataByComponent
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SWIFT_EXPORT_METADATA_SCHEMA_VERSION
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SwiftExportMetadata
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.utils.LazyResolvedConfigurationWithArtifacts
import org.jetbrains.kotlin.gradle.utils.createConsumable
import org.jetbrains.kotlin.gradle.utils.createResolvable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests that exercise [metadataByComponent] directly: they feed a hand-crafted Swift Export metadata JSON as the
 * single resolved artifact of a [LazyResolvedConfigurationWithArtifacts] and assert either that the metadata was parsed
 * correctly or that the expected diagnostic was reported.
 */
class SwiftExportMetadataConsumptionUnitTests {

    @Test
    fun `valid metadata is parsed`() {
        val result = consumeMetadata(
            """
            {"schemaVersion":$SWIFT_EXPORT_METADATA_SCHEMA_VERSION,"moduleName":"Foo","rootPackage":"com.foo.bar"}
            """.trimIndent()
        )

        assertTrue(result.diagnostics.isEmpty(), "No diagnostics expected for valid metadata, got: ${result.diagnostics}")
        val metadata = result.metadata.values.single()
        assertEquals(SWIFT_EXPORT_METADATA_SCHEMA_VERSION, metadata.schemaVersion)
        assertEquals("Foo", metadata.moduleName)
        assertEquals("com.foo.bar", metadata.rootPackage)
    }

    @Test
    fun `missing metadata is ignored`() {
        val result = consumeMetadata(metadataJson = null)

        assertTrue(result.diagnostics.isEmpty(), "No diagnostics expected for missing metadata, got: ${result.diagnostics}")
        assertTrue(result.metadata.isEmpty(), "No metadata expected, got: ${result.metadata}")
    }

    @Test
    fun `unknown fields in a supported schema are ignored`() {
        val result = consumeMetadata(
            """
            {"schemaVersion":$SWIFT_EXPORT_METADATA_SCHEMA_VERSION,"moduleName":"Foo","rootPackage":null,"unknownField":42,"nested":{"a":1}}
            """.trimIndent()
        )

        assertTrue(result.diagnostics.isEmpty(), "No diagnostics expected when ignoring unknown fields, got: ${result.diagnostics}")
        val metadata = result.metadata.values.single()
        assertEquals("Foo", metadata.moduleName)
        assertNull(metadata.rootPackage)
    }

    @Test
    fun `unsupported newer schema produces a warning and is ignored`() {
        val newerSchemaVersion = SWIFT_EXPORT_METADATA_SCHEMA_VERSION + 1
        val result = consumeMetadata(
            """
            {"schemaVersion":$newerSchemaVersion,"moduleName":"Foo","rootPackage":null}
            """.trimIndent()
        )

        assertTrue(result.metadata.isEmpty(), "Metadata with an unsupported schema version must be ignored")
        val diagnostic = result.diagnostics.single()
        assertEquals(KotlinToolingDiagnostics.SwiftExportUnsupportedMetadataSchemaVersion.id, diagnostic.id)
        assertEquals(KotlinToolingDiagnosticsSeverity.WARNING, diagnostic.severity)
    }

    @Test
    fun `malformed metadata for a supported schema fails with a diagnostic`() {
        val result = consumeMetadata("this is not valid json")

        assertTrue(result.metadata.isEmpty(), "Malformed metadata must not be parsed")
        val diagnostic = result.diagnostics.single()
        assertEquals(KotlinToolingDiagnostics.SwiftExportMalformedMetadata.id, diagnostic.id)
        assertEquals(KotlinToolingDiagnosticsSeverity.WARNING, diagnostic.severity)
    }

    private class MetadataConsumptionResult(
        val metadata: Map<ComponentIdentifier, SwiftExportMetadata>,
        val diagnostics: List<ToolingDiagnostic>,
    )

    /**
     * Builds a [LazyResolvedConfigurationWithArtifacts] whose single resolved artifact is a metadata JSON file carrying
     * [metadataJson], then reads it via [metadataByComponent], collecting any reported diagnostics.
     */
    private fun consumeMetadata(metadataJson: String?): MetadataConsumptionResult {
        val project = buildProject()

        // A resolvable configuration that depends on a consumable configuration of the same project, which exposes the
        // metadata JSON as its only artifact (see LazyResolvedConfigurationTest for the same self-dependency pattern).
        val resolvable = project.configurations.createResolvable("swiftExportMetadataForTest")
        val consumable = project.configurations.createConsumable("swiftExportMetadataForTestElements")
        project.dependencies.add(
            resolvable.name,
            project.dependencies.project(mapOf("path" to ":", "configuration" to consumable.name))
        )
        resolvable.extendsFrom(consumable)

        if (metadataJson != null) {
            val metadataFile = project.file("swift-export-metadata.json").apply { writeText(metadataJson) }
            project.artifacts.add(consumable.name, metadataFile)
        }

        val diagnostics = mutableListOf<ToolingDiagnostic>()
        val metadata = LazyResolvedConfigurationWithArtifacts(resolvable).metadataByComponent { diagnostics.add(it) }

        return MetadataConsumptionResult(metadata, diagnostics)
    }
}

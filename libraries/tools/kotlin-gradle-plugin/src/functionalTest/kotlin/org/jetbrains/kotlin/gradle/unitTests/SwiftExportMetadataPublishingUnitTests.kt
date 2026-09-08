/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalExportDsl::class, ExperimentalSwiftExportDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.kotlin.gradle.export.ExperimentalExportDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SWIFT_EXPORT_METADATA_SCHEMA_VERSION
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SwiftExportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.deserializeSwiftExportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.serializeSwiftExportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.export.tasks.SerializeSwiftExportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.export.tasks.locateOrRegisterSwiftExportMetadataTaskAndConsumableConfiguration
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.util.EMBED_SWIFT_EXPORT_TASK_NAME
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.exportDslProject
import org.jetbrains.kotlin.gradle.util.exportExtension
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * None of these tests need a macOS host: publishing depends on the project having an Apple target, which
 * `KonanTarget.family` reports regardless of the host.
 */
class SwiftExportMetadataPublishingUnitTests {

    private fun SwiftExportMetadata.roundTrip(): SwiftExportMetadata {
        val serialized = ByteArrayOutputStream()
        serializeSwiftExportMetadata(serialized)
        return deserializeSwiftExportMetadata(ByteArrayInputStream(serialized.toByteArray()))
    }

    @Test
    fun `swift export metadata round-trips module name and root package`() {
        val metadata = SwiftExportMetadata(moduleName = "Foo", rootPackage = "org.bar.foo")
        assertEquals(metadata, metadata.roundTrip())
    }

    @Test
    fun `swift export metadata round-trips with absent root package`() {
        val metadata = SwiftExportMetadata(moduleName = "Foo", rootPackage = null)
        assertEquals(metadata, metadata.roundTrip())
    }

    @Test
    fun `swift export metadata round-trips with absent module name`() {
        val metadata = SwiftExportMetadata(moduleName = null, rootPackage = "org.bar.foo")
        assertEquals(metadata, metadata.roundTrip())
    }

    @Test
    fun `swift export metadata is serialized with the current schema version`() {
        val serialized = ByteArrayOutputStream()
        SwiftExportMetadata(moduleName = "Foo", rootPackage = "org.bar.foo").serializeSwiftExportMetadata(serialized)

        val schemaVersion = Json.parseToJsonElement(serialized.toString(Charsets.UTF_8.name()))
            .jsonObject["schemaVersion"]?.jsonPrimitive?.int
        assertEquals(SWIFT_EXPORT_METADATA_SCHEMA_VERSION, schemaVersion)
    }

    @Test
    fun `deserialization reports the schema version written by the producer`() {
        val foreignVersion = SWIFT_EXPORT_METADATA_SCHEMA_VERSION + 1
        val payload = """{"schemaVersion":$foreignVersion,"moduleName":"Foo","rootPackage":"org.bar.foo"}"""

        val metadata = deserializeSwiftExportMetadata(ByteArrayInputStream(payload.toByteArray()))
        assertEquals(foreignVersion, metadata.schemaVersion)
    }

    @Test
    fun `registering the metadata task creates the consumable configuration`() {
        val project = buildProjectWithMPP()
        project.exportExtension.swift {
            moduleName.set("Foo")
            rootPackage.set("org.bar.foo")
        }

        project.locateOrRegisterSwiftExportMetadataTaskAndConsumableConfiguration(
            project.exportExtension.swiftExportConfiguration
        )

        assertNotNull(
            project.configurations.findByName(SWIFT_EXPORT_METADATA_ELEMENTS),
            "$SWIFT_EXPORT_METADATA_ELEMENTS configuration should be created when the metadata task is registered"
        )

        val serializeTask = project.tasks.withType(SerializeSwiftExportMetadata::class.java).single()
        assertEquals(
            SwiftExportMetadata(moduleName = "Foo", rootPackage = "org.bar.foo"),
            serializeTask.swiftExportMetadata()
        )
    }

    @Test
    fun `registering the metadata task twice reuses the existing task and configuration`() {
        val project = buildProjectWithMPP()
        project.exportExtension.swift {
            moduleName.set("Foo")
        }
        val configuration = project.exportExtension.swiftExportConfiguration

        val first = project.locateOrRegisterSwiftExportMetadataTaskAndConsumableConfiguration(configuration)
        val second = project.locateOrRegisterSwiftExportMetadataTaskAndConsumableConfiguration(configuration)

        assertEquals(first.name, second.name)
        assertEquals(1, project.tasks.withType(SerializeSwiftExportMetadata::class.java).size)
    }

    @Test
    fun `swift export metadata variant is published when the export DSL configures a module name`() {
        val project = exportDslProject(multiplatform = { iosArm64() }) {
            exportExtension.swift {
                moduleName.set("Foo")
                rootPackage.set("org.bar.foo")
            }
        }

        assertNotNull(
            project.configurations.findByName(SWIFT_EXPORT_METADATA_ELEMENTS),
            "$SWIFT_EXPORT_METADATA_ELEMENTS configuration should be created when the export DSL configures moduleName"
        )

        val serializeTask = project.tasks.withType(SerializeSwiftExportMetadata::class.java).single()
        assertEquals(
            SwiftExportMetadata(moduleName = "Foo", rootPackage = "org.bar.foo"),
            serializeTask.swiftExportMetadata()
        )
    }

    @Test
    fun `swift export metadata variant is published when only the root package is configured`() {
        val project = exportDslProject(multiplatform = { iosArm64() }) {
            exportExtension.swift {
                rootPackage.set("org.bar.foo")
            }
        }

        assertNotNull(
            project.configurations.findByName(SWIFT_EXPORT_METADATA_ELEMENTS),
            "$SWIFT_EXPORT_METADATA_ELEMENTS configuration should be created when only rootPackage is configured"
        )

        val serializeTask = project.tasks.withType(SerializeSwiftExportMetadata::class.java).single()
        assertEquals(
            SwiftExportMetadata(moduleName = null, rootPackage = "org.bar.foo"),
            serializeTask.swiftExportMetadata()
        )
    }

    @Test
    fun `swift export metadata variant is not published when the export DSL is never used`() {
        val project = exportDslProject(multiplatform = { iosArm64() })

        assertNull(
            project.configurations.findByName(SWIFT_EXPORT_METADATA_ELEMENTS),
            "$SWIFT_EXPORT_METADATA_ELEMENTS configuration should not be created when the export DSL is never used"
        )
    }

    @Test
    fun `swift export metadata variant is not published when swift block leaves both properties unset`() {
        val project = exportDslProject(multiplatform = { iosArm64() }) {
            exportExtension.swift { }
        }

        assertNull(
            project.configurations.findByName(SWIFT_EXPORT_METADATA_ELEMENTS),
            "$SWIFT_EXPORT_METADATA_ELEMENTS configuration should not be created when swift {} sets neither property"
        )
    }

    @Test
    fun `swift export metadata variant is not published without apple targets`() {
        val project = exportDslProject(multiplatform = { jvm() }) {
            exportExtension.swift {
                moduleName.set("Foo")
                rootPackage.set("org.bar.foo")
            }
        }

        assertNull(
            project.configurations.findByName(SWIFT_EXPORT_METADATA_ELEMENTS),
            "Swift Export only works for apple targets, so no metadata variant should be created without them"
        )
    }

    @Test
    fun `swift export metadata publication does not activate the xcode integration`() {
        val project = exportDslProject {
            exportExtension.swift {
                moduleName.set("Foo")
            }
        }

        assertNotNull(
            project.configurations.findByName(SWIFT_EXPORT_METADATA_ELEMENTS),
            "Metadata should still be published even though the Xcode integration was never activated"
        )
        assertNull(
            project.tasks.findByName(EMBED_SWIFT_EXPORT_TASK_NAME),
            "Publishing metadata must not activate the Xcode integration pipeline"
        )
    }

    private companion object {
        const val SWIFT_EXPORT_METADATA_ELEMENTS = "swiftExportMetadataElements"
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalSwiftExportDsl::class)

package org.jetbrains.kotlin.gradle.unitTests

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPublicationFormat
import org.jetbrains.kotlin.gradle.plugin.mpp.archive.KarLayout
import org.jetbrains.kotlin.gradle.plugin.mpp.export.internal.SWIFT_EXPORT_METADATA_ELEMENTS_NAME
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
import kotlin.test.assertTrue

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
            project.configurations.findByName(SWIFT_EXPORT_METADATA_ELEMENTS_NAME),
            "$SWIFT_EXPORT_METADATA_ELEMENTS_NAME configuration should be created when the metadata task is registered"
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
            project.configurations.findByName(SWIFT_EXPORT_METADATA_ELEMENTS_NAME),
            "$SWIFT_EXPORT_METADATA_ELEMENTS_NAME configuration should be created when the export DSL configures moduleName"
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
            project.configurations.findByName(SWIFT_EXPORT_METADATA_ELEMENTS_NAME),
            "$SWIFT_EXPORT_METADATA_ELEMENTS_NAME configuration should be created when only rootPackage is configured"
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
            project.configurations.findByName(SWIFT_EXPORT_METADATA_ELEMENTS_NAME),
            "$SWIFT_EXPORT_METADATA_ELEMENTS_NAME configuration should not be created when the export DSL is never used"
        )
    }

    @Test
    fun `swift export metadata variant is not published when swift block leaves both properties unset`() {
        val project = exportDslProject(multiplatform = { iosArm64() }) {
            exportExtension.swift { }
        }

        assertNull(
            project.configurations.findByName(SWIFT_EXPORT_METADATA_ELEMENTS_NAME),
            "$SWIFT_EXPORT_METADATA_ELEMENTS_NAME configuration should not be created when swift {} sets neither property"
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
            project.configurations.findByName(SWIFT_EXPORT_METADATA_ELEMENTS_NAME),
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
            project.configurations.findByName(SWIFT_EXPORT_METADATA_ELEMENTS_NAME),
            "Metadata should still be published even though the Xcode integration was never activated"
        )
        assertNull(
            project.tasks.findByName(EMBED_SWIFT_EXPORT_TASK_NAME),
            "Publishing metadata must not activate the Xcode integration pipeline"
        )
    }

    @Test
    fun `swift export metadata variant is published from the root component under the legacy format`() {
        val project = exportDslProject(multiplatform = { iosArm64() }) {
            exportExtension.swift {
                moduleName.set("Foo")
            }
        }

        val usage = project.multiplatformExtension.rootSoftwareComponent.usages.single { it.name == SWIFT_EXPORT_METADATA_ELEMENTS_NAME }
        val artifact = usage.artifacts.single()
        assertEquals("json", artifact.extension)
        assertEquals("swift-export-metadata", artifact.classifier)
        assertNull(
            usage.attributes.getAttribute(KarLayout.Attributes.compressionMethod),
            "The legacy variant must not carry the Kotlin Archive compression attribute"
        )
    }

    @Test
    fun `swift export metadata variant points at the Kotlin Archive under the KAR format`() {
        val project = exportDslProject(
            multiplatform = {
                iosArm64()
                publishing {
                    publicationFormat.set(KotlinPublicationFormat.KOTLIN_ARCHIVE)
                }
            }
        ) {
            exportExtension.swift {
                moduleName.set("Foo")
            }
        }

        val usages = project.multiplatformExtension.rootSoftwareComponent.usages
        assertNull(
            usages.find { it.name == SWIFT_EXPORT_METADATA_ELEMENTS_NAME },
            "Under KAR only the -published variant is part of the root component"
        )
        val usage = usages.single { it.name == "$SWIFT_EXPORT_METADATA_ELEMENTS_NAME-published" }
        val artifact = usage.artifacts.single()
        assertEquals(KarLayout.KAR_XZ_PACKED_EXTENSION, artifact.extension)
        assertEquals(
            KarLayout.Attributes.CompressionMethod.XZ,
            usage.attributes.getAttribute(KarLayout.Attributes.compressionMethod),
        )

        // The consumable configuration keeps the json; the archive is only published.
        val configurationArtifact = project.configurations.getByName(SWIFT_EXPORT_METADATA_ELEMENTS_NAME).artifacts.single()
        assertEquals("json", configurationArtifact.extension)
    }

    @Test
    fun `swift export metadata variant is no longer attached through the adhoc component`() {
        val project = exportDslProject(multiplatform = { iosArm64() }) {
            exportExtension.swift {
                moduleName.set("Foo")
            }
        }

        val adhocUsages = (project.multiplatformExtension.publishing.adhocSoftwareComponent as SoftwareComponentInternal).usages
        assertTrue(
            adhocUsages.none { it.name == SWIFT_EXPORT_METADATA_ELEMENTS_NAME },
            "Expected the adhoc component not to list $SWIFT_EXPORT_METADATA_ELEMENTS_NAME, got ${adhocUsages.map { it.name }}"
        )
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.kotlin.gradle.dependencyResolutionTests.configureRepositoriesForTests
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SWIFT_EXPORT_METADATA_SCHEMA_VERSION
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.SwiftExportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.deserializeSwiftExportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.serializeSwiftExportMetadata
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.swiftExportMetadataProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.tasks.SerializeSwiftExportMetadata
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.util.swiftExport
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.Assumptions
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SwiftExportMetadataSerializationTests {

    private fun SwiftExportMetadata.roundTrip(): SwiftExportMetadata {
        val serialized = ByteArrayOutputStream()
        serializeSwiftExportMetadata(serialized)
        return deserializeSwiftExportMetadata(ByteArrayInputStream(serialized.toByteArray()))
    }

    @Test
    fun `swift export metadata round-trips module name and flatten package`() {
        val metadata = SwiftExportMetadata(moduleName = "Foo", flattenPackage = "org.bar.foo")
        assertEquals(metadata, metadata.roundTrip())
    }

    @Test
    fun `swift export metadata round-trips with absent flatten package`() {
        val metadata = SwiftExportMetadata(moduleName = "Foo", flattenPackage = null)
        assertEquals(metadata, metadata.roundTrip())
    }

    @Test
    fun `swift export metadata round-trips with absent module name`() {
        val metadata = SwiftExportMetadata(moduleName = null, flattenPackage = "org.bar.foo")
        assertEquals(metadata, metadata.roundTrip())
    }

    @Test
    fun `swift export metadata is serialized with the current schema version`() {
        val serialized = ByteArrayOutputStream()
        SwiftExportMetadata(moduleName = "Foo", flattenPackage = "org.bar.foo").serializeSwiftExportMetadata(serialized)

        val schemaVersion = Json.parseToJsonElement(serialized.toString(Charsets.UTF_8.name()))
            .jsonObject["schemaVersion"]?.jsonPrimitive?.int
        assertEquals(SWIFT_EXPORT_METADATA_SCHEMA_VERSION, schemaVersion)
    }

    @Test
    fun `deserialization reports the schema version written by the producer`() {
        val foreignVersion = SWIFT_EXPORT_METADATA_SCHEMA_VERSION + 1
        val payload = """{"schemaVersion":$foreignVersion,"moduleName":"Foo","flattenPackage":"org.bar.foo"}"""

        val metadata = deserializeSwiftExportMetadata(ByteArrayInputStream(payload.toByteArray()))
        assertEquals(foreignVersion, metadata.schemaVersion)
    }

    @Test
    fun `swift export metadata variant is published when module name or flatten package is configured`() {
        Assumptions.assumeTrue(HostManager.hostIsMac, "macOS host required for this test")
        with(buildProjectWithMPP()) {
            kotlin {
                iosArm64()
                swiftExport {
                    moduleName.set("Foo")
                    flattenPackage.set("org.bar.foo")
                }
            }
            configureRepositoriesForTests()
            evaluate()

            assertNotNull(
                configurations.findByName("swiftExportMetadataElements"),
                "swiftExportMetadataElements configuration should be created when swiftExport is configured"
            )

            val serializeTask = tasks.withType(SerializeSwiftExportMetadata::class.java).single()
            assertEquals(
                SwiftExportMetadata(moduleName = "Foo", flattenPackage = "org.bar.foo"),
                serializeTask.swiftExportMetadata()
            )
        }
    }

    @Test
    fun `swift export metadata variant is not published when nothing is configured`() {
        Assumptions.assumeTrue(HostManager.hostIsMac, "macOS host required for this test")
        with(buildProjectWithMPP()) {
            kotlin {
                iosArm64()
                swiftExport { }
            }
            configureRepositoriesForTests()
            evaluate()

            assertNull(
                configurations.findByName("swiftExportMetadataElements"),
                "swiftExportMetadataElements configuration should not be created when swiftExport is not configured"
            )
        }
    }

    @Test
    fun `swift export metadata variant is published when only the flatten package is configured`() {
        Assumptions.assumeTrue(HostManager.hostIsMac, "macOS host required for this test")
        with(buildProjectWithMPP()) {
            kotlin {
                iosArm64()
                swiftExport {
                    flattenPackage.set("org.bar.foo")
                }
            }
            configureRepositoriesForTests()
            evaluate()

            assertNotNull(
                configurations.findByName("swiftExportMetadataElements"),
                "swiftExportMetadataElements configuration should be created when only flattenPackage is configured"
            )

            val serializeTask = tasks.withType(SerializeSwiftExportMetadata::class.java).single()
            assertEquals(
                SwiftExportMetadata(moduleName = null, flattenPackage = "org.bar.foo"),
                serializeTask.swiftExportMetadata()
            )
        }
    }

    @Test
    fun `swift export metadata variant is not published when the project has no apple targets`() {
        with(buildProjectWithMPP()) {
            kotlin {
                jvm()
                swiftExport {
                    moduleName.set("Foo")
                    flattenPackage.set("org.bar.foo")
                }
            }
            configureRepositoriesForTests()
            evaluate()

            assertNull(
                configurations.findByName("swiftExportMetadataElements"),
                "Swift Export only works for apple targets, so no metadata variant should be created without them"
            )
        }
    }

    @Test
    fun `swift export metadata provider is empty when no dependencies publish metadata`() {
        Assumptions.assumeTrue(HostManager.hostIsMac, "macOS host required for this test")
        with(buildProjectWithMPP()) {
            kotlin {
                iosArm64()
            }
            configureRepositoriesForTests()
            evaluate()

            assertTrue(
                swiftExportMetadataProvider().get().isEmpty(),
                "No Swift Export metadata should be resolved without dependencies that publish it"
            )
        }
    }
}

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.Opaque
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.XCLocalSwiftPackageReference
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.PbxProject
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.deserializeXcodeProject
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.serializeXcodeProject
import org.jetbrains.kotlin.gradle.testing.XcodeProjectSerializationFixtures
import org.jetbrains.kotlin.gradle.util.assertIsInstance
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class XcodeProjectSerializationTests {

    @Test
    fun `reserializing pbxproj - persists the project structure`() {
        val reserializedJson: JsonElement = Json.decodeFromString<JsonElement>(
            ByteArrayOutputStream().use {
                deserializeXcodeProject(XcodeProjectSerializationFixtures.sampleXcodeProjectWithEmbedAndSignIntegration.toByteArray())
                    .serializeXcodeProject(it)
                it.toString()
            },
        )

        val originalJson = Json.decodeFromString<JsonElement>(
            XcodeProjectSerializationFixtures.sampleXcodeProjectWithEmbedAndSignIntegration,
        )

        assertEquals(
            originalJson,
            reserializedJson,
        )
    }

    @Test
    fun `adding XCLocalSwiftPackageReference entry in the objects map - persists existing project structure`() {
        val inputProject = """
            {
              "classes": {},
              "unknownProperty": 1,
              "archiveVersion": "1",
              "objects": {
                "1": {
                  "isa": "UnknownIsa",
                  "unknownProperty": {
                    "foo": "bar"
                  }
                }
              }
            }
            """.trimIndent()

        val project = deserializeXcodeProject(inputProject.toByteArray())
        project.objects["2"] = XCLocalSwiftPackageReference(
            relativePath = "/path/to/package"
        )

        assertIsInstance<Opaque>(project.objects["1"])

        val reserializedJson: JsonElement = Json.decodeFromString<JsonElement>(
            ByteArrayOutputStream().use {
                project.serializeXcodeProject(it)
                it.toString()
            },
        )

        val outputProject = """
            {
              "classes": {},
              "unknownProperty": 1,
              "archiveVersion": "1",
              "objects": {
                "1": {
                  "isa": "UnknownIsa",
                  "unknownProperty": {
                    "foo": "bar"
                  }
                },
                "2": {
                  "isa": "XCLocalSwiftPackageReference",
                  "relativePath": "/path/to/package"
                }
              }
            }
        """.trimIndent()

        assertEquals(
            Json.decodeFromString<JsonElement>(outputProject),
            reserializedJson,
        )
    }

    @Test
    fun `mutating XCLocalSwiftPackageReference entry with unknown properties - persists unknown properties in XCLocalSwiftPackageReference`() {
        val inputProject = """
            {
              "classes": {},
              "archiveVersion": "1",
              "objects": {
                "2": {
                  "isa": "XCLocalSwiftPackageReference",
                  "unknownProperty": {
                    "listSubproperty": [
                       {
                         "foo": 1
                       }
                    ]
                  },
                  "relativePath": "/path/to/package"
                }
              }
            }
            """.trimIndent()

        val project = deserializeXcodeProject(inputProject.toByteArray())
        (project.objects["2"] as XCLocalSwiftPackageReference).relativePath = "/path/to/mutated"

        val reserializedJson: JsonElement = Json.decodeFromString<JsonElement>(
            ByteArrayOutputStream().use {
                project.serializeXcodeProject(it)
                it.toString()
            },
        )

        val outputProject = """
            {
              "classes": {},
              "archiveVersion": "1",
              "objects": {
                "2": {
                  "isa": "XCLocalSwiftPackageReference",
                  "unknownProperty": {
                    "listSubproperty": [
                       {
                         "foo": 1
                       }
                    ]
                  },
                  "relativePath": "/path/to/mutated"
                }
              }
            }
        """.trimIndent()

        assertEquals(
            Json.decodeFromString<JsonElement>(outputProject),
            reserializedJson,
        )
    }

    @Test
    fun `mutating PBXProject build phases - persists unknown properties in PBXProject`() {
        val inputProject = """
            {
              "classes": {},
              "archiveVersion": "1",
              "objects": {
                "3": {
                  "isa": "PBXProject",
                  "unknownProperty": {
                    "listSubproperty": [
                       {
                         "foo": 1
                       }
                    ]
                  },
                  "buildPhases": [
                    "1"
                  ]
                }
              }
            }
            """.trimIndent()

        val project = deserializeXcodeProject(inputProject.toByteArray())
        (project.objects["3"] as PbxProject).buildPhases?.add("2")

        val reserializedJson: JsonElement = Json.decodeFromString<JsonElement>(
            ByteArrayOutputStream().use {
                project.serializeXcodeProject(it)
                it.toString()
            },
        )

        val outputProject = """
            {
              "classes": {},
              "archiveVersion": "1",
              "objects": {
                "3": {
                  "isa": "PBXProject",
                  "unknownProperty": {
                    "listSubproperty": [
                       {
                         "foo": 1
                       }
                    ]
                  },
                  "buildPhases": [
                    "1",
                    "2"
                  ]
                }
              }
            }
        """.trimIndent()

        assertEquals(
            Json.decodeFromString<JsonElement>(outputProject),
            reserializedJson,
        )
    }
}
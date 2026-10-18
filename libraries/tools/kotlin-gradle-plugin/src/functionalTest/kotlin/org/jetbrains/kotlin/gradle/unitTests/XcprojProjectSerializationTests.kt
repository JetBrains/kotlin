/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.gradle.unitTests

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.xcproj.*
import org.jetbrains.kotlin.gradle.testing.PrettyPrint
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.testing.sampleXcprojProject
import java.io.ByteArrayOutputStream
import kotlin.test.*

class XcprojProjectSerializationTests {
    @Test
    fun `round trip reserialization - preserves xcproj including unknown fields`() {
        assertEquals<PrettyPrint<JsonElement>>(
            json.parseToJson5Element(sampleXcprojProject).prettyPrinted,
            deserializeXcprojProject(
                sampleXcprojProject
            ).toJson().prettyPrinted
        )
    }

    @Test
    fun `integrateEmbedAndSign - mutates script phase and the sandboxing build setting`() {
        val original = json.parseToJson5Element(sampleXcprojProject).jsonObject
        val project =
            deserializeXcprojProject(sampleXcprojProject)
        assertTrue(project.integrateEmbedAndSign("./gradlew :shared:embedAndSignAppleFrameworkForXcode"))
        val targets = original.getValue("targets").jsonArray
        val native = targets[0].jsonObject
        val aggregate = targets[1].jsonObject
        val embedAndSignScript = json.parseToJson5Element(
            """
            {
              "kind": "script",
              "name": "Compile Kotlin Framework",
              "shell": "/bin/sh",
              "script": "./gradlew :shared:embedAndSignAppleFrameworkForXcode",
              "run-on-every-build": true
            }
          """
        )
        val expected = original.replacing(
            "targets" to JsonArray(
                listOf(
                    native.replacing(
                        "build-settings" to native.getValue("build-settings").jsonObject.replacing(
                            "ENABLE_USER_SCRIPT_SANDBOXING" to JsonPrimitive("NO"),
                        ),
                        "build-phases" to JsonArray(listOf(embedAndSignScript) + native.getValue("build-phases").jsonArray),
                    ),
                    aggregate,
                )
            ),
        )
        assertEquals(expected.prettyPrinted, project.toJson().prettyPrinted)
        assertFalse(project.integrateEmbedAndSign("different script"))
        assertEquals(expected, project.toJson())
    }

    @Test
    fun `integrateLinkagePackage - preserves existing package-product-members and dependencies`() {
        val project =
            deserializeXcprojProject(sampleXcprojProject)
        project.integrateEmbedAndSign("./gradlew embedAndSignAppleFrameworkForXcode")
        val before = project.toJson()
        assertTrue(project.integrateLinkagePackage())
        val name = GenerateSyntheticLinkageImportProject.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME
        val targets = before.getValue("targets").jsonArray
        val target = targets[0].jsonObject
        val expected = before.replacing(
            "packages" to json.parseToJson5Element(
                """
                    [
                      {
                        "kind": "local",
                        "path": "$name"
                      }
                    ]
                   """.trimMargin()
            ),
            "targets" to JsonArray(
                listOf(
                    target.replacing(
                        "dependencies" to json.parseToJson5Element(
                            """
                             [
                                "ExistingDependencyLiteral",
                                {
                                  "kind": "opaque",
                                  "foo": "bar",
                                },
                                {
                                  "kind": "package",
                                  "product-name": "$name"
                                }
                             ]
                            """.trimIndent()
                        ),
                        "package-product-members" to json.parseToJson5Element(
                            """
                            [
                              {
                                "product-name": "Something",
                                "build-phase": {
                                  "build-phase": "frameworks"
                                }
                              },
                              {
                                "product-name": "$name",
                                "build-phase": {
                                  "build-phase": "frameworks"
                                }
                              },
                              {
                                "product-name": "$name",
                                "build-phase": {
                                  "build-phase": "id:222222222222222222222222"
                                }
                              }
                            ]
                          """
                        ),
                    ),
                    targets[1],
                )
            ),
        )
        assertEquals(expected.prettyPrinted, project.toJson().prettyPrinted)
        assertTrue(project.hasLinkagePackageReference())
        assertFalse(project.integrateLinkagePackage())
    }

    @Test
    fun `compact and named framework phases and existing local packages are supported`() {
        val name = GenerateSyntheticLinkageImportProject.SYNTHETIC_IMPORT_TARGET_MAGIC_NAME
        val input = """
            {
              "packages": [
                {
                  "kind": "local",
                  "path": "$name",
                  "future": true
                }
              ],
              "targets": [
                {
                  "build-phases": [
                    "frameworks",
                    {
                      "kind": "frameworks",
                      "name": "Special/Phase"
                    },
                    {
                      "kind": "script",
                      "shell": "/bin/sh",
                      "script": [
                        "echo start",
                        "./gradlew task"
                      ]
                    }
                  ]
                }
              ]
            }
        """
        val project = deserializeXcprojProject(input)
        project.integrateLinkagePackage()
        val output = project.toJson()
        assertEquals(
            json.parseToJson5Element(input).jsonObject["packages"],
            output["packages"],
        )
        assertEquals(
            json.parseToJson5Element(
                """
            [
              {
                "product-name": "$name",
                "build-phase": {
                  "build-phase": "frameworks"
                }
              },
              {
                "product-name": "$name",
                "build-phase": {
                  "build-phase": "Special/Phase"
                }
              }
            ]
          """
            ).prettyPrinted, output.getValue("targets").jsonArray.single().jsonObject["package-product-members"]!!.prettyPrinted
        )
    }

    @Test
    fun `missing native targets - throw MissingEmbedAndSignIntegration`() {
        val empty =
            deserializeXcprojProject("""{"targets":[{"kind":"aggregate"}]}""")
        val before = empty.toJson()
        assertFailsWith<MissingTargetsForEmbedAndSignIntegration> { empty.integrateEmbedAndSign("gradle") }
        assertFailsWith<MissingForLinkagePackageIntegration> { empty.integrateLinkagePackage() }
        assertEquals(before, empty.toJson())
    }

    @Test
    fun `comments and trailing commas - deserialize`() {
        val project = deserializeXcprojProject(
            """
            { // project comment
              "unknown": "https://example.org/*literal*/,",
              "targets": [{"name":"App", /* target comment */},],
            }
        """
        )
        assertEquals(
            json.parseToJson5Element("""{"unknown":"https://example.org/*literal*/,","targets":[{"name":"App"}]}"""),
            project.toJson()
        )
    }

    private fun XcprojProject.toJson(): JsonObject = ByteArrayOutputStream().use {
        serializeXcprojProject(it)
        json.parseToJson5Element(it.toString("UTF-8")).jsonObject
    }

    private fun Json.parseToJson5Element(@Language("JSON5") json: String) = parseToJsonElement(json)

    private fun JsonObject.replacing(vararg entries: Pair<String, JsonElement>) = JsonObject(this + entries)

    private val json = Json { allowTrailingComma = true }
}

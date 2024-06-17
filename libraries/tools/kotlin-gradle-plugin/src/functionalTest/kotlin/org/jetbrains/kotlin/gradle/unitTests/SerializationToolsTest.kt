/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.SerializationTools
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportFiles
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModule
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class SerializationToolsTest {

    @Test
    fun `test hierarchy SwiftModule serialization`() {
        val json = SerializationTools.writeToJson(hierarchyModules())
        val hierarchyJson = hierarchyJson()

        assertEquals(json.unixStylePath(), hierarchyJson)
    }

    @Test
    fun `test hierarchy SwiftModule deserialization`() {
        val modules = SerializationTools.readFromJson<List<GradleSwiftExportModule>>(hierarchyJson())
        val hierarchyModules = hierarchyModules()

        assertEquals(modules, hierarchyModules)
    }

    @Test
    fun `test hierarchy SwiftModule equality`() {
        val modulesA = hierarchyModules()
        val modulesB = hierarchyModules()

        assertEquals(modulesA, modulesB)
    }

    @Test
    fun `test nested SwiftModule serialization`() {
        val json = SerializationTools.writeToJson(nestedModules())
        val nestedJson = nestedJson()

        assertEquals(json.unixStylePath(), nestedJson)
    }

    @Test
    fun `test nested SwiftModule deserialization`() {
        val modules = SerializationTools.readFromJson<List<GradleSwiftExportModule>>(nestedJson())
        val nestedModules = nestedModules()

        assertEquals(modules, nestedModules)
    }

    @Test
    fun `test nested SwiftModule equality`() {
        val modulesA = nestedModules()
        val modulesB = nestedModules()

        assertEquals(modulesA, modulesB)
    }

    @Test
    fun `test Windows Unix SwiftModule paths`() {
        val json = SerializationTools.writeToJson(simpleModules())
        val simpleJson = simpleJson()
        val simpleWinJson = simpleWindowsJson()

        if (HostManager.hostIsMingw) {
            assertEquals(json, simpleWinJson)
        } else if (HostManager.hostIsMac || HostManager.hostIsLinux) {
            assertEquals(json, simpleJson)
            assertEquals(json, simpleWinJson.unixStylePath())
        }
    }

    private fun simpleJson(): String = """
        [
          {
            "files": {
              "swiftApi": "/A/SwiftFile.swift",
              "kotlinBridges": "/A/KotlinBridge.kt",
              "cHeaderBridges": "/A/Header.h"
            },
            "bridgeName": "Bridge_A",
            "name": "Module_A",
            "type": "BRIDGES_TO_KOTLIN",
            "dependencies": []
          }
        ]
    """.trimIndent()

    private fun simpleWindowsJson(): String = """
        [
          {
            "files": {
              "swiftApi": "\\A\\SwiftFile.swift",
              "kotlinBridges": "\\A\\KotlinBridge.kt",
              "cHeaderBridges": "\\A\\Header.h"
            },
            "bridgeName": "Bridge_A",
            "name": "Module_A",
            "type": "BRIDGES_TO_KOTLIN",
            "dependencies": []
          }
        ]
    """.trimIndent()

    private fun simpleModules(): List<GradleSwiftExportModule> = listOf(
        GradleSwiftExportModule.BridgesToKotlin(
            GradleSwiftExportFiles(
                File("/A/SwiftFile.swift"),
                File("/A/KotlinBridge.kt"),
                File("/A/Header.h")
            ),
            "Bridge_A",
            "Module_A",
            emptyList()
        )
    )

    /*
     *  Module_A
     *  ├── Module_C
     *  └── Module_D
     *
     *  Module_B
     *  ├── Module_C
     *  └── Module_E
     */
    private fun hierarchyJson(): String = """
        [
          {
            "files": {
              "swiftApi": "/A/SwiftFile.swift",
              "kotlinBridges": "/A/KotlinBridge.kt",
              "cHeaderBridges": "/A/Header.h"
            },
            "bridgeName": "Bridge_A",
            "name": "Module_A",
            "type": "BRIDGES_TO_KOTLIN",
            "dependencies": [
              "Module_C",
              "Module_D"
            ]
          },
          {
            "files": {
              "swiftApi": "/B/SwiftFile.swift",
              "kotlinBridges": "/B/KotlinBridge.kt",
              "cHeaderBridges": "/B/Header.h"
            },
            "bridgeName": "Bridge_B",
            "name": "Module_B",
            "type": "BRIDGES_TO_KOTLIN",
            "dependencies": [
              "Module_C",
              "Module_E"
            ]
          },
          {
            "swiftApi": "/C/SwiftFile.swift",
            "name": "Module_C",
            "type": "SWIFT_ONLY",
            "dependencies": []
          },
          {
            "swiftApi": "/D/SwiftFile.swift",
            "name": "Module_D",
            "type": "SWIFT_ONLY",
            "dependencies": []
          },
          {
            "swiftApi": "/E/SwiftFile.swift",
            "name": "Module_E",
            "type": "SWIFT_ONLY",
            "dependencies": []
          }
        ]
    """.trimIndent()

    private fun hierarchyModules(): List<GradleSwiftExportModule> = listOf(
        GradleSwiftExportModule.BridgesToKotlin(
            GradleSwiftExportFiles(
                File("/A/SwiftFile.swift"),
                File("/A/KotlinBridge.kt"),
                File("/A/Header.h")
            ),
            "Bridge_A",
            "Module_A",
            listOf("Module_C", "Module_D")
        ),
        GradleSwiftExportModule.BridgesToKotlin(
            GradleSwiftExportFiles(
                File("/B/SwiftFile.swift"),
                File("/B/KotlinBridge.kt"),
                File("/B/Header.h")
            ),
            "Bridge_B",
            "Module_B",
            listOf("Module_C", "Module_E")
        ),
        GradleSwiftExportModule.SwiftOnly(
            File("/C/SwiftFile.swift"),
            "Module_C",
            emptyList()
        ),
        GradleSwiftExportModule.SwiftOnly(
            File("/D/SwiftFile.swift"),
            "Module_D",
            emptyList()
        ),
        GradleSwiftExportModule.SwiftOnly(
            File("/E/SwiftFile.swift"),
            "Module_E",
            emptyList()
        )
    )

    /*
     * Module_A
     * └── Module_B
     *     └── Module_C
     *         └── Module_D
     *             └── Module_E
     */
    private fun nestedJson() = """
        [
          {
            "files": {
              "swiftApi": "/A/SwiftFile.swift",
              "kotlinBridges": "/A/KotlinBridge.kt",
              "cHeaderBridges": "/A/Header.h"
            },
            "bridgeName": "Bridge_A",
            "name": "Module_A",
            "type": "BRIDGES_TO_KOTLIN",
            "dependencies": [
              "Module_B"
            ]
          },
          {
            "files": {
              "swiftApi": "/B/SwiftFile.swift",
              "kotlinBridges": "/B/KotlinBridge.kt",
              "cHeaderBridges": "/B/Header.h"
            },
            "bridgeName": "Bridge_B",
            "name": "Module_B",
            "type": "BRIDGES_TO_KOTLIN",
            "dependencies": [
              "Module_C"
            ]
          },
          {
            "swiftApi": "/C/SwiftFile.swift",
            "name": "Module_C",
            "type": "SWIFT_ONLY",
            "dependencies": [
              "Module_D"
            ]
          },
          {
            "swiftApi": "/D/SwiftFile.swift",
            "name": "Module_D",
            "type": "SWIFT_ONLY",
            "dependencies": [
              "Module_E"
            ]
          },
          {
            "swiftApi": "/E/SwiftFile.swift",
            "name": "Module_E",
            "type": "SWIFT_ONLY",
            "dependencies": []
          }
        ]
    """.trimIndent()

    private fun nestedModules(): List<GradleSwiftExportModule> = listOf(
        GradleSwiftExportModule.BridgesToKotlin(
            GradleSwiftExportFiles(
                File("/A/SwiftFile.swift"),
                File("/A/KotlinBridge.kt"),
                File("/A/Header.h")
            ),
            "Bridge_A",
            "Module_A",
            listOf("Module_B")
        ),
        GradleSwiftExportModule.BridgesToKotlin(
            GradleSwiftExportFiles(
                File("/B/SwiftFile.swift"),
                File("/B/KotlinBridge.kt"),
                File("/B/Header.h")
            ),
            "Bridge_B",
            "Module_B",
            listOf("Module_C")
        ),
        GradleSwiftExportModule.SwiftOnly(
            File("/C/SwiftFile.swift"),
            "Module_C",
            listOf("Module_D")
        ),
        GradleSwiftExportModule.SwiftOnly(
            File("/D/SwiftFile.swift"),
            "Module_D",
            listOf("Module_E")
        ),
        GradleSwiftExportModule.SwiftOnly(
            File("/E/SwiftFile.swift"),
            "Module_E",
            emptyList()
        )
    )
}

private fun String.unixStylePath() = this.replace("\\\\", "/")
/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.SerializationTools
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportFiles
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModules
import org.jetbrains.kotlin.gradle.util.resourcesRoot
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Test
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals

class SerializationToolsTest {

    @Test
    fun `test hierarchy SwiftModule serialization`() {
        val json = SerializationTools.writeToJson(GradleSwiftExportModules(hierarchyModules(), 1721919536167))
        val hierarchyJson = testJson("hierarchyJson").readText()

        assertEquals(json.unixStylePath(), hierarchyJson)
    }

    @Test
    fun `test hierarchy SwiftModule deserialization`() {
        val modules = SerializationTools.readFromJson<GradleSwiftExportModules>(
            testJson("hierarchyJson").readText()
        )
        val hierarchyModules = GradleSwiftExportModules(hierarchyModules(), 1721919536167)

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
        val json = SerializationTools.writeToJson(GradleSwiftExportModules(nestedModules(), 1721919536167))
        val nestedJson = testJson("nestedJson").readText()

        assertEquals(json.unixStylePath(), nestedJson)
    }

    @Test
    fun `test nested SwiftModule deserialization`() {
        val modules = SerializationTools.readFromJson<GradleSwiftExportModules>(
            testJson("nestedJson").readText()
        )
        val nestedModules = GradleSwiftExportModules(nestedModules(), 1721919536167)

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
        val json = SerializationTools.writeToJson(GradleSwiftExportModules(simpleModules(), 1721919536167))
        val simpleJson = testJson("simpleJson").readText()
        val simpleWinJson = testJson("simpleWindowsJson").readText()

        if (HostManager.hostIsMingw) {
            assertEquals(json, simpleWinJson)
        } else if (HostManager.hostIsMac || HostManager.hostIsLinux) {
            assertEquals(json, simpleJson)
            assertEquals(json, simpleWinJson.unixStylePath())
        }
    }

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

private val serializationToolsTestFilesRoot: Path
    get() = resourcesRoot.resolve("testData/SerializationToolsTest")

private fun testJson(fileName: String): File = serializationToolsTestFilesRoot.resolve("$fileName.json").toFile()

private fun String.unixStylePath() = this.replace("\\\\", "/")
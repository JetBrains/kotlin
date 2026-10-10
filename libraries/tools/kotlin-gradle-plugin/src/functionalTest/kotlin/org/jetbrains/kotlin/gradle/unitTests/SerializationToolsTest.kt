/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.SerializationTools
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportFiles
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModule
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.GradleSwiftExportModules
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.kotlin.gradle.util.resourcesRoot
import java.io.File
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SerializationToolsTest {

    /** The directory with the modules file. The generated files are in its `files` directory. */
    private val baseDirectory = File("SwiftExport/iosArm64").absoluteFile

    private fun file(path: String) = baseDirectory.resolve("files").resolve(path)

    @Test
    fun `test hierarchy SwiftModule serialization`() {
        val json = SerializationTools.writeToJson(GradleSwiftExportModules(hierarchyModules()), baseDirectory)
        val hierarchyJson = testJson("hierarchyJson").readText()

        assertEquals(hierarchyJson, json)
    }

    @Test
    fun `test hierarchy SwiftModule deserialization`() {
        val modules = SerializationTools.readFromJson(
            testJson("hierarchyJson").readText(),
            baseDirectory,
        )
        val hierarchyModules = GradleSwiftExportModules(hierarchyModules())

        assertEquals(hierarchyModules, modules)
    }

    @Test
    fun `test hierarchy SwiftModule equality`() {
        val modulesA = hierarchyModules()
        val modulesB = hierarchyModules()

        assertEquals(modulesA, modulesB)
    }

    @Test
    fun `test nested SwiftModule serialization`() {
        val json = SerializationTools.writeToJson(GradleSwiftExportModules(nestedModules()), baseDirectory)
        val nestedJson = testJson("nestedJson").readText()

        assertEquals(nestedJson, json)
    }

    @Test
    fun `test nested SwiftModule deserialization`() {
        val modules = SerializationTools.readFromJson(
            testJson("nestedJson").readText(),
            baseDirectory,
        )
        val nestedModules = GradleSwiftExportModules(nestedModules())

        assertEquals(nestedModules, modules)
    }

    @Test
    fun `test nested SwiftModule equality`() {
        val modulesA = nestedModules()
        val modulesB = nestedModules()

        assertEquals(modulesA, modulesB)
    }

    @Test
    fun `test the same JSON is written on every host`() {
        val json = SerializationTools.writeToJson(GradleSwiftExportModules(simpleModules()), baseDirectory)

        assertEquals(testJson("simpleJson").readText(), json)
    }

    @Test
    fun `test the modules file holds paths relative to its directory`() {
        val json = SerializationTools.writeToJson(GradleSwiftExportModules(hierarchyModules()), baseDirectory)
        val modules = Json.parseToJsonElement(json).jsonObject.getValue("modules").jsonArray.map { it.jsonObject }

        val writtenPaths = modules.flatMap { module ->
            val files = module["files"]?.jsonObject
            listOfNotNull(
                files?.get("swiftApi"),
                files?.get("kotlinBridges"),
                files?.get("cHeaderBridges"),
                module["swiftApi"],
            ).map { it.jsonPrimitive.content }
        }

        assertEquals(
            listOf(
                "files/A/SwiftFile.swift", "files/A/KotlinBridge.kt", "files/A/Header.h",
                "files/B/SwiftFile.swift", "files/B/KotlinBridge.kt", "files/B/Header.h",
                "files/C/SwiftFile.swift",
                "files/D/SwiftFile.swift",
                "files/E/SwiftFile.swift",
            ),
            writtenPaths,
        )
    }

    @Test
    fun `test the modules file can be read from another location`() {
        val json = SerializationTools.writeToJson(GradleSwiftExportModules(simpleModules()), baseDirectory)
        val movedDirectory = File("moved/SwiftExport/iosArm64").absoluteFile

        val modules = SerializationTools.readFromJson(json, movedDirectory)

        assertEquals(
            GradleSwiftExportFiles(
                movedDirectory.resolve("files/A/SwiftFile.swift"),
                movedDirectory.resolve("files/A/KotlinBridge.kt"),
                movedDirectory.resolve("files/A/Header.h"),
            ),
            (modules.modules.single() as GradleSwiftExportModule.BridgesToKotlin).files,
        )
    }

    @Test
    fun `test a file outside of the base directory is rejected`() {
        val outside = GradleSwiftExportModule.SwiftOnly(
            baseDirectory.resolveSibling("macosArm64/files/A/SwiftFile.swift"),
            "Module_A",
            emptyList(),
        )

        assertFailsWith<IllegalArgumentException> {
            SerializationTools.writeToJson(GradleSwiftExportModules(listOf(outside)), baseDirectory)
        }
    }

    private fun simpleModules(): List<GradleSwiftExportModule> = listOf(
        GradleSwiftExportModule.BridgesToKotlin(
            GradleSwiftExportFiles(
                file("A/SwiftFile.swift"),
                file("A/KotlinBridge.kt"),
                file("A/Header.h")
            ),
            "Bridge_A",
            "Module_A",
            emptyList()
        )
    )

    private fun hierarchyModules(): List<GradleSwiftExportModule> = listOf(
        GradleSwiftExportModule.BridgesToKotlin(
            GradleSwiftExportFiles(
                file("A/SwiftFile.swift"),
                file("A/KotlinBridge.kt"),
                file("A/Header.h")
            ),
            "Bridge_A",
            "Module_A",
            listOf("Module_C", "Module_D")
        ),
        GradleSwiftExportModule.BridgesToKotlin(
            GradleSwiftExportFiles(
                file("B/SwiftFile.swift"),
                file("B/KotlinBridge.kt"),
                file("B/Header.h")
            ),
            "Bridge_B",
            "Module_B",
            listOf("Module_C", "Module_E")
        ),
        GradleSwiftExportModule.SwiftOnly(
            file("C/SwiftFile.swift"),
            "Module_C",
            emptyList()
        ),
        GradleSwiftExportModule.SwiftOnly(
            file("D/SwiftFile.swift"),
            "Module_D",
            emptyList()
        ),
        GradleSwiftExportModule.SwiftOnly(
            file("E/SwiftFile.swift"),
            "Module_E",
            emptyList()
        )
    )

    private fun nestedModules(): List<GradleSwiftExportModule> = listOf(
        GradleSwiftExportModule.BridgesToKotlin(
            GradleSwiftExportFiles(
                file("A/SwiftFile.swift"),
                file("A/KotlinBridge.kt"),
                file("A/Header.h")
            ),
            "Bridge_A",
            "Module_A",
            listOf("Module_B")
        ),
        GradleSwiftExportModule.BridgesToKotlin(
            GradleSwiftExportFiles(
                file("B/SwiftFile.swift"),
                file("B/KotlinBridge.kt"),
                file("B/Header.h")
            ),
            "Bridge_B",
            "Module_B",
            listOf("Module_C")
        ),
        GradleSwiftExportModule.SwiftOnly(
            file("C/SwiftFile.swift"),
            "Module_C",
            listOf("Module_D")
        ),
        GradleSwiftExportModule.SwiftOnly(
            file("D/SwiftFile.swift"),
            "Module_D",
            listOf("Module_E")
        ),
        GradleSwiftExportModule.SwiftOnly(
            file("E/SwiftFile.swift"),
            "Module_E",
            emptyList()
        )
    )
}

private val serializationToolsTestFilesRoot: Path
    get() = resourcesRoot.resolve("testData/SerializationToolsTest")

private fun testJson(fileName: String): File = serializationToolsTestFilesRoot.resolve("$fileName.json").toFile()

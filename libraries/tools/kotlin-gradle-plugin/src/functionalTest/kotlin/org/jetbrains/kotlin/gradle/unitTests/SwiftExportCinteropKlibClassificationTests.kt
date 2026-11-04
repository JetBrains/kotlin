/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.isCinteropKlib
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport.internal.isSwiftPMImportCinteropKlib
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class SwiftExportCinteropKlibClassificationTests {

    private data class Classification(val isCinterop: Boolean, val isSwiftPMImportCinterop: Boolean)

    private fun classify(vararg fileNames: String): Map<String, Classification> = fileNames.associateWith {
        val file = File(it)
        Classification(isCinterop = file.isCinteropKlib, isSwiftPMImportCinterop = file.isSwiftPMImportCinteropKlib)
    }

    @Test
    fun `swiftPMImport cinterop klib of a project dependency`() {
        assertEquals(
            mapOf("demo-cinterop-swiftPMImport" to Classification(isCinterop = true, isSwiftPMImportCinterop = true)),
            classify("demo-cinterop-swiftPMImport"),
        )
    }

    @Test
    fun `swiftPMImport cinterop klib of a published dependency`() {
        val expected = Classification(isCinterop = true, isSwiftPMImportCinterop = true)
        assertEquals(
            mapOf(
                "demo-iosSimulatorArm64Cinterop-swiftPMImportMain-1.0.klib" to expected,
                "demo-iosSimulatorArm64Cinterop-swiftPMImportMain.klib" to expected,
            ),
            classify(
                "demo-iosSimulatorArm64Cinterop-swiftPMImportMain-1.0.klib",
                "demo-iosSimulatorArm64Cinterop-swiftPMImportMain.klib",
            ),
        )
    }

    @Test
    fun `regular cinterop klibs are not swiftPMImport cinterops`() {
        val expected = Classification(isCinterop = true, isSwiftPMImportCinterop = false)
        assertEquals(
            mapOf(
                "demo-cinterop-sqlite" to expected,
                "demo-iosSimulatorArm64Cinterop-sqliteMain-1.0.klib" to expected,
                "demo-iosSimulatorArm64Cinterop-swiftPMImportExtraMain-1.0.klib" to expected,
            ),
            classify(
                "demo-cinterop-sqlite",
                "demo-iosSimulatorArm64Cinterop-sqliteMain-1.0.klib",
                "demo-iosSimulatorArm64Cinterop-swiftPMImportExtraMain-1.0.klib",
            ),
        )
    }

    @Test
    fun `plain klibs are not cinterops`() {
        val expected = Classification(isCinterop = false, isSwiftPMImportCinterop = false)
        assertEquals(
            mapOf(
                "demo" to expected,
                "demo-iosSimulatorArm64Main-1.0.klib" to expected,
            ),
            classify(
                "demo",
                "demo-iosSimulatorArm64Main-1.0.klib",
            ),
        )
    }
}

/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.blackboxtest

import com.intellij.testFramework.TestDataPath
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File

@Tag("swift-export")
@TestDataPath("\$PROJECT_ROOT")
class SwiftExportTest : AbstractNativeSwiftExportTest() {

    @Test
    fun trivial() {
        val testDir = File("native/native.tests/testData/SwiftExport/trivial")
        runIntegrationTest(testDir.absolutePath)
    }

    @Test
    fun primitiveTypeFunctions() {
        val testDir = File("native/native.tests/testData/SwiftExport/primitive_type_functions")
        runIntegrationTest(testDir.absolutePath)
    }

    @Test
    fun bridgedTypes() {
        val testDir = File("native/native.tests/testData/SwiftExport/bridged_types")
        runIntegrationTest(testDir.absolutePath)
    }

    @Test
    fun primitiveProperties() {
        val testDir = File("native/native.tests/testData/SwiftExport/primitive_properties")
        runIntegrationTest(testDir.absolutePath)
    }

    @Test
    fun namespacing() {
        val testDir = File("native/native.tests/testData/SwiftExport/namespacing")
        runIntegrationTest(testDir.absolutePath)
    }

    @Test
    fun classes() {
        val testDir = File("native/native.tests/testData/SwiftExport/classes")
        runIntegrationTest(testDir.absolutePath)
    }

    @Test
    fun extensions() {
        val testDir = File("native/native.tests/testData/SwiftExport/extensions")
        runTest(testDir.absolutePath)
    }
    
    @Test
    fun unknownSwiftType() {
        val testDir = File("native/native.tests/testData/SwiftExport/unknown_swift_type")
        runIntegrationTest(testDir.absolutePath)
    }

    @Test
    fun overloading() {
        val testDir = File("native/native.tests/testData/SwiftExport/overloading")
        runIntegrationTest(testDir.absolutePath)
    }

    @Test
    fun `kotlin sources should generate swift api`() {
        val testDir = File("native/native.tests/testData/SwiftExport/swift_api_generation")
        runAPIGenerationTest(testDir.absolutePath)
    }
}

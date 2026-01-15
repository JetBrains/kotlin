/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.native.interop.indexer.*
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Tests for SwiftBridge annotation extraction from API Notes YAML files.
 * API Notes are external YAML files that annotate C/Objective-C headers without modifying them.
 * They require module-based compilation with -fapinotes-modules flag.
 */
class ApiNotesSwiftBridgeTest : InteropTestsBase() {

    @BeforeEach
    fun onlyOnMac() {
        Assumptions.assumeTrue(HostManager.hostIsMac)
    }

    @Test
    fun `class with SwiftBridge from API Notes`() {
        val clazz = indexObjCClassWithApiNotes(
            headerContents = """
                @interface MyDataClass
                @end
            """.trimIndent(),
            apiNotesContents = """
                Name: TestModule
                Classes:
                - Name: MyDataClass
                  SwiftBridge: Swift.MySwiftData
            """.trimIndent(),
            className = "MyDataClass"
        )
        assertEquals("Swift.MySwiftData", clazz.swiftBridge)
    }

    @Test
    fun `class without SwiftBridge in API Notes has null swiftBridge`() {
        val clazz = indexObjCClassWithApiNotes(
            headerContents = """
                @interface PlainClass
                @end
            """.trimIndent(),
            apiNotesContents = """
                Name: TestModule
                Classes:
                - Name: PlainClass
            """.trimIndent(),
            className = "PlainClass"
        )
        assertNull(clazz.swiftBridge)
    }

    @Test
    fun `class with both SwiftName and SwiftBridge from API Notes`() {
        val clazz = indexObjCClassWithApiNotes(
            headerContents = """
                @interface OriginalName
                @end
            """.trimIndent(),
            apiNotesContents = """
                Name: TestModule
                Classes:
                - Name: OriginalName
                  SwiftName: RenamedClass
                  SwiftBridge: Swift.BridgedType
            """.trimIndent(),
            className = "OriginalName"
        )
        assertEquals("RenamedClass", clazz.swiftName)
        assertEquals("Swift.BridgedType", clazz.swiftBridge)
    }

    @Test
    fun `multiple classes with SwiftBridge from API Notes`() {
        val classes = indexObjCClassesWithApiNotes(
            headerContents = """
                @interface FirstClass
                @end

                @interface SecondClass
                @end
            """.trimIndent(),
            apiNotesContents = """
                Name: TestModule
                Classes:
                - Name: FirstClass
                  SwiftBridge: Swift.FirstBridge
                - Name: SecondClass
                  SwiftBridge: Swift.SecondBridge
            """.trimIndent()
        )
        val firstClass = classes.single { it.name == "FirstClass" }
        val secondClass = classes.single { it.name == "SecondClass" }

        assertEquals("Swift.FirstBridge", firstClass.swiftBridge)
        assertEquals("Swift.SecondBridge", secondClass.swiftBridge)
    }

    private fun indexObjCClassWithApiNotes(
        headerContents: String,
        apiNotesContents: String,
        className: String,
        moduleName: String = "TestModule"
    ): ObjCClass {
        return indexObjCClassesWithApiNotes(headerContents, apiNotesContents, moduleName)
            .single { it.name == className }
    }

    private fun indexObjCClassesWithApiNotes(
        headerContents: String,
        apiNotesContents: String,
        moduleName: String = "TestModule"
    ): Collection<ObjCClass> {
        val files = testFiles()

        // Create header file
        files.file("$moduleName.h", headerContents)

        // Create module map
        files.file("module.modulemap", """
            module $moduleName {
              header "$moduleName.h"
              export *
            }
        """.trimIndent())

        // Create API Notes file
        files.file("$moduleName.apinotes", apiNotesContents)

        // Create def file that uses modules with API Notes support
        // Note: -fmodules and -fapinotes-modules are required for API Notes to be loaded
        val defFile = files.file("test.def", """
            language = Objective-C
            modules = $moduleName
            compilerOpts = -fmodules -fapinotes-modules
        """.trimIndent())

        val library = buildNativeLibraryFrom(defFile, files.directory)
        val indexerResult = buildNativeIndex(library, verbose = false)

        return indexerResult.index.objCClasses
    }
}

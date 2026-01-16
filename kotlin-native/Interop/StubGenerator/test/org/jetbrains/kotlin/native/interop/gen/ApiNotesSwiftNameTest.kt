/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.native.interop.indexer.ObjCClass
import org.jetbrains.kotlin.native.interop.indexer.buildNativeIndex
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ApiNotesSwiftNameTest : InteropTestsBase() {

    @BeforeEach
    fun onlyOnMac() {
        Assumptions.assumeTrue(HostManager.hostIsMac)
    }

    @Test
    fun `class with SwiftName from API Notes`() {
        val clazz = indexObjCClassWithApiNotes(
            headerContents = """
                @interface OriginalClass
                @end
            """.trimIndent(),
            apiNotesContents = """
                Name: TestModule
                Classes:
                - Name: OriginalClass
                  SwiftName: RenamedClass
            """.trimIndent(),
            className = "OriginalClass"
        )
        assertEquals("RenamedClass", clazz.swiftName)
    }

    @Test
    fun `class without SwiftName in API Notes has null swiftName`() {
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
        assertNull(clazz.swiftName)
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

        files.file("$moduleName.h", headerContents)
        files.file("module.modulemap", """
            module $moduleName {
              header "$moduleName.h"
              export *
            }
        """.trimIndent())
        files.file("$moduleName.apinotes", apiNotesContents)

        val defFile = files.file("test.def", """
            language = Objective-C
            modules = $moduleName
            apinotes = true
             
        """.trimIndent())

        val library = buildNativeLibraryFrom(defFile, files.directory)
        val indexerResult = buildNativeIndex(library, verbose = false)

        return indexerResult.index.objCClasses
    }
}

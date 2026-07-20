/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.native.interop.indexer.NativeIndex
import org.jetbrains.kotlin.native.interop.indexer.ObjCClass
import org.jetbrains.kotlin.native.interop.indexer.ObjCContainer
import org.jetbrains.kotlin.native.interop.indexer.ObjCMethod
import org.jetbrains.kotlin.native.interop.indexer.ObjCProperty
import org.jetbrains.kotlin.native.interop.indexer.ObjCProtocol
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
    fun `class with SwiftName`() {
        val index = index(
                header = """
                    @interface OriginalClass
                    @end
                """,
                apiNotes = """
                    Name: TestModule
                    Classes:
                    - Name: OriginalClass
                      SwiftName: RenamedClass
                """
        )
        assertEquals("RenamedClass", index.objCClass("OriginalClass").swiftName)
    }

    @Test
    fun `class without SwiftName has null swiftName`() {
        val index = index(
                header = """
                    @interface PlainClass
                    @end
                """,
                apiNotes = """
                    Name: TestModule
                    Classes:
                    - Name: PlainClass
                """
        )
        assertNull(index.objCClass("PlainClass").swiftName)
    }

    @Test
    fun `protocol with SwiftName`() {
        val index = index(
                header = """
                    @protocol OriginalProtocol
                    @end
                """,
                apiNotes = """
                    Name: TestModule
                    Protocols:
                    - Name: OriginalProtocol
                      SwiftName: RenamedProtocol
                """
        )
        assertEquals("RenamedProtocol", index.objCProtocol("OriginalProtocol").swiftName)
    }

    @Test
    fun `instance method with SwiftName`() {
        val index = index(
                header = """
                    @interface C
                    - (void)doWith:(int)value;
                    @end
                """,
                apiNotes = """
                    Name: TestModule
                    Classes:
                    - Name: C
                      Methods:
                      - Selector: 'doWith:'
                        MethodKind: Instance
                        SwiftName: 'do(with:)'
                """
        )
        assertEquals("do(with:)", index.objCClass("C").instanceMethod("doWith:").swiftName)
    }

    @Test
    fun `class method with SwiftName`() {
        val index = index(
                header = """
                    @interface C
                    + (void)makeWith:(int)value;
                    @end
                """,
                apiNotes = """
                    Name: TestModule
                    Classes:
                    - Name: C
                      Methods:
                      - Selector: 'makeWith:'
                        MethodKind: Class
                        SwiftName: 'make(with:)'
                """
        )
        assertEquals("make(with:)", index.objCClass("C").classMethod("makeWith:").swiftName)
    }

    @Test
    fun `instance and class methods with the same selector are renamed independently`() {
        val index = index(
                header = """
                    @interface C
                    - (void)shared;
                    + (void)shared;
                    @end
                """,
                apiNotes = """
                    Name: TestModule
                    Classes:
                    - Name: C
                      Methods:
                      - Selector: 'shared'
                        MethodKind: Instance
                        SwiftName: 'sharedInstance()'
                      - Selector: 'shared'
                        MethodKind: Class
                        SwiftName: 'sharedClass()'
                """
        )
        val c = index.objCClass("C")
        assertEquals("sharedInstance()", c.instanceMethod("shared").swiftName)
        assertEquals("sharedClass()", c.classMethod("shared").swiftName)
    }

    @Test
    fun `method without SwiftName has null swiftName`() {
        val index = index(
                header = """
                    @interface C
                    - (void)untouched;
                    @end
                """,
                apiNotes = """
                    Name: TestModule
                    Classes:
                    - Name: C
                      Methods:
                      - Selector: 'untouched'
                        MethodKind: Instance
                        Availability: none
                """
        )
        assertNull(index.objCClass("C").instanceMethod("untouched").swiftName)
    }

    @Test
    fun `instance property with SwiftName`() {
        val index = index(
                header = """
                    @interface C
                    @property int originalValue;
                    @end
                """,
                apiNotes = """
                    Name: TestModule
                    Classes:
                    - Name: C
                      Properties:
                      - Name: originalValue
                        PropertyKind: Instance
                        SwiftName: renamedValue
                """
        )
        assertEquals("renamedValue", index.objCClass("C").instanceProperty("originalValue").swiftName)
    }

    @Test
    fun `class property with SwiftName`() {
        val index = index(
                header = """
                    @interface C
                    @property(class) int originalValue;
                    @end
                """,
                apiNotes = """
                    Name: TestModule
                    Classes:
                    - Name: C
                      Properties:
                      - Name: originalValue
                        PropertyKind: Class
                        SwiftName: renamedValue
                """
        )
        assertEquals("renamedValue", index.objCClass("C").classProperty("originalValue").swiftName)
    }

    @Test
    fun `property without PropertyKind renames both instance and class properties`() {
        val index = index(
                header = """
                    @interface C
                    @property int shared;
                    @property(class) int shared;
                    @end
                """,
                apiNotes = """
                    Name: TestModule
                    Classes:
                    - Name: C
                      Properties:
                      - Name: shared
                        SwiftName: renamedShared
                """
        )
        val c = index.objCClass("C")
        assertEquals("renamedShared", c.instanceProperty("shared").swiftName)
        assertEquals("renamedShared", c.classProperty("shared").swiftName)
    }

    @Test
    fun `protocol members with SwiftName`() {
        val index = index(
                header = """
                    @protocol P
                    - (void)doIt;
                    @property int value;
                    @end
                """,
                apiNotes = """
                    Name: TestModule
                    Protocols:
                    - Name: P
                      Methods:
                      - Selector: 'doIt'
                        MethodKind: Instance
                        SwiftName: 'doIt()'
                      Properties:
                      - Name: value
                        SwiftName: renamedValue
                """
        )
        val p = index.objCProtocol("P")
        assertEquals("doIt()", p.instanceMethod("doIt").swiftName)
        assertEquals("renamedValue", p.instanceProperty("value").swiftName)
    }

    @Test
    fun `API Notes SwiftName supersedes the in-header swift_name attribute`() {
        val index = index(
                header = """
                    __attribute__((swift_name("HeaderClass")))
                    @interface C
                    - (void)doIt __attribute__((swift_name("headerDoIt()")));
                    @end
                """,
                apiNotes = """
                    Name: TestModule
                    Classes:
                    - Name: C
                      SwiftName: NotesClass
                      Methods:
                      - Selector: 'doIt'
                        MethodKind: Instance
                        SwiftName: 'notesDoIt()'
                """
        )
        val c = index.objCClass("C")
        assertEquals("NotesClass", c.swiftName)
        assertEquals("notesDoIt()", c.instanceMethod("doIt").swiftName)
    }

    @Test
    fun `the in-header swift_name attribute is used when there is no API Notes entry`() {
        val index = index(
                header = """
                    __attribute__((swift_name("HeaderClass")))
                    @interface C
                    @end
                """,
                apiNotes = """
                    Name: TestModule
                """
        )
        assertEquals("HeaderClass", index.objCClass("C").swiftName)
    }

    @Test
    fun `API Notes are ignored when the feature is disabled`() {
        val index = index(
                header = """
                    __attribute__((swift_name("HeaderClass")))
                    @interface C
                    @end
                """,
                apiNotes = """
                    Name: TestModule
                    Classes:
                    - Name: C
                      SwiftName: NotesClass
                """,
                apiNotesEnabled = false
        )
        // Falls back to the in-header attribute; the API notes rename is not applied.
        assertEquals("HeaderClass", index.objCClass("C").swiftName)
    }

    @Test
    fun `versioned SwiftVersions section is ignored in favor of the top-level SwiftName`() {
        val index = index(
                header = """
                    @interface C
                    @end
                """,
                apiNotes = """
                    Name: TestModule
                    Classes:
                    - Name: C
                      SwiftName: CurrentName
                    SwiftVersions:
                    - Version: 3.0
                      Classes:
                      - Name: C
                        SwiftName: LegacyName
                """
        )
        assertEquals("CurrentName", index.objCClass("C").swiftName)
    }

    private fun NativeIndex.objCClass(name: String): ObjCClass =
            objCClasses.single { it.name == name }

    private fun NativeIndex.objCProtocol(name: String): ObjCProtocol =
            objCProtocols.single { it.name == name }

    private fun ObjCContainer.instanceMethod(selector: String): ObjCMethod =
            methods.single { it.selector == selector && !it.isClass }

    private fun ObjCContainer.classMethod(selector: String): ObjCMethod =
            methods.single { it.selector == selector && it.isClass }

    private fun ObjCContainer.instanceProperty(name: String): ObjCProperty =
            properties.single { it.name == name && !it.getter.isClass }

    private fun ObjCContainer.classProperty(name: String): ObjCProperty =
            properties.single { it.name == name && it.getter.isClass }

    private fun index(
            header: String,
            apiNotes: String,
            moduleName: String = "TestModule",
            apiNotesEnabled: Boolean = true,
    ): NativeIndex {
        val files = testFiles()

        files.file("$moduleName.h", header.trimIndent())
        files.file("module.modulemap", """
            module $moduleName {
              header "$moduleName.h"
              export *
            }
        """.trimIndent())
        files.file("$moduleName.apinotes", apiNotes.trimIndent())

        val defFile = files.file("test.def", """
            language = Objective-C
            modules = $moduleName
            apiNotesSwiftName = $apiNotesEnabled

        """.trimIndent())

        val library = buildNativeLibraryFrom(defFile, files.directory)
        return buildNativeIndex(library, verbose = false).index
    }
}

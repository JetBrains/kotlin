/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.indexer

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFails

// Note: this class contains only very basic tests.
class ModuleTests : IndexerTests() {

    @Test
    fun testSingleHeaderModule() {
        val files = TempFiles("testSingleHeaderModule")

        val header = files.file("Foo.h", "")

        files.file("module.modulemap", """
            module Foo {
              header "Foo.h"
            }
        """.trimIndent())

        val modulesInfo = getModulesInfo(compilationIncluding(files.directory), listOf("Foo"))
        assertEquals(setOf(header.absolutePath), modulesInfo.ownHeaders)
        assertEquals(listOf(header.absolutePath), modulesInfo.topLevelHeaders.canonicalize())
    }

    @Test
    fun testModuleWithTransitiveInclude() {
        val files = TempFiles("testModuleWithTransitiveInclude")

        val fooH = files.file("Foo.h", """
            #include "Bar.h"
        """.trimIndent())

        val barH = files.file("Bar.h", "")

        files.file("module.modulemap", """
            module Foo {
              header "Foo.h"
            }
        """.trimIndent())

        val modulesInfo = getModulesInfo(compilationIncluding(files.directory), listOf("Foo"))
        assertEquals(setOf(fooH.absolutePath, barH.absolutePath), modulesInfo.ownHeaders)
        assertEquals(listOf(fooH.absolutePath), modulesInfo.topLevelHeaders.canonicalize())
    }

    @Test
    fun testModuleImportingOtherModule() {
        val files = TempFiles("testModuleImportingOtherModule")

        val fooH = files.file("Foo.h", """
            #include "Bar.h"
        """.trimIndent())

        files.file("Bar.h", "")

        files.file("module.modulemap", """
            module Foo {
              header "Foo.h"
            }
            module Bar {
              header "Bar.h"
            }
        """.trimIndent())

        val modulesInfo = getModulesInfo(compilationIncluding(files.directory), listOf("Foo"))
        assertEquals(setOf(fooH.absolutePath), modulesInfo.ownHeaders)
        assertEquals(listOf(fooH.absolutePath), modulesInfo.topLevelHeaders.canonicalize())
    }

    @Test
    fun testFrameworkModule() {
        val files = TempFiles("testFramework")
        val fooH = files.file("Foo.framework/Headers/Foo.h", """
            #include "Bar.h"
            #include <Foo/Baz.h>
        """.trimIndent())

        val barH = files.file("Foo.framework/Headers/Bar.h", "")
        val bazH = files.file("Foo.framework/Headers/Baz.h", "")

        files.file("Foo.framework/Modules/module.modulemap", """
            framework module Foo {
              umbrella header "Foo.h"
            }
        """.trimIndent())

        val modulesInfo = getModulesInfo(compilation("-F${files.directory}"), listOf("Foo"))
        assertEquals(setOf(fooH.absolutePath, barH.absolutePath, bazH.absolutePath), modulesInfo.ownHeaders)
        assertEquals(listOf(fooH.absolutePath), modulesInfo.topLevelHeaders.canonicalize())
    }

    @Test
    fun testModuleImport() {
        val files = TempFiles("testModuleImport")
        val fooH = files.file("Foo.h", "@import Bar;")
        files.file("Bar.h", "")
        files.file("module.modulemap", """
            module Foo {
              header "Foo.h"
            }
            module Bar {
              header "Bar.h"
            }
        """.trimIndent())

        //without '-fmodules'
        val error = assertFails {
            getModulesInfo(compilation("-I${files.directory}"), listOf("Foo"))
        }
        assertContains(
                error.message.orEmpty(),
                """
                    use of '@import' when modules are disabled
                    header: '${fooH.absolutePath}'
                    module name: 'Bar'
                """.trimIndent()
        )

        //with '-fmodules'
        val modulesInfo = getModulesInfo(compilation("-I${files.directory}", "-fmodules"), listOf("Foo"))
        assertEquals(setOf(fooH.absolutePath), modulesInfo.ownHeaders)
        assertEquals(listOf(fooH.absolutePath), modulesInfo.topLevelHeaders.canonicalize())
    }

    @Test
    fun testMissingModule() {
        val files = TempFiles("testMissingModule")

        val compilation = compilationIncluding(files.directory)

        val error = assertFails {
            getModulesInfo(compilation, modules = listOf("Foo"))
        }

        assertContains(error.message.orEmpty(), "fatal error: module 'Foo' not found")
    }

    @Test
    fun testModuleWithMissingHeader() {
        val files = TempFiles("testModuleWithMissingHeader")

        files.file("module.modulemap", """
            module Foo {
              header "Foo.h"
            }
        """.trimIndent())

        val compilation = compilationIncluding(files.directory)

        val error = assertFails {
            getModulesInfo(compilation, modules = listOf("Foo"))
        }

        assertContains(error.message.orEmpty(), "error: header 'Foo.h' not found")
    }

    @Test
    fun testModuleWithBadCode() {
        val files = TempFiles("testModuleWithBadCode")

        files.file("Foo.h", """
            bad code;
        """.trimIndent())

        files.file("module.modulemap", """
            module Foo {
              header "Foo.h"
            }
        """.trimIndent())

        val compilation = compilationIncluding(files.directory)

        val error = assertFails {
            getModulesInfo(compilation, modules = listOf("Foo"))
        }

        assertContains(error.message.orEmpty(), "testModuleWithBadCode/Foo.h:1:1: error: unknown type name 'bad'")
    }

    private fun List<IncludeInfo>.canonicalize(): List<String> = this.map { File(it.headerPath).canonicalPath }

    private fun compilationIncluding(includeDirectory: File) = compilation("-I$includeDirectory")

    private fun compilation(vararg args: String) = CompilationImpl(
            includes = emptyList(),
            additionalPreambleLines = emptyList(),
            compilerArgs = listOf(*args),
            language = Language.OBJECTIVE_C
    )
}

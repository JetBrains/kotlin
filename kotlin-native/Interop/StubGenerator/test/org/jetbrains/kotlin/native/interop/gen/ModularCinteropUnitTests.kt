/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.native.interop.indexer.HeaderId
import org.jetbrains.kotlin.native.interop.indexer.Location
import org.jetbrains.kotlin.native.interop.indexer.NativeLibraryHeaderFilter
import org.jetbrains.kotlin.native.interop.indexer.ObjCIdType
import org.jetbrains.kotlin.native.interop.indexer.ObjCObjectPointer
import org.jetbrains.kotlin.native.interop.indexer.PointerType
import org.jetbrains.kotlin.native.interop.indexer.RecordType
import org.jetbrains.kotlin.native.interop.indexer.Typedef
import org.jetbrains.kotlin.native.interop.indexer.buildNativeIndex
import org.jetbrains.kotlin.native.interop.indexer.headerContentsHash
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.to

class ModularCinteropUnitTests : IndexerTestsBase() {

    @BeforeEach
    fun onlyOnObjCSupportedHost() {
        // These tests run with "modules", which currently requires ObjC, which currently only works on an Apple host
        Assumptions.assumeTrue(HostManager.hostIsMac)
    }

    @Test
    fun `cinterop modular import with -fmodule-map-file - sees modules`() {
        val files = testFiles()
        val markerFunction = "marker"

        files.file("foo.h", """
            void ${markerFunction}(void);
        """.trimIndent())
        val onlyExplicitModuleMap = files.file("foo.modulemap", """
            module foo {
              header "foo.h"
            }
        """.trimIndent())
        val def = files.file("foo.def", """
            language = Objective-C
            modules = foo
        """.trimIndent())

        assertEquals(
                markerFunction,
                buildNativeIndex(
                        buildNativeLibraryFrom(def, argsWithFmodules("-compiler-option", "-fmodule-map-file=${onlyExplicitModuleMap}")),
                        verbose = false,
                ).function.name,
        )
    }

    data class PartiallyImportableModulesCase(
            val importableHeaderOne: File,
            val importableHeaderTwo: File,
            val tempFiles: TempFiles,
    )

    private fun partiallyImportableSetup(): PartiallyImportableModulesCase {
        val files = testFiles()
        val one = files.file("one.h", """
            void one(void);
        """.trimIndent())
        val two = files.file("two.h", """
            void two(void);
        """.trimIndent())
        files.file("failure.h", """
            #include <iostream>
            using namespace std;
            void hello_cpp(void);
        """.trimIndent())
        files.file("module.modulemap", """
            module one { header "one.h" }
            module failure { header "failure.h" }
            module two { header "two.h" }
        """.trimIndent())
        return PartiallyImportableModulesCase(one, two, files)
    }

    @Test
    fun `skipNonImportableModules - imports available modules`() {
        val partiallyImportableSetup = partiallyImportableSetup()
        val skipNonImportableModulesDef = partiallyImportableSetup.tempFiles.file("skip_non_importable_modules.def", """
            language = Objective-C
            modules = one failure two
            skipNonImportableModules = true
        """.trimIndent())

        val library = buildNativeLibraryFrom(
                skipNonImportableModulesDef,
                argsWithFmodulesAndSearchPath(partiallyImportableSetup.tempFiles.directory)
        )
        val filter = assertIs<NativeLibraryHeaderFilter.Predefined>(library.headerFilter)
        assertEquals(
                setOf(partiallyImportableSetup.importableHeaderOne.path, partiallyImportableSetup.importableHeaderTwo.path),
                filter.headers,
        )
        assertEquals(
                listOf("one",
                        // FIXME: ???
                        "failure",
                        "two"),
                filter.modules,
        )

        assertEquals(
                setOf("one", "two"),
                buildNativeIndex(library, verbose = false).index.functions.map { it.name }.toSet(),
        )
    }

    @Test
    fun `skipNonImportableModules - is disabled by default - which leads to cinterop failure when some modules don't import`() {
        val partiallyImportableSetup = partiallyImportableSetup()
        val defaultBehavior = partiallyImportableSetup.tempFiles.file("default_behavior.def", """
            language = Objective-C
            modules = one failure two
        """.trimIndent())

        val importFailure = assertThrows<Error> {
            buildNativeLibraryFrom(
                    defaultBehavior,
                    argsWithFmodulesAndSearchPath(partiallyImportableSetup.tempFiles.directory)
            )
        }.toString()
        // FIXME: KT-84023 - We actually want to see the "'iostream' file not found", but it doesn't display right now
        assertContains(
                importFailure,
                "fatal error: could not build module 'failure'"
        )
    }

    @Test
    fun `skipNonImportableModules - emits failure - when all modules fail to import`() {
        val files = testFiles()
        files.file("failure_one.h", """
            #error "non-importable module failure_one"
        """.trimIndent())
        files.file("failure_two.h", """
            #error "non-importable module failure_two"
        """.trimIndent())
        files.file("module.modulemap", """
            module failure_one { header "failure_one.h" }
            module failure_two { header "failure_two.h" }
        """.trimIndent())

        val defFile = files.file("failure.def", """
           language = Objective-C
           modules = failure_one failure_two
           skipNonImportableModules = true
        """.trimIndent())

        val importFailure = assertThrows<Error> {
            buildNativeLibraryFrom(
                    defFile,
                    argsWithFmodulesAndSearchPath(files.directory)
            )
        }.toString()
        assertContains(importFailure, "error: \"non-importable module failure_one\"")
        assertContains(importFailure, "error: \"non-importable module failure_two\"")
    }

    @Test
    fun `skipNonImportableModules - no modules imported`() {
        val markerFunction = "marker"
        val files = testFiles()
        val def = files.file("no_modules.def", """
            language = Objective-C
            skipNonImportableModules = true
            ---
            void ${markerFunction}(void);
        """.trimIndent())
        assertEquals(
                markerFunction,
                buildNativeIndex(
                        buildNativeLibraryFrom(def, argsWithFmodulesAndSearchPath(files.directory)),
                        verbose = false,
                ).function.name,
        )
    }

    private fun argsWithFmodules(vararg arguments: String): Array<String> = arrayOf("-compiler-option", "-fmodules") + arguments
    private fun argsWithFmodulesAndSearchPath(searchPath: File) = argsWithFmodules("-compiler-option", "-I${searchPath}")

}
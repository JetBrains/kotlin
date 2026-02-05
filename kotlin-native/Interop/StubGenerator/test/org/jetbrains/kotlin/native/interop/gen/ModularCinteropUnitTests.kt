/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.native.interop.indexer.HeaderId
import org.jetbrains.kotlin.native.interop.indexer.Location
import org.jetbrains.kotlin.native.interop.indexer.NativeLibraryHeaderFilter
import org.jetbrains.kotlin.native.interop.indexer.ObjCObjectPointer
import org.jetbrains.kotlin.native.interop.indexer.Typedef
import org.jetbrains.kotlin.native.interop.indexer.buildNativeIndex
import org.jetbrains.kotlin.native.interop.indexer.headerContentsHash
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.to

class ModularCinteropUnitTests : IndexerTestsBase() {

    @BeforeEach
    fun onlyOnObjCSupportedHost() {
        // These tests run with "modules" that currently only support ObjC
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

    @Test
    fun `skipNonImportableModules - is disabled by default - which leads to cinterop failure when some modules don't import`() {
        val files = testFiles()
        val markerFunctionOne = "one"
        val markerFunctionTwo = "two"

        val one = files.file("one.h", """
            void ${markerFunctionOne}(void);
        """.trimIndent())
        val two = files.file("two.h", """
            void ${markerFunctionTwo}(void);
        """.trimIndent())
        files.file("failure.h", """
            #include <iostream>
            using namespace std;
            void hello_cpp(void) { cout << "Hello world"; }
        """.trimIndent())
        files.file("module.modulemap", """
            module one { header "one.h" }
            module failure { header "failure.h" }
            module two { header "two.h" }
        """.trimIndent())
        val defFileContent = """
            language = Objective-C
            modules = one failure two
            
        """.trimIndent()
        val skipNonImportableModulesDef = files.file("skip_non_importable_modules.def", defFileContent + """
            skipNonImportableModules = true
        """.trimIndent())
        val defaultBehavior = files.file("default_behavior.def", defFileContent)

        val library = buildNativeLibraryFrom(
                skipNonImportableModulesDef,
                argsWithFmodules("-compiler-option", "-I${files.directory}")
        )
        val filter = assertIs<NativeLibraryHeaderFilter.Predefined>(library.headerFilter)
        assertEquals(
                setOf(one.path, two.path),
                filter.headers,
        )
        assertEquals(
                listOf("one",
                        // FIXME: ???
                        "failure",
                        "two"),
                filter.modules,
        )

        // Also check
        assertEquals(
                setOf(markerFunctionOne, markerFunctionTwo),
                buildNativeIndex(library, verbose = false).index.functions.map { it.name }.toSet(),
        )

        val importFailure = assertThrows<Error> {
            buildNativeLibraryFrom(
                    defaultBehavior,
                    argsWithFmodules("-compiler-option", "-I${files.directory}")
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
                    argsWithFmodules("-compiler-option", "-I${files.directory}")
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
                        buildNativeLibraryFrom(def, argsWithFmodules("-compiler-option", "-I${files.directory}")),
                        verbose = false,
                ).function.name,
        )
    }

    @Test
    fun `forward protocol`

    // FIXME: Is this a sufficient test? The root cause was 2 typedefs, but the failure was in the StubIrDriver
    @Test
    fun `KT-81695 repeated typedefs with -fmodules - reference the same underlying typedef`() {
        val markerFunctionOne = "foo"
        val markerFunctionTwo = "bar"
        val files = testFiles()
        files.file("module.modulemap", """
            module one {
                header "one.h"
            }
            module two {
                header "two.h"
            }
        """.trimIndent())
        files.file("one.h", """
            typedef unsigned char char8_t;
            void ${markerFunctionOne}(char8_t);
        """.trimIndent())
        files.file("two.h", """
            typedef unsigned char char8_t;
            void ${markerFunctionTwo}(char8_t);
        """.trimIndent())
        val def = files.file("dup.def", """
            language = Objective-C
            modules = one two
        """.trimIndent())

        val index = buildNativeIndex(
                buildNativeLibraryFrom(def, argsWithFmodules("-compiler-option", "-I${files.directory}")),
                verbose = false
        ).index

        assertEquals(
                listOf("char8_t"),
                index.typedefs.map { it.name },
        )

        val typedef = index.typedefs.single()

        assertEquals(
                listOf(markerFunctionOne to typedef, markerFunctionTwo to typedef),
                index.functions.map { it.name to assertIs<Typedef>(it.parameters.single().type).def },
        )
    }

    @Test
    fun `KT-82766 external source symbol (or generated_declaration) - see the original type`() {
        val files = testFiles()
        files.file("module.modulemap", """
            module forward { header "forward.h" }
            module original { header "original.h" }
        """.trimIndent())
        files.file("forward.h", """
            @class Foo;
            void consume(Foo *);
        """.trimIndent())
        files.file("original.h", """
            # pragma clang attribute push(__attribute__((external_source_symbol(language="Swift", defined_in="original",generated_declaration))), apply_to=any(function,enum,objc_interface,objc_category,objc_protocol))

            @interface Foo
            -(void)bar;
            @end
            
            # pragma clang attribute pop
        """.trimIndent())
        val def = files.file("external_source_symbol.def", """
            language = Objective-C
            modules = original forward
        """.trimIndent())

        val index = buildNativeIndex(
                buildNativeLibraryFrom(def, argsWithFmodules("-compiler-option", "-I${files.directory}")),
                verbose = false
        ).index

        assertEquals(
                listOf("Foo" to false),
                index.objCClasses.map { it.name to it.isForwardDeclaration }
        )

        val objcClass = index.objCClasses.single()
        assertEquals(
                listOf("consume" to objcClass),
                index.functions.map { it.name to assertIs<ObjCObjectPointer>(it.parameters.single().type).def },
        )
    }

    @Test
    fun `KT-82377 forward before original - original still gets emitted`() {
        val files = testFiles()
        files.file("module.modulemap", """
            module forward { header "forward.h" }
            module original { header "original.h" }
        """.trimIndent())
        files.file("forward.h", """
            @class Foo;
            void consume(Foo *);
        """.trimIndent())
        files.file("original.h", """
            @interface Foo
            -(void)bar;
            @end
        """.trimIndent())
        val def = files.file("forward.def", """
            language = Objective-C
            modules = forward original
        """.trimIndent())

        val index = buildNativeIndex(
                buildNativeLibraryFrom(def, argsWithFmodules("-compiler-option", "-I${files.directory}")),
                verbose = false
        ).index

        assertEquals(
                listOf("Foo" to false),
                index.objCClasses.map { it.name to it.isForwardDeclaration }
        )

        val objcClass = index.objCClasses.single()
        assertEquals(
                listOf("consume" to objcClass),
                index.functions.map { it.name to assertIs<ObjCObjectPointer>(it.parameters.single().type).def },
        )
    }

    @Test
    fun `KT-82402 cinterop type reuse with -fmodules - uses the original type when it is visible`() {
        val files = testFiles()
        files.file("module.modulemap", """
            module forward { header "forward.h" }
            module original { header "original.h" }
        """.trimIndent())
        files.file("forward.h", """
            @class Foo;
            void consume(Foo *);
        """.trimIndent())
        val original = files.file("original.h", """
            @interface Foo
            -(void)bar;
            @end
        """.trimIndent())
        val def = files.file("forward.def", """
            language = Objective-C
            modules = forward original
        """.trimIndent())

        val definitionHeaderId = HeaderId(headerContentsHash(original.path))
        val index = buildNativeIndex(
                buildNativeLibraryFrom(
                        def,
                        argsWithFmodules("-compiler-option", "-I${files.directory}"),
                        imports = ImportsMock(
                                mapOf(
                                        definitionHeaderId to "original"
                                )
                        )
                ),
                verbose = false
        ).index

        assertEquals(
                emptyList(),
                index.objCClasses,
                message = "ObjC class should not be included as it would come from original"
        )

        data class ClassCheck(
                val name: String,
                val isForwardDeclaration: Boolean,
                val location: Location,
        )

        assertEquals(
                listOf("consume" to ClassCheck(
                        name = "Foo",
                        isForwardDeclaration = false,
                        location = Location(definitionHeaderId),
                )),
                index.functions.map {
                    it.name to assertIs<ObjCObjectPointer>(it.parameters.single().type).def.let {
                        ClassCheck(
                                it.name,
                                it.isForwardDeclaration,
                                it.location,
                        )
                    }},
        )
    }

    private fun argsWithFmodules(vararg arguments: String): Array<String> = arrayOf("-compiler-option", "-fmodules") + arguments

}
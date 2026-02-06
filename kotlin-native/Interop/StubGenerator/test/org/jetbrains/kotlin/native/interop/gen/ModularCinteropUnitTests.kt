/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.native.interop.indexer.EnumType
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
                        // We don't expect the "failure" module here, but having it here also should have no adverse side effects
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

    @Test
    fun `multimodular import - forward declaration to protocol in another module - is dereferenced to definition`() {
        val files = testFiles()
        files.file("module.modulemap", """
            module forward { header "forward.h" }
            module original { header "original.h" }
        """.trimIndent())
        files.file("forward.h", """
            @protocol Foo;
            void consume(id<Foo>);
        """.trimIndent())
        files.file("original.h", """
            @protocol Foo
            - (void)bar;
            @end
        """.trimIndent())
        val def = files.file("forward.def", """
            language = Objective-C
            modules = forward original
        """.trimIndent())

        val index = buildNativeIndex(
                buildNativeLibraryFrom(def, argsWithFmodulesAndSearchPath(files.directory)),
                verbose = false
        ).index

        data class TypeCheck(
                val name: String,
                val isForwardDeclaration: Boolean,
        )

        assertEquals(
                listOf(TypeCheck("Foo", false)),
                index.objCProtocols.map { TypeCheck(it.name, it.isForwardDeclaration) }
        )

        val protocol = index.objCProtocols.single()
        assertEquals(
                listOf("consume" to listOf(protocol)),
                index.functions.map { it.name to assertIs<ObjCIdType>(it.parameters.single().type).protocols },
        )
    }

    @Test
    fun `multimodular import - forward declaration to platform library type - is dereferenced to definition`() {
        val files = testFiles()
        files.file("module.modulemap", """
            module forward { header "forward.h" }
            module original { header "original.h" }
        """.trimIndent())
        files.file("forward.h", """
            @class NSObject;
            void consume(NSObject *);
        """.trimIndent())
        files.file("original.h", """
            #import <Foundation/Foundation.h>
        """.trimIndent())
        val def = files.file("forward.def", """
            language = Objective-C
            modules = forward original
        """.trimIndent())

        val index = buildNativeIndex(
                buildNativeLibraryFrom(def, argsWithFmodulesAndSearchPath(files.directory)),
                verbose = false
        ).index

        data class TypeCheck(
                val name: String,
                val isForwardDeclaration: Boolean,
        )

        assertEquals(
                listOf("consume" to TypeCheck("NSObject", false)),
                index.functions.map { it.name to assertIs<ObjCObjectPointer>(it.parameters.single().type).def.let { TypeCheck(it.name, it.isForwardDeclaration) } },
        )
    }

    @Test
    fun `multimodular import - forward declarations to structs, their typedefs and unions and - are dereferenced to definition`() {
        val files = testFiles()
        files.file("module.modulemap", """
            module forward { header "forward.h" }
            module original { header "original.h" }
        """.trimIndent())
        files.file("forward.h", """
            struct S;
            void consumeS(struct S *);
            
            struct F;
            typedef struct F T;
            void consumeT(T *);
            
            union U;
            void consumeU(union U *);
        """.trimIndent())
        files.file("original.h", """
            struct S {
                int a;
            };
            
            struct F {
                int b;
            };
            
            union U {
                int c;
                long d;
            };
        """.trimIndent())
        val def = files.file("forward.def", """
            language = Objective-C
            modules = forward original
        """.trimIndent())

        val index = buildNativeIndex(
                buildNativeLibraryFrom(def, argsWithFmodulesAndSearchPath(files.directory)),
                verbose = false
        ).index

        data class TypeCheck(
                val name: String,
                val isForwardDeclaration: Boolean,
        )

        assertEquals(
                listOf(TypeCheck("struct S", false), TypeCheck("struct F", false), TypeCheck("union U", false)),
                index.structs.map { TypeCheck(it.spelling, it.def == null) },
        )

        val structS = index.structs.single { it.spelling == "struct S" }
        val structF = index.structs.single { it.spelling == "struct F" }
        val unionU = index.structs.single { it.spelling == "union U" }

        assertEquals(
                listOf("consumeS", "consumeT", "consumeU"),
                index.functions.map { it.name },
        )

        assertEquals(
                listOf(structS),
                index.functions.single {
                    it.name == "consumeS"
                }.let {
                    it.parameters.map {
                        assertIs<RecordType>(assertIs<PointerType>(it.type).pointeeType).decl
                    }
                }
        )
        assertEquals(
                listOf(unionU),
                index.functions.single {
                    it.name == "consumeU"
                }.let {
                    it.parameters.map {
                        assertIs<RecordType>(assertIs<PointerType>(it.type).pointeeType).decl
                    }
                }
        )

        assertEquals(
                listOf(structF),
                index.functions.single {
                    it.name == "consumeT"
                }.let {
                    it.parameters.map {
                        assertIs<RecordType>(
                                assertIs<Typedef>(assertIs<PointerType>(it.type).pointeeType).def.aliased
                        ).decl
                    }
                }
        )
    }

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
        val typedefDecl = "typedef unsigned char char8_t;"
        files.file("one.h", """
            $typedefDecl
            void ${markerFunctionOne}(char8_t);
        """.trimIndent())
        files.file("two.h", """
            $typedefDecl
            void ${markerFunctionTwo}(char8_t);
        """.trimIndent())
        val def = files.file("dup.def", """
            language = Objective-C
            modules = one two
        """.trimIndent())

        val index = buildNativeIndex(
                buildNativeLibraryFrom(def, argsWithFmodulesAndSearchPath(files.directory)),
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
    fun `KT-81695 repeated NS_ENUM with -fmodules - reference the same underlying typedef`() {
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
        val typedefDecl = "typedef NS_ENUM(NSInteger, Foo) { One, Two };"
        files.file("one.h", """
            #import <Foundation/Foundation.h>
            $typedefDecl
            void ${markerFunctionOne}(Foo);
        """.trimIndent())
        files.file("two.h", """
            #import <Foundation/Foundation.h>
            $typedefDecl
            void ${markerFunctionTwo}(Foo);
        """.trimIndent())
        val def = files.file("dup.def", """
            language = Objective-C
            modules = one two
        """.trimIndent())

        val index = buildNativeIndex(
                buildNativeLibraryFrom(def, argsWithFmodulesAndSearchPath(files.directory)),
                verbose = false
        ).index

        assertEquals(
                listOf("enum Foo"),
                index.enums.map { it.spelling },
        )

        val enumDecl = index.enums.single()
        listOf(
                listOf("one" to enumDecl, "two" to enumDecl),
                index.functions.map {
                    it.name to assertIs<EnumType>(it.parameters.single().type).def
                }
        )
    }

    private class MultiModularImportCase(
            val def: File,
            val originalHeader: File,
            val forwardHeader: File,
            val tempFiles: TempFiles,
    )
    private fun multiModularImportBaseCase(originalHeaderContent: String): MultiModularImportCase {
        val files = testFiles()
        files.file("module.modulemap", """
            module forward { header "forward.h" }
            module original { header "original.h" }
        """.trimIndent())
        val forward = files.file("forward.h", """
            @class Foo;
            void consume(Foo *);
        """.trimIndent())
        val original = files.file("original.h", originalHeaderContent)
        val def = files.file("foo.def", """
            language = Objective-C
            modules = forward original
        """.trimIndent())
        return MultiModularImportCase(def, original, forward, files)
    }

    @Test
    fun `KT-82377 multimodular import - forward before original - original still gets emitted`() {
        val multiModularImportBaseCase = multiModularImportBaseCase("""
            @interface Foo
            -(void)bar;
            @end
            """.trimIndent()
        )

        val index = buildNativeIndex(
                buildNativeLibraryFrom(multiModularImportBaseCase.def, argsWithFmodulesAndSearchPath(multiModularImportBaseCase.tempFiles.directory)),
                verbose = false
        ).index

        data class TypeCheck(
                val name: String,
                val methods: List<String>,
                val isForwardDeclaration: Boolean,
        )

        assertEquals(
                listOf(TypeCheck("Foo", listOf("bar"), false)),
                index.objCClasses.map { TypeCheck(it.name, it.methods.map { it.kotlinName }, it.isForwardDeclaration) }
        )

        val objcClass = index.objCClasses.single()
        assertEquals(
                listOf("consume" to objcClass),
                index.functions.map { it.name to assertIs<ObjCObjectPointer>(it.parameters.single().type).def },
        )
    }

    @Test
    fun `KT-82766 multimodular import - forward declaration to external source symbol with generated_declaration flag - are dereferenced to definition`() {
        val multiModularImportBaseCase = multiModularImportBaseCase("""
            # pragma clang attribute push(__attribute__((external_source_symbol(language="Swift", defined_in="original",generated_declaration))), apply_to=any(function,enum,objc_interface,objc_category,objc_protocol))
            @interface Foo
            -(void)bar;
            @end
            # pragma clang attribute pop
            """.trimIndent()
        )

        val index = buildNativeIndex(
                buildNativeLibraryFrom(multiModularImportBaseCase.def, argsWithFmodulesAndSearchPath(multiModularImportBaseCase.tempFiles.directory)),
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
    fun `KT-82766 multimodular import - forward declaration to external source symbol with generated_declaration flag - with USR override`() {
        val multiModularImportBaseCase = multiModularImportBaseCase("""
            __attribute__((external_source_symbol(language="Swift", defined_in="sample",generated_declaration, USR="Baz")))
            @interface Foo
            -(void)bar;
            @end
            """.trimIndent()
        )

        val index = buildNativeIndex(
                buildNativeLibraryFrom(multiModularImportBaseCase.def, argsWithFmodulesAndSearchPath(multiModularImportBaseCase.tempFiles.directory)),
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
        val multiModularImportBaseCase = multiModularImportBaseCase("""
            @interface Foo
            -(void)bar;
            @end
            """.trimIndent()
        )

        val definitionHeaderId = HeaderId(headerContentsHash(multiModularImportBaseCase.originalHeader.path))
        val index = buildNativeIndex(
                buildNativeLibraryFrom(
                        multiModularImportBaseCase.def,
                        argsWithFmodulesAndSearchPath(multiModularImportBaseCase.tempFiles.directory),
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
                    }
                },
        )
    }

    private fun argsWithFmodules(vararg arguments: String): Array<String> = arrayOf("-compiler-option", "-fmodules") + arguments
    private fun argsWithFmodulesAndSearchPath(searchPath: File) = argsWithFmodules("-compiler-option", "-I${searchPath}")

}
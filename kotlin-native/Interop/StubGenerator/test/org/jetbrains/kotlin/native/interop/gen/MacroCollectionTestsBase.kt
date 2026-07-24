/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.native.interop.indexer.CharType
import org.jetbrains.kotlin.native.interop.indexer.ConstantDef
import org.jetbrains.kotlin.native.interop.indexer.FloatingConstantDef
import org.jetbrains.kotlin.native.interop.indexer.FloatingType
import org.jetbrains.kotlin.native.interop.indexer.IndexerResult
import org.jetbrains.kotlin.native.interop.indexer.IntegerConstantDef
import org.jetbrains.kotlin.native.interop.indexer.IntegerType
import org.jetbrains.kotlin.native.interop.indexer.MacroNamesCollectingMode
import org.jetbrains.kotlin.native.interop.indexer.PointerType
import org.jetbrains.kotlin.native.interop.indexer.StringConstantDef
import org.jetbrains.kotlin.native.interop.indexer.TypedConstantDef
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private inline fun <reified T : TypedConstantDef<V>, V> Collection<ConstantDef>.getByName(name: String): V? = this
        .filterIsInstance<T>()
        .singleOrNull { it.name == name }?.value

abstract class MacroCollectionTestsBase : InteropTestsBase() {
    protected abstract val macroNamesCollectingMode: MacroNamesCollectingMode
    protected abstract val isModular: Boolean

    @BeforeEach
    fun onlyOnObjCSupportedHost() {
        if (isModular) {
            // These tests run with "modules", which currently requires ObjC, which currently only works on an Apple host
            Assumptions.assumeTrue(HostManager.hostIsMac)
        }
    }

    private fun macroNames(index: org.jetbrains.kotlin.native.interop.indexer.NativeIndex): Set<String> =
            (index.macroConstants.asSequence() + index.wrappedMacros.asSequence())
                    .map { it.name }
                    .toSet()

    private fun index(
            files: List<Pair<String, String>>,
            defFileContents: String,
            additionalCompilerArgs: Array<String> = emptyArray(),
    ): IndexerResult {
        val testFiles = testFiles()
        files.forEach { [name, contents] ->
            testFiles.file(name, contents)
        }
        val defFile = testFiles.file("test.def", defFileContents)
        val searchPath = testFiles.directory
        val args = if (isModular) {
            arrayOf("-compiler-option", "-fmodules", "-compiler-option", "-I${searchPath}")
        } else {
            arrayOf("-compiler-option", "-I${searchPath}")
        }
        val library = buildNativeLibraryFrom(defFile, args + additionalCompilerArgs)

        return org.jetbrains.kotlin.native.interop.indexer.buildNativeIndex(
                library,
                verbose = false,
                macroNamesCollectingMode = macroNamesCollectingMode,
        )
    }

    private fun index(headerContents: String, additionalCompilerArgs: Array<String> = emptyArray()): IndexerResult {
        val headerName = "header.h"
        val files = buildList {
            add(headerName to headerContents)
            if (isModular) {
                add("module.modulemap" to """
                    module test_module {
                        header "$headerName"
                    }
                """.trimIndent())
            }
        }
        val defFileContents = if (isModular) {
            """
            language = Objective-C
            modules = test_module
            """.trimIndent()
        } else {
            """
            language = C
            headers = $headerName
            headerFilter = $headerName
            """.trimIndent()
        }
        return index(files, defFileContents, additionalCompilerArgs)
    }

    @Test
    fun `collects object-like macros`() {
        val index = index(
                """
                #define MACRO_CONST_INT 42
                #define MACRO_CONST_FLOAT 42.42
                #define MACRO_CONST_STR "foobar"
                #define MACRO_CONST_STR_CONCAT "foo" "bar"
                #define MACRO_CONST_EXPR_INT (40 + 2)
                #define MACRO_CONST_EXPR_FLOAT (42.0 + 0.42)
                #define MACRO_CONST_EXPR_STR ("foobar")
                #define MACRO_CONST_EXPR_STR_CONCAT ("foo" "bar")
                """.trimIndent()
        ).index
        val macros = index.macroConstants

        assertEquals(42, macros.getByName<IntegerConstantDef, _>("MACRO_CONST_INT"))
        assertEquals(42.42, assertNotNull(macros.getByName<FloatingConstantDef, _>("MACRO_CONST_FLOAT")), 1e-9)
        assertEquals("foobar", macros.getByName<StringConstantDef, _>("MACRO_CONST_STR"))
        assertEquals("foobar", macros.getByName<StringConstantDef, _>("MACRO_CONST_STR_CONCAT"))
        assertEquals(42, macros.getByName<IntegerConstantDef, _>("MACRO_CONST_EXPR_INT"))
        assertEquals(42.42, assertNotNull(macros.getByName<FloatingConstantDef, _>("MACRO_CONST_EXPR_FLOAT")), 1e-9)

        // KT-85610: This should work but currently doesn't. Update the expectation once we support that kind of macro.
        assertNull(macros.getByName<StringConstantDef, _>("MACRO_CONST_EXPR_STR"))
        assertNull(macros.getByName<StringConstantDef, _>("MACRO_CONST_EXPR_STR_CONCAT"))
    }

    @Test
    fun `skips function-like and empty macros`() {
        val index = index(
                """
                #define MACRO_FUNC(x) ((x) + 1)
                #define MACRO_NO_VALUE
                #define MACRO_TEMP 1
                #undef MACRO_TEMP
                """.trimIndent()
        ).index
        val names = (index.macroConstants.asSequence() + index.wrappedMacros.asSequence())
                .map { it.name }
                .toSet()
        assertFalse("MACRO_FUNC" in names)
        assertFalse("MACRO_NO_VALUE" in names)
        assertFalse("MACRO_TEMP" in names)
    }

    @Test
    fun `collects wrapped non-constant object-like macros`() {
        val index = index(
                """
                extern int extern_int;
                extern double extern_double;
                extern const char* extern_str;
                #define MACRO_WRAPPED_INT extern_int
                #define MACRO_WRAPPED_DOUBLE extern_double
                #define MACRO_WRAPPED_STR extern_str
                """.trimIndent()
        ).index
        val constNames = index.macroConstants
                .map { it.name }
                .toSet()
        assertFalse("MACRO_WRAPPED_INT" in constNames)
        assertFalse("MACRO_WRAPPED_DOUBLE" in constNames)
        assertFalse("MACRO_WRAPPED_STR" in constNames)
        assertTrue(index.wrappedMacros.any { it.name == "MACRO_WRAPPED_INT" && it.type is IntegerType })
        assertTrue(index.wrappedMacros.any { it.name == "MACRO_WRAPPED_DOUBLE" && it.type is FloatingType })
        assertTrue(index.wrappedMacros.any { it.name == "MACRO_WRAPPED_STR" && (it.type as? PointerType)?.pointeeType is CharType })
    }

    @Test
    fun `skips command-line macros`() {
        val index = index(
                headerContents = "#define MACRO_IN_FILE 1",
                additionalCompilerArgs = arrayOf("-compiler-option", "-DMACRO_FROM_COMMAND_LINE=42"),
        ).index

        val names = macroNames(index)
        assertTrue("MACRO_IN_FILE" in names)
        assertFalse("MACRO_FROM_COMMAND_LINE" in names)
    }

    @Test
    fun `filters out macros outside selected headers or modules`() {
        val commonFiles = listOf(
                "main.h" to """
                    #include "outside.h"
                    #define MACRO_INSIDE 1
                """.trimIndent(),
                "outside.h" to "#define MACRO_OUTSIDE 2"
        )
        val index = if (isModular) {
            index(
                    files = commonFiles + listOf(
                            "module.modulemap" to """
                                module test_module {
                                    header "main.h"
                                }
                                module another_module {
                                    header "outside.h"
                                }
                            """.trimIndent(),
                    ),
                    defFileContents = """
                        language = Objective-C
                        modules = test_module
                    """.trimIndent(),
            )
        } else {
            index(
                    files = commonFiles,
                    defFileContents = """
                        language = C
                        headers = main.h
                        headerFilter = main.h
                    """.trimIndent(),
            )
        }.index

        val names = macroNames(index)
        assertTrue("MACRO_INSIDE" in names)
        assertFalse("MACRO_OUTSIDE" in names)
    }

    @Test
    fun `collects macros from multiple selected headers or modules`() {
        val commonFiles = listOf(
                "first.h" to "#define MACRO_FIRST 1",
                "second.h" to "#define MACRO_SECOND 2",
        )
        val index = if (isModular) {
            index(
                    files = commonFiles + listOf(
                            "module.modulemap" to """
                                module first {
                                    header "first.h"
                                }
                                module second {
                                    header "second.h"
                                }
                            """.trimIndent(),
                    ),
                    defFileContents = """
                        language = Objective-C
                        modules = first second
                    """.trimIndent(),
            )
        } else {
            index(
                    files = commonFiles,
                    defFileContents = """
                        language = C
                        headers = first.h second.h
                        headerFilter = first.h second.h
                    """.trimIndent(),
            )
        }.index

        val names = macroNames(index)
        assertTrue("MACRO_FIRST" in names)
        assertTrue("MACRO_SECOND" in names)
    }
}

class NonModularLegacyMacroCollectionTests : MacroCollectionTestsBase() {
    override val macroNamesCollectingMode = MacroNamesCollectingMode.LEGACY
    override val isModular = false
}

class NonModularLibclangextMacroCollectionTests : MacroCollectionTestsBase() {
    override val macroNamesCollectingMode = MacroNamesCollectingMode.LIBCLANGEXT
    override val isModular = false
}

class NonModularLibclangextParallelMacroCollectionTests : MacroCollectionTestsBase() {
    override val macroNamesCollectingMode = MacroNamesCollectingMode.LIBCLANGEXT_PARALLEL
    override val isModular = false
}

class ModularLegacyMacroCollectionTests : MacroCollectionTestsBase() {
    override val macroNamesCollectingMode = MacroNamesCollectingMode.LEGACY
    override val isModular = true
}

class ModularLibclangextMacroCollectionTests : MacroCollectionTestsBase() {
    override val macroNamesCollectingMode = MacroNamesCollectingMode.LIBCLANGEXT
    override val isModular = true
}

class ModularLibclangextParallelMacroCollectionTests : MacroCollectionTestsBase() {
    override val macroNamesCollectingMode = MacroNamesCollectingMode.LIBCLANGEXT_PARALLEL
    override val isModular = true
}

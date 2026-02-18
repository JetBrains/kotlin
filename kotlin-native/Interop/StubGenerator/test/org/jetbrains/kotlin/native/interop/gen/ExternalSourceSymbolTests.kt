/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.native.interop.indexer.headerContentsHash
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals

class ExternalSourceSymbolTests : IndexerTestsBase() {

    @BeforeEach
    fun onlyOnObjCSupportedHost() {
        Assumptions.assumeTrue(HostManager.hostIsMac)
    }

    private fun interfaceDeclaration(attribute: String): String = """
        ${attribute}
        @interface Foo
        -(void)bar;
        @end
    """.trimIndent()

    // Check that we enable CXIndexOpt_IndexGeneratedDeclarations in the main indexing iteration and don't need visitChildren
    @Test
    fun `standalone generated declaration - is indexed - without visitChildren pass`() {
        val files = testFiles()
        files.file("generated.h", interfaceDeclaration("""
            __attribute__((external_source_symbol(language="Swift", defined_in="sample",generated_declaration)))
        """.trimIndent()))
        val def = files.file("generated.def", """
            language = Objective-C
            headers = generated.h
        """.trimIndent())
        val index = org.jetbrains.kotlin.native.interop.indexer.buildNativeIndex(
                buildNativeLibraryFrom(def, arrayOf("-compiler-option", "-I${files.directory}")),
                verbose = false,
        ).index

        assertEquals(
                listOf("Foo" to listOf("bar")),
                index.objCClasses.map { it.name to it.methods.map { it.kotlinName } },
        )
    }

    @Test
    fun `standalone non-generated declaration - is indexed - without visitChildren pass`() {
        val files = testFiles()
        files.file("generated.h", interfaceDeclaration("""
            __attribute__((external_source_symbol(language="Swift", defined_in="sample")))
        """.trimIndent()))
        val def = files.file("generated.def", """
            language = Objective-C
            headers = generated.h
        """.trimIndent())
        val index = org.jetbrains.kotlin.native.interop.indexer.buildNativeIndex(
                buildNativeLibraryFrom(def, arrayOf("-compiler-option", "-I${files.directory}")),
                verbose = false,
        ).index

        assertEquals(
                listOf("Foo" to listOf("bar")),
                index.objCClasses.map { it.name to it.methods.map { it.kotlinName } },
        )
    }

    @Test
    fun `standalone generated declaration with non-Swift language - is indexed - without visitChildren pass`() {
        val files = testFiles()
        files.file("generated.h", interfaceDeclaration("""
            __attribute__((external_source_symbol(language="Kwift", defined_in="sample",generated_declaration)))
        """.trimIndent()))
        val def = files.file("generated.def", """
            language = Objective-C
            headers = generated.h
        """.trimIndent())
        val index = org.jetbrains.kotlin.native.interop.indexer.buildNativeIndex(
                buildNativeLibraryFrom(def, arrayOf("-compiler-option", "-I${files.directory}")),
                verbose = false,
        ).index

        assertEquals(
                listOf("Foo" to listOf("bar")),
                index.objCClasses.map { it.name to it.methods.map { it.kotlinName } },
        )
    }

    @Test
    fun `KT49455 category marked generated_declaration - is indexed - without visitChildren pass`() {
        val files = testFiles()
        val header = files.file("KT49455.h", """
            #import <Foundation/NSObject.h>
            
            @interface KT49455 : NSObject
            @end

            __attribute__((external_source_symbol(language="Swift", defined_in="sample",generated_declaration)))
            @interface KT49455 (KT49455Ext)
            - (int)extensionFunction;
            @end

            __attribute__((unavailable("unavailableExtensionFunction is unavailable")))
            @interface KT49455 (KT49455UnavailableExt)
            - (int)unavailableExtensionFunction;
            @end
        """.trimIndent())
        val def = files.file("generated.def", """
            language = Objective-C
            headers = KT49455.h
        """.trimIndent())

        val index = org.jetbrains.kotlin.native.interop.indexer.buildNativeIndex(
                buildNativeLibraryFrom(def, arrayOf("-compiler-option", "-I${files.directory}")),
                verbose = false,
        ).index

        assertEquals(
                listOf("KT49455Ext" to listOf("extensionFunction")),
                index.objCCategories.filter {
                    it.location.headerId.value == headerContentsHash(header.path)
                }.map { it.name to it.methods.map { it.kotlinName } }
        )
    }

}
/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.native.interop.indexer.HeaderId
import org.jetbrains.kotlin.native.interop.indexer.buildNativeIndex
import org.jetbrains.kotlin.native.interop.indexer.headerContentsHash
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

class IncludedHeadersTests : IndexerTestsBase() {

    private fun createHeaders(files: TempFiles): Pair<File, File> {
        val fooH = files.file("foo.h", """
            #include "bar.h"
            void foo(void);
        """.trimIndent())

        val barH = files.file("bar.h", """
            void bar(void);
        """.trimIndent())

        files.file("module.modulemap", """
            module Foo {
              header "foo.h"
              export *
            }

            module Bar {
              header "bar.h"
              export *
            }
        """.trimIndent())

        return fooH to barH
    }

    private fun IncludedHeadersTests.checkIncludedHeaders(
            defFile: File,
            expected: Set<HeaderId>
    ) {
        val library = buildNativeLibraryFrom(defFile, headersDirectory = defFile.parentFile)
        val index = buildNativeIndex(library, verbose = false).index
        val actual = index.includedHeaders.toSet()

        // TODO KT-83814: includedHeaders contains the "main file".
        // It is tricky to predict the `HeaderId` of it, so here we just expect a single extra value.
        assertTrue(
                (expected - actual).isEmpty() && (actual - expected).size == 1,
                "expected: <$expected + something> but was: <$actual>"
        )
    }

    private fun headerId(header: File) = HeaderId(headerContentsHash(header.absolutePath))

    @Test
    fun `test with headers`() {
        val files = testFiles()

        val (fooH, barH) = createHeaders(files)

        val defFile = files.file("test.def", """
            headers = foo.h
        """.trimIndent())

        val expected = setOf(
                headerId(fooH),
                headerId(barH),
                HeaderId("builtins")
        )
        checkIncludedHeaders(defFile, expected)
    }

    @Test
    fun `test with headers and headerFilter`() {
        val files = testFiles()

        val (fooH, _) = createHeaders(files)

        val defFile = files.file("test.def", """
            headers = foo.h
            headerFilter = foo.h
        """.trimIndent())

        checkIncludedHeaders(defFile, setOf(headerId(fooH)))
    }

    @Test
    fun `test with modules`() {
        assumeTrue(HostManager.hostIsMac)

        val files = testFiles()

        val (fooH, _) = createHeaders(files)

        val defFile = files.file("test.def", """
            language = Objective-C
            modules = Foo
        """.trimIndent())

        checkIncludedHeaders(defFile, setOf(headerId(fooH)))
    }

    @Test
    fun `test with modules and fmodules`() {
        assumeTrue(HostManager.hostIsMac)

        val files = testFiles()

        val (fooH, _) = createHeaders(files)

        val defFile = files.file("test.def", """
            language = Objective-C
            modules = Foo
            compilerOpts = -fmodules
            """.trimIndent()
        )

        checkIncludedHeaders(defFile, setOf(headerId(fooH)))
    }
}
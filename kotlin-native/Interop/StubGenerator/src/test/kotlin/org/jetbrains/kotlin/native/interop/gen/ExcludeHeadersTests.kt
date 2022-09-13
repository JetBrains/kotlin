/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.getHeaderPaths
import kotlin.test.*

class ExcludeHeadersTests : InteropTestsBase() {
    @Test
    fun `excludeHeaders smoke 0`() {
        val files = TempFiles("excludeHeadersSmoke0")
        val header1 = files.file("header1.h", "")
        val header2 = files.file("header2.h", "")
        val header3 = files.file("header3.h", "")
        val defFile = files.file("excludeSmoke.def", """
            headers = header1.h header2.h header3.h
            headerFilter = **
            excludeFilter = header3.h
        """.trimIndent())
        val library = buildNativeLibraryFrom(defFile, files.directory)
        val headers = library.getHeaderPaths().ownHeaders
        assertContains(headers, header1.absolutePath)
        assertContains(headers, header2.absolutePath)
        assertFalse(header3.absolutePath in headers)
    }

    @Test
    fun `excludeHeaders smoke 1`() {
        val files = TempFiles("excludeHeadersSmoke1")
        val header1 = files.file("header1.h", "")
        val header2 = files.file("header2.h", "")
        val header3 = files.file("header3.h", "")
        val defFile = files.file("excludeSmoke.def", """
            headers = header1.h header2.h header3.h
            headerFilter = **
            excludeFilter = header[2-3].h
        """.trimIndent())
        val library = buildNativeLibraryFrom(defFile, files.directory)
        val headers = library.getHeaderPaths().ownHeaders
        assertContains(headers, header1.absolutePath)
        assertFalse(header2.absolutePath in headers)
        assertFalse(header3.absolutePath in headers)
    }

    @Test
    fun `excludeHeaders empty`() {
        val files = TempFiles("excludeHeadersEmpty")
        val header1 = files.file("header1.h", "")
        val defFile = files.file("excludeSmoke.def", """
            headers = header1.h
            headerFilter = **
            excludeFilter = 
        """.trimIndent())
        val library = buildNativeLibraryFrom(defFile, files.directory)
        val headers = library.getHeaderPaths().ownHeaders
        assertContains(headers, header1.absolutePath)
    }

    @Test
    fun `excludeFilter has higher priority than headerFilter`() {
        val files = TempFiles("excludeFilterHasHigherPriority")
        val header1 = files.file("header1.h", "")
        val defFile = files.file("excludeSmoke.def", """
            headers = header1.h
            headerFilter = header1.h
            excludeFilter = header1.h
        """.trimIndent())
        val library = buildNativeLibraryFrom(defFile, files.directory)
        val headers = library.getHeaderPaths().ownHeaders
        assertFalse(header1.absolutePath in headers)
    }
}
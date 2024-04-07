/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.StructDef
import org.jetbrains.kotlin.native.interop.indexer.buildNativeIndex
import kotlin.test.Test
import kotlin.test.assertEquals

class StructRenderingTests : InteropTestsBase() {
    // See https://youtrack.jetbrains.com/issue/KT-55030#focus=Comments-27-6742454.0-0
    @Test
    fun kt55030() {
        val files = TempFiles("kt55030")
        val header = files.file("header.h", """
            union SupportedValue {
                struct {
                    int a;
                } b;
                int c;
            };

            union UnsupportedValue {
                struct {
                    int a : 5;
                } b;
                int c;
            };
        """.trimIndent())
        val defFile = files.file("kt55030.def", """
            headers = ${header.name}
        """.trimIndent())


        val library = buildNativeLibraryFrom(defFile, files.directory)
        val index = buildNativeIndex(library, verbose = false).index

        fun getUnionDef(name: String): StructDef {
            return index.structs.find { it.spelling == "union $name" }!!.def!!
        }

        assertEquals(
                null,
                tryRenderStructOrUnion(getUnionDef("UnsupportedValue"))
        )

        assertEquals(
                "union { struct  { int a; } b; int c; }",
                tryRenderStructOrUnion(getUnionDef("SupportedValue"))
        )
    }
}

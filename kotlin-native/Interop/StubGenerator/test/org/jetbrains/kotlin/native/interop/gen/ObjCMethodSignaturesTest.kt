/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.native.interop.indexer.buildNativeIndex
import org.junit.Assume
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ObjCMethodSignaturesTest : InteropTestsBase() {
    companion object {
        @BeforeClass
        @JvmStatic
        fun assumeMacOS() {
            Assume.assumeTrue(HostManager.hostIsMac)
        }
    }

    @Test
    fun `ObjC Direct`() {
        val files = TempFiles("ObjCDirect")
        files.file("header.h", """
            #import <Foundation/Foundation.h>
            
            @interface Foo : NSObject
            + (void)direct __attribute__((objc_direct));
            - (void)direct __attribute__((objc_direct));
            @end
            
            @interface Foo(Ext)
            + (void)directExt __attribute__((objc_direct));
            - (void)directExt __attribute__((objc_direct));
            @end
        """.trimIndent())
        val defFile = files.file("direct.def", """
            language = Objective-C
            headers = header.h
        """.trimIndent())
        val library = buildNativeLibraryFrom(defFile, files.directory)
        val index = buildNativeIndex(library, false).index

        index.objCClasses.find { it.name == "Foo" }.let { cls ->
            assertNotNull(cls, "Class 'Foo' not found in native library $library")
            assertNotNull(cls.methods.find { it.selector == "direct" && it.isClass && it.isDirect })
            assertNotNull(cls.methods.find { it.selector == "direct" && !it.isClass && it.isDirect })
        }

        index.objCCategories.find { it.name == "Ext" && it.clazz.name == "Foo" }.let { cat ->
            assertNotNull(cat, "Category 'Foo(Ext)' not found in native library $library")
            assertNotNull(cat.methods.find { it.selector == "directExt" && it.isClass && it.isDirect })
            assertNotNull(cat.methods.find { it.selector == "directExt" && !it.isClass && it.isDirect })
        }
    }
}
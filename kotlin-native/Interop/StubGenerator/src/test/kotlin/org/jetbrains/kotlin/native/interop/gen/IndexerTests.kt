/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.native.interop.indexer.buildNativeIndex
import kotlin.test.*

class IndexerTests : InteropTestsBase() {

    @Test
    fun smoke() {
        val files = TempFiles("indexerSmoke0")
        files.file("header1.h", """
            #ifndef _HEADER1_
            #define _HEADER1_
           
            @interface MyClass
            - (void) action;
            @end
            
            @interface SkipClass
            @end
            
            @interface SkipClass(Test)
            - (void)lastNameFirstNameString;
            @end
            
            @interface MyClass(Test)
            - (void)lastNameFirstNameString;
            @end
            
            #endif
        """.trimIndent())

        files.file("header2.h", """
            #ifndef _HEADER2_
            #define _HEADER2_
            
            #include "header1.h"
            
            @interface MyClass(SkipCategory)
            - (void)skipMethod;
            @end
            
            #endif
        """.trimIndent())
        val defFile = files.file("indexerSmoke0.def", """
            language = Objective-C
            headers = header1.h header2.h
            objcClassesWithCategories = MyClass
        """.trimIndent())
        val library = buildNativeLibraryFrom(defFile, files.directory)
        val index = buildNativeIndex(library, verbose = true)
        assertContains(index.index.objCClasses.map { it.name }, "MyClass")
        val myClass = index.index.objCClasses.first { it.name == "MyClass" }
        val myClassCategories = myClass.includedCategories.map { it.name }
        assertContains(myClassCategories, "Test")
        assertFalse("SkipCategory" in myClassCategories)
    }
}
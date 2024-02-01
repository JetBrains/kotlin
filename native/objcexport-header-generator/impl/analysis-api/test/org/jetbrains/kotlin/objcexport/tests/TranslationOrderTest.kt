/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.objcexport.StableFileOrder
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class TranslationOrderTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {

    @Test
    fun `test - StableFileOrder - sort by packageName`() {
        val files = inlineSourceCodeAnalysis.createKtFiles {
            sourceFile("A.kt", "package com.a")
            sourceFile("B.kt", "package com.b")
            sourceFile("C.kt", "package com.c")
        }

        val unsorted = listOf(files.getValue("C.kt"), files.getValue("B.kt"), files.getValue("A.kt"))
        val sorted = listOf(files.getValue("A.kt"), files.getValue("B.kt"), files.getValue("C.kt"))

        assertNotEquals(sorted, unsorted)
        assertEquals(sorted, unsorted.sortedWith(StableFileOrder))
    }

    @Test
    fun `test - StableFileOrder - sort by packageName then FileName`() {
        val files = inlineSourceCodeAnalysis.createKtFiles {
            sourceFile("A.kt", "package com.a")
            sourceFile("B1.kt", "package com.b")
            sourceFile("B2.kt", "package com.b")
        }

        val unsorted = listOf(files.getValue("B2.kt"), files.getValue("A.kt"), files.getValue("B1.kt"))
        val sorted = listOf(files.getValue("A.kt"), files.getValue("B1.kt"), files.getValue("B2.kt"))

        assertNotEquals(sorted, unsorted)
        assertEquals(sorted, unsorted.sortedWith(StableFileOrder))
    }
}
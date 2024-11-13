/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.objcexport.isAnnotatedAsExternalObjCClass
import org.jetbrains.kotlin.objcexport.isObjCClass
import org.jetbrains.kotlin.objcexport.isSuperTypeMapped
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UnavailableObjCClassifierTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - isSuperTypeMapped`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            interface Foo
            interface ListType<T>: List<T> 
            interface ListTypeExt<T>: ListType<T> 
        """.trimIndent()
        )

        analyze(file) {
            assertFalse(isSuperTypeMapped(getClassOrFail(file, "Foo")))
            assertTrue(isSuperTypeMapped(getClassOrFail(file, "ListType")))
            assertTrue(isSuperTypeMapped(getClassOrFail(file, "ListTypeExt")))
        }
    }

    @Test
    fun `test - isObjCClass`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            import kotlinx.cinterop.ObjCObject
            interface Foo : ObjCObject
            interface Bar : List<String>
        """.trimIndent()
        )

        analyze(file) {
            assertTrue(isObjCClass(getClassOrFail(file, "Foo")))
            assertFalse(isObjCClass(getClassOrFail(file, "Bar")))
        }
    }

    @Test
    fun `test - isAnnotatedAsExternalObjCClass`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            import kotlinx.cinterop.ExternalObjCClass
            @ExternalObjCClass
            interface FooAnnotated
            interface Foo
        """.trimIndent()
        )

        analyze(file) {
            assertTrue(isAnnotatedAsExternalObjCClass(getClassOrFail(file, "FooAnnotated")))
            assertFalse(isAnnotatedAsExternalObjCClass(getClassOrFail(file, "Foo")))
        }
    }
}
/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.DefaultTypeClassIds
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getInlineTargetTypeOrNull
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.jetbrains.kotlin.objcexport.testUtils.getPropertyOrFail
import org.junit.jupiter.api.Test
import kotlin.test.*

class GetInlineTargetTypeOrNullTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - no inlined class`() {
        val file = inlineSourceCodeAnalysis.createKtFile("class Foo")
        analyze(file) {
            val foo = file.getClassOrFail("Foo")
            assertNull(foo.getInlineTargetTypeOrNull())
        }
    }

    @Test
    fun `test - simple inline class`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            value class Foo(val value: Int)
        """.trimIndent()
        )

        analyze(file) {
            val foo = file.getClassOrFail("Foo")
            val inlineTargetType = assertNotNull(foo.getInlineTargetTypeOrNull())
            assertEquals(DefaultTypeClassIds.INT, inlineTargetType.classIdOrFail())
        }
    }

    @Test
    fun `test - transitive inline class`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            value class A(val value: Int)
            value class B(val value: A)
            
            val foo: B get() = error("stub")
        """.trimIndent()
        )

        analyze(file) {
            val foo = file.getPropertyOrFail("foo")
            assertEquals(DefaultTypeClassIds.INT, foo.returnType.getInlineTargetTypeOrNull().classIdOrFail())
        }
    }

    @Test
    fun `test - transitive inline class - with nullability`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
            value class A(val value: Int)
            value class B(val value: A?)
            
            val foo: B get() = error("stub")
        """.trimIndent()
        )

        analyze(file) {
            val foo = file.getPropertyOrFail("foo")
            assertEquals(DefaultTypeClassIds.INT, foo.returnType.getInlineTargetTypeOrNull().classIdOrFail())
            assertTrue(foo.returnType.getInlineTargetTypeOrNull()?.isMarkedNullable ?: false)
        }
    }

    private fun KtType?.classIdOrFail(): ClassId {
        if (this == null) error("Type was null")
        if (this !is KtNonErrorClassType) fail("Unexpected error type: '$this'")
        return classId
    }
}
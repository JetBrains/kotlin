/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.DefaultTypeClassIds
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
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
            val foo = getClassOrFail(file, "Foo")
            assertNull(getInlineTargetTypeOrNull(foo))
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
            val foo = getClassOrFail(file, "Foo")
            val inlineTargetType = assertNotNull(getInlineTargetTypeOrNull(foo))
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
            val foo = getPropertyOrFail(file, "foo")
            assertEquals(DefaultTypeClassIds.INT, getInlineTargetTypeOrNull(foo.returnType).classIdOrFail())
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
            val foo = getPropertyOrFail(file, "foo")
            assertEquals(DefaultTypeClassIds.INT, getInlineTargetTypeOrNull(foo.returnType).classIdOrFail())
            assertTrue(getInlineTargetTypeOrNull(foo.returnType)?.isMarkedNullable ?: false)
        }
    }

    private fun KaType?.classIdOrFail(): ClassId {
        if (this == null) error("Type was null")
        if (this !is KaClassType) fail("Unexpected error type: '$this'")
        return classId
    }
}
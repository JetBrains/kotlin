/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.session.useSiteSession
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaStandardTypeClassIds
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.isMarkedNullable
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.export.test.getClassOrFail
import org.jetbrains.kotlin.export.test.getPropertyOrFail
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.objcexport.analysisApiUtils.getInlineTargetTypeOrNull
import org.junit.jupiter.api.Test
import kotlin.test.*

class GetInlineTargetTypeOrNullTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - no inlined class`() {
        val file = inlineSourceCodeAnalysis.createKtFile("class Foo")
        analyze(file) {
            val session = useSiteSession
            val foo = session.getClassOrFail(file, "Foo")
            assertNull(session.getInlineTargetTypeOrNull(foo))
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
            val session = useSiteSession
            val foo = session.getClassOrFail(file, "Foo")
            val inlineTargetType = assertNotNull(session.getInlineTargetTypeOrNull(foo))
            assertEquals(KaStandardTypeClassIds.INT, inlineTargetType.classIdOrFail())
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
            val session = useSiteSession
            val foo = session.getPropertyOrFail(file, "foo")
            assertEquals(KaStandardTypeClassIds.INT, session.getInlineTargetTypeOrNull(foo.returnType).classIdOrFail())
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
            val session = useSiteSession
            val foo = session.getPropertyOrFail(file, "foo")
            assertEquals(KaStandardTypeClassIds.INT, session.getInlineTargetTypeOrNull(foo.returnType).classIdOrFail())
            assertTrue(session.getInlineTargetTypeOrNull(foo.returnType)?.isMarkedNullable ?: false)
        }
    }

    private fun KaType?.classIdOrFail(): ClassId {
        if (this == null) error("Type was null")
        if (this !is KaClassType) fail("Unexpected error type: '$this'")
        return classId
    }
}

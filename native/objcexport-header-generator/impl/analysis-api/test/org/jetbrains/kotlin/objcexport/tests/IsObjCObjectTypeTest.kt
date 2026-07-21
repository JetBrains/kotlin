/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.scopes.memberScope
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.session.useSiteSession
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.export.test.getClassOrFail
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isObjCObjectType
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsObjCObjectTypeTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - simple class`() {
        val file = inlineSourceCodeAnalysis.createKtFile("class Foo")
        analyze(file) {
            val session = useSiteSession
            val fooSymbol = session.getClassOrFail(file, "Foo")
            assertFalse(session.isObjCObjectType(fooSymbol.memberScope.constructors.single().returnType))
        }
    }

    @Test
    fun `test - class implementing ObjCObject`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                class Foo: kotlinx.cinterop.ObjCObject()
            """.trimIndent()
        )

        analyze(file) {
            val session = useSiteSession
            val fooSymbol = session.getClassOrFail(file, "Foo")
            assertTrue(session.isObjCObjectType(fooSymbol.memberScope.constructors.single().returnType))
        }
    }

    @Test
    fun `test - class implementing ObjCObject transitively`() {
        val file = inlineSourceCodeAnalysis.createKtFile(
            """
                abstract class A: kotlinx.cinterop.ObjCObject()
                abstract class B: A()
                class Foo: B()
            """.trimIndent()
        )

        analyze(file) {
            val session = useSiteSession
            val fooSymbol = session.getClassOrFail(file, "Foo")
            assertTrue(session.isObjCObjectType(fooSymbol.memberScope.constructors.single().returnType))
        }
    }
}

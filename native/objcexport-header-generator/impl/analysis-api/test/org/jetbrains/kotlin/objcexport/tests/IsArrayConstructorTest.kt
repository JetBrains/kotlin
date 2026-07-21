/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.scopes.memberScope
import org.jetbrains.kotlin.analysis.api.session.analyze
import org.jetbrains.kotlin.analysis.api.session.useSiteSession
import org.jetbrains.kotlin.analysis.api.symbols.findClass
import org.jetbrains.kotlin.export.test.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.export.test.getClassOrFail
import org.jetbrains.kotlin.export.test.getFunctionOrFail
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isArrayConstructor
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

class IsArrayConstructorTest(
    private val inlineSourceCodeAnalysis: InlineSourceCodeAnalysis,
) {
    @Test
    fun `test - regular function`() {
        val file = inlineSourceCodeAnalysis.createKtFile("fun foo() = Unit")
        analyze(file) {
            val session = useSiteSession
            val foo = session.getFunctionOrFail(file, "foo")
            assertFalse(session.isArrayConstructor(foo))
        }
    }

    @Test
    fun `test - regular constructor`() {
        val file = inlineSourceCodeAnalysis.createKtFile("class Foo(val x: Int)")
        analyze(file) {
            val session = useSiteSession
            val foo = session.getClassOrFail(file, "Foo")
            val constructor = foo.memberScope.constructors.singleOrNull() ?: fail("No single constructor found")
            assertFalse(session.isArrayConstructor(constructor))
        }
    }

    @Test
    fun `test - IntArray constructor`() = doTestForArray(ClassId.fromString("kotlin/IntArray"))

    @Test
    fun `test - ByteArray constructor`() = doTestForArray(ClassId.fromString("kotlin/ByteArray"))

    @Test
    fun `test - Array - constructor`() = doTestForArray(ClassId.fromString("kotlin/Array"))

    @Test
    fun `test - NativePtrArray - constructor`() = doTestForArray(ClassId.fromString("kotlin/native/internal/NativePtrArray"))

    private fun doTestForArray(classId: ClassId) {
        val file = inlineSourceCodeAnalysis.createKtFile("")
        analyze(file) {
            val session = useSiteSession
            val arraySymbol = findClass(classId)
                ?: fail("Missing $$classId symbol")

            arraySymbol.memberScope.constructors
                .ifEmpty { fail("No constructors found on $classId") }
                .forEach { constructor ->
                    assertTrue(
                        session.isArrayConstructor(constructor),
                        "Expected $classId constructor to be detected as array constructor"
                    )
                }
        }
    }
}

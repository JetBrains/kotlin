/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.tests

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.objcexport.analysisApiUtils.isArrayConstructor
import org.jetbrains.kotlin.objcexport.testUtils.InlineSourceCodeAnalysis
import org.jetbrains.kotlin.objcexport.testUtils.getClassOrFail
import org.jetbrains.kotlin.objcexport.testUtils.getFunctionOrFail
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
            val foo = file.getFunctionOrFail("foo")
            assertFalse(foo.isArrayConstructor)
        }
    }

    @Test
    fun `test - regular constructor`() {
        val file = inlineSourceCodeAnalysis.createKtFile("class Foo(val x: Int)")
        analyze(file) {
            val foo = file.getClassOrFail("Foo")
            val constructor = foo.getMemberScope().getConstructors().singleOrNull() ?: fail("No single constructor found")
            assertFalse(constructor.isArrayConstructor)
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
            val arraySymbol = getClassOrObjectSymbolByClassId(classId)
                ?: fail("Missing $$classId symbol")

            arraySymbol.getMemberScope().getConstructors()
                .ifEmpty { fail("No constructors found on $classId") }
                .forEach { constructor ->
                    assertTrue(
                        constructor.isArrayConstructor,
                        "Expected $classId constructor to be detected as array constructor"
                    )
                }
        }
    }
}

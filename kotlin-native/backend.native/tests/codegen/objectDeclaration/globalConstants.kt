/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package codegen.objectDeclaration.globalConstants

import kotlin.test.*
import kotlin.native.internal.*

object EmptyClass {}

@Test fun checkEmptyClass() {
    assertTrue(EmptyClass.isPermanent())
}


object ClassWithConstants {
    const val A = 1
    const val B = 2L
    const val C = 3.0
    const val D = 4.0f
    const val E = 5.toShort()
    const val F = 6.toByte()
    const val G = "8"
}

@Test fun checkInit() {
    assertTrue(ClassWithConstants.isPermanent())
    assertEquals(1, ClassWithConstants.A)
    assertEquals(2, ClassWithConstants.B)
    assertEquals(3.0, ClassWithConstants.C)
    assertEquals(4.0f, ClassWithConstants.D)
    assertEquals(5, ClassWithConstants.E)
    assertEquals(6, ClassWithConstants.F)
    assertEquals("8", ClassWithConstants.G)

    assertEquals(1, (ClassWithConstants::A)())
    assertEquals(2, (ClassWithConstants::B)())
    assertEquals(3.0, (ClassWithConstants::C)())
    assertEquals(4.0f, (ClassWithConstants::D)())
    assertEquals(5, (ClassWithConstants::E)())
    assertEquals(6, (ClassWithConstants::F)())
    assertEquals("8", (ClassWithConstants::G)())
}


var ClassWithConstructorInitialized = 0

object ClassWithConstructor {
    init {
        ClassWithConstructorInitialized += 1
    }
    const val A = 1;
}

@Test fun checkConstructor() {
    assertEquals(0, ClassWithConstructorInitialized)
    assertEquals(1, ClassWithConstructor.A)
    assertEquals(0, ClassWithConstructorInitialized)
    val unused1 = ClassWithConstructor
    assertEquals(1, unused1.A)
    assertEquals(1, ClassWithConstructorInitialized)
    val unused2 = ClassWithConstructor
    assertEquals(1, unused2.A)
    assertEquals(1, ClassWithConstructorInitialized)
    assertFalse(ClassWithConstructor.isPermanent())
}

object ClassWithField {
    val x = 4
}

@Test fun checkField() {
    assertFalse(ClassWithField.isPermanent())
}

object ClassWithComputedField {
    val x : Int
       get() = 4
}

@Test fun checkComputedField() {
    assertTrue(ClassWithComputedField.isPermanent())
}

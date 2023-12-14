/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.test.*
import kotlin.native.internal.*

object ClassWithConstants {
    const val A = 1
    const val B = 2L
    const val C = 3.0
    const val D = 4.0f
    const val E = 5.toShort()
    const val F = 6.toByte()
    const val G = "8"
}

fun box(): String {
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

    return "OK"
}

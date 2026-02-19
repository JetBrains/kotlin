/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.test.*
import kotlin.native.internal.*

var ClassWithConstructorInitialized = 0

object ClassWithConstructor {
    init {
        ClassWithConstructorInitialized += 1
    }
    const val A = 1;
}

fun box(): String {
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

    return "OK"
}

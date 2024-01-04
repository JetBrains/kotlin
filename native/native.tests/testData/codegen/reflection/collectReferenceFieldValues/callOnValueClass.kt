/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.native.internal.*
import kotlin.test.*

data class A(val x: Int)
value class BoxA(val x: A)
value class BoxBoxA(val x: BoxA)

fun box(): String {
    val a1 = A(1)
    assertEquals(BoxA(a1).collectReferenceFieldValues(), listOf(a1))
    assertEquals(BoxBoxA(BoxA(a1)).collectReferenceFieldValues(), listOf(a1))

    return "OK"
}

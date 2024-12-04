/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.native.internal.*
import kotlin.test.*

value class BoxInt(val x: Int)
value class BoxBoxInt(val x: BoxInt)

fun box(): String {
    assertEquals(BoxInt(1).collectReferenceFieldValues(), emptyList<Any>())
    assertEquals(BoxBoxInt(BoxInt(1)).collectReferenceFieldValues(), emptyList<Any>())

    return "OK"
}
